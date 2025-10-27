/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.aviator.token.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserTokenResolverMixin;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.grpc.token.TokenValidationResponse;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "validate")
public class AviatorTokenValidateCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin @Getter private AviatorUserTokenResolverMixin tokenResolver;

    @Override
    protected JsonNode getJsonNode(AviatorAdminConfigDescriptor configDescriptor) throws AviatorSimpleException, AviatorTechnicalException {
        String tokenToValidate = tokenResolver.getToken();

        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(configDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(configDescriptor, tokenToValidate);
            TokenValidationResponse response = validateToken(client, configDescriptor, messageAndSignature, tokenToValidate);
            return createResponseNode(response);
        }
    }

    private String[] createMessageAndSignature(AviatorAdminConfigDescriptor configDescriptor, String tokenToValidate) {
        return AviatorSignatureUtils.createMessageAndSignature(
                configDescriptor,
                tokenToValidate,
                configDescriptor.getTenant()
        );
    }

    private TokenValidationResponse validateToken(AviatorGrpcClient client, AviatorAdminConfigDescriptor configDescriptor, String[] messageAndSignature, String tokenToValidate) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.validateToken(tokenToValidate, configDescriptor.getTenant(), signature, message);
    }

    private JsonNode createResponseNode(TokenValidationResponse response) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode tokenNode = objectMapper.createObjectNode();
        String message;
        if (response.getValid()) {
            message = "Token is valid";
        } else {
            String errorMessage = response.getErrorMessage();
            message = (errorMessage == null || errorMessage.isBlank()) ? "Token is invalid" : errorMessage;
        }
        tokenNode.put("message", message);
        return tokenNode;
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}