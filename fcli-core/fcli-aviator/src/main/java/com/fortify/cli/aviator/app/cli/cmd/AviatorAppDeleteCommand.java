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
package com.fortify.cli.aviator.app.cli.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.application.ApplicationResponseMessage;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Delete.CMD_NAME)
public class AviatorAppDeleteCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Delete outputHelper;
    @Parameters(index = "0", description = "Application ID") private String applicationId;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorAppDeleteCommand.class);

    @Override
    protected JsonNode getJsonNode(AviatorAdminConfigDescriptor configDescriptor) throws AviatorSimpleException, AviatorTechnicalException {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(configDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(configDescriptor);
            ApplicationResponseMessage response = deleteApplication(client, configDescriptor, messageAndSignature);
            JsonNode result = processDeleteResponse(response);
            LOG.info("Application '{}' deleted successfully for tenant: {}", applicationId, configDescriptor.getTenant());
            return result;
        }
    }

    private String[] createMessageAndSignature(AviatorAdminConfigDescriptor configDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(configDescriptor, configDescriptor.getTenant(), applicationId);
    }

    private ApplicationResponseMessage deleteApplication(AviatorGrpcClient client, AviatorAdminConfigDescriptor configDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.deleteApplication(applicationId, signature, message, configDescriptor.getTenant());
    }

    private JsonNode processDeleteResponse(ApplicationResponseMessage response) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode deleteProjectNode = objectMapper.createObjectNode();
        deleteProjectNode.put("message", response.getResponseMessage());
        return deleteProjectNode;
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}