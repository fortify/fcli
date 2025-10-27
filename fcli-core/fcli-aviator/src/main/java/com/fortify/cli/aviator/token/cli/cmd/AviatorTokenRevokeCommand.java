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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.fortify.grpc.token.RevokeTokenResponse;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Revoke.CMD_NAME)
public class AviatorTokenRevokeCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Revoke outputHelper;
    @Option(names = {"-e", "--email"}) private String email;
    @Mixin @Getter private AviatorUserTokenResolverMixin tokenResolver;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorTokenRevokeCommand.class);

    @Override
    protected JsonNode getJsonNode(AviatorAdminConfigDescriptor configDescriptor) throws AviatorSimpleException, AviatorTechnicalException {
        String tokenToRevoke = tokenResolver.getToken();

        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(configDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(configDescriptor, tokenToRevoke);
            RevokeTokenResponse response = revokeToken(client, configDescriptor, messageAndSignature, tokenToRevoke);
            return processRevokeResponse(response, tokenToRevoke);
        }
    }

    private String[] createMessageAndSignature(AviatorAdminConfigDescriptor configDescriptor, String tokenToRevoke) {
        return AviatorSignatureUtils.createMessageAndSignature(configDescriptor, tokenToRevoke, email, configDescriptor.getTenant());
    }

    private RevokeTokenResponse revokeToken(AviatorGrpcClient client, AviatorAdminConfigDescriptor configDescriptor, String[] messageAndSignature, String tokenToRevoke) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.revokeToken(tokenToRevoke, email, configDescriptor.getTenant(), signature, message);
    }

    private JsonNode processRevokeResponse(RevokeTokenResponse response, String tokenToRevoke) {
        if (!response.getSuccess()) {
            String errorMessage = response.getErrorMessage().isBlank()
                    ? "Token revocation failed: Unable to revoke token '" + tokenToRevoke + "' for email '" + email + "'. Please verify the token and email, and ensure you have the necessary permissions."
                    : response.getErrorMessage();
            throw new AviatorSimpleException(errorMessage);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode revokeTokenNode = objectMapper.createObjectNode();
        revokeTokenNode.put("message", "Token successfully revoked");
        LOG.info("Token '{}' revoked successfully for email: {}", tokenToRevoke, email);
        return revokeTokenNode;
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}