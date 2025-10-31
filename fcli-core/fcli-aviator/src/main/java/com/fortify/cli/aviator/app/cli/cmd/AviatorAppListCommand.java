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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class AviatorAppListCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorAppListCommand.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    protected JsonNode getJsonNode(AviatorAdminConfigDescriptor configDescriptor) throws AviatorSimpleException, AviatorTechnicalException {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(configDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(configDescriptor);
            List<Application> applications = listApplications(client, configDescriptor, messageAndSignature);
            return formatApplicationsArray(applications, configDescriptor.getTenant());
        }
    }

    private String[] createMessageAndSignature(AviatorAdminConfigDescriptor configDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(configDescriptor, configDescriptor.getTenant());
    }

    private List<Application> listApplications(AviatorGrpcClient client, AviatorAdminConfigDescriptor configDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.listApplication(configDescriptor.getTenant(), signature, message);
    }

    private JsonNode formatApplicationsArray(List<Application> applications, String tenant) {
        ArrayNode applicationsArray = AviatorGrpcUtils.createArrayNode();
        for (Application application : applications) {
            JsonNode applicationNode = AviatorGrpcUtils.grpcToJsonNode(application);
            String createdAt = applicationNode.get("created_at") != null ? applicationNode.get("created_at").asText() : "N/A";
            if (!"N/A".equals(createdAt)) {
                createdAt = ZonedDateTime.parse(createdAt).format(FORMATTER);
            }
            ((ObjectNode) applicationNode).put("created_at", createdAt);
            applicationsArray.add(applicationNode);
        }
        logProjectCount(applications.size(), tenant);
        return applicationsArray;
    }

    private void logProjectCount(int projectCount, String tenant) {
        if (projectCount == 0) {
            LOG.info("No applications found for tenant: {}", tenant);
        } else {
            LOG.info("Successfully listed {} applications for tenant: {}", projectCount, tenant);
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}