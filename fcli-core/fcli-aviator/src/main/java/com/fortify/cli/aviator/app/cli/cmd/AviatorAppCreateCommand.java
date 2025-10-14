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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.application.Application;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.util.AviatorGrpcUtils;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class AviatorAppCreateCommand extends AbstractAviatorAdminSessionOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Parameters(index = "0", description = "Name of the application to create") private String applicationName;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorAppCreateCommand.class);

    @Override
    protected JsonNode getJsonNode(AviatorAdminConfigDescriptor configDescriptor) {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(configDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(configDescriptor);
            Application createdApplication = createApplication(client, configDescriptor, messageAndSignature);
            LOG.info("Application '{}' created successfully for tenant: {}", applicationName, configDescriptor.getTenant());
            return processCreateApplicationResponse(createdApplication);
        }
    }

    private String[] createMessageAndSignature(AviatorAdminConfigDescriptor configDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(configDescriptor, configDescriptor.getTenant(), applicationName);
    }

    private Application createApplication(AviatorGrpcClient client, AviatorAdminConfigDescriptor configDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.createApplication(applicationName, configDescriptor.getTenant(), signature, message);
    }

    private JsonNode processCreateApplicationResponse(Application createdApplication) {
        if (createdApplication == null) {
            return null;
        }

        JsonNode applicationNode = AviatorGrpcUtils.grpcToJsonNode(createdApplication);
        JsonNode legalTerms = applicationNode.get("legalTermsOfService");
        if (legalTerms != null) {
            ((ObjectNode) applicationNode).set("disclaimer", legalTerms);
        }

        return applicationNode;
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    @Override
    public String getActionCommandResult() {
        return "CREATED";
    }
}