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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.application.Application;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.util.AviatorGrpcUtils;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class AviatorAppUpdateCommand extends AbstractAviatorAdminSessionOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Parameters(index = "0", description = "Application ID") private String applicationId;
    @Option(names = {"-n", "--name"}, required = true) private String newName;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorAppUpdateCommand.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    protected JsonNode getJsonNode(AviatorAdminConfigDescriptor configDescriptor) throws AviatorSimpleException, AviatorTechnicalException {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(configDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(configDescriptor);
            Application updatedApplication = updateApplication(client, configDescriptor, messageAndSignature);
            JsonNode response = processUpdateApplicationResponse(AviatorGrpcUtils.grpcToJsonNode(updatedApplication));
            LOG.info("Application '{}' updated to name '{}' for tenant: {}", applicationId, newName, configDescriptor.getTenant());
            return response;
        }
    }

    private String[] createMessageAndSignature(AviatorAdminConfigDescriptor configDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(configDescriptor, configDescriptor.getTenant(), applicationId, newName);
    }

    private JsonNode processUpdateApplicationResponse(JsonNode jsonNode) {
        if (jsonNode instanceof ObjectNode && jsonNode.has("updated_at")) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            String updatedAtStr = objectNode.get("updated_at").asText();
            Instant instant = Instant.parse(updatedAtStr);
            String formattedDate = DATE_FORMATTER.format(instant.atZone(ZoneId.of("UTC")));
            objectNode.put("updated_at", formattedDate);
        }
        return jsonNode;
    }

    private Application updateApplication(AviatorGrpcClient client, AviatorAdminConfigDescriptor configDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.updateApplication(applicationId, newName, signature, message, configDescriptor.getTenant());
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }
}