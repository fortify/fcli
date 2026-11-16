/*
 * Copyright 2021-2026 Open Text.
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
import com.fortify.cli.aviator._common.operation.cli.mixin.AviatorOperationIdMixin;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserTokenResolverMixin;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.grpc.token.DeleteTokenResponse;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Delete.CMD_NAME)
public class AviatorTokenDeleteCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Delete outputHelper;
    @Mixin private AviatorOperationIdMixin operationIdMixin;
    @Option(names = {"-e", "--email"}) private String email;
    @Mixin @Getter private AviatorUserTokenResolverMixin tokenResolver;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorTokenDeleteCommand.class);

    @Override
    protected JsonNode getJsonNode(AviatorAdminConfigDescriptor configDescriptor) throws AviatorSimpleException, AviatorTechnicalException {
        String tokenToDelete = tokenResolver.getToken();

        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(configDescriptor.getAviatorUrl())) {
            DeleteTokenResponse response = client.deleteToken(tokenToDelete, configDescriptor.getTenant(),
                    operationIdMixin.getOperationId(),
                    AviatorSignatureUtils.operationSigner(configDescriptor, tokenToDelete, email, configDescriptor.getTenant()));
            return processDeleteResponse(response, tokenToDelete);
        }
    }

    private JsonNode processDeleteResponse(DeleteTokenResponse response, String tokenToDelete) {
        if (!response.getSuccess()) {
            String errorMessage = response.getErrorMessage().isBlank()
                    ? "Token deletion failed: Unable to delete token '" + tokenToDelete + "' for email '" + email + "'. Please verify the provided token and email, and ensure you have the necessary permissions."
                    : response.getErrorMessage();
            throw new AviatorSimpleException(errorMessage);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode deleteTokenNode = objectMapper.createObjectNode();
        deleteTokenNode.put("message", "Token deleted successfully");
        LOG.info("Token deleted successfully for email: {}", email);
        return deleteTokenNode;
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}