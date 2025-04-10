package com.fortify.cli.aviator.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.aviator.application.Application;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.util.AviatorGrpcUtils;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class AviatorAppCreateCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Parameters(index = "0", description = "Name of the application to create") private String applicationName;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorAppCreateCommand.class);

    @Override
    protected JsonNode getJsonNode(AviatorAdminConfigDescriptor configDescriptor) throws AviatorSimpleException, AviatorTechnicalException {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(configDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(configDescriptor);
            Application createdApplication = createApplication(client, configDescriptor, messageAndSignature);
            LOG.info("Application '{}' created successfully for tenant: {}", applicationName, configDescriptor.getTenant());
            return AviatorGrpcUtils.grpcToJsonNode(createdApplication);
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

    @Override
    public boolean isSingular() {
        return true;
    }
}