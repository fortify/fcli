package com.fortify.cli.aviator.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.application.ApplicationResponseMessage;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
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

@Command(name = OutputHelperMixins.Delete.CMD_NAME)
public class AviatorAppDeleteCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Delete outputHelper;
    @Parameters(index = "0", description = "Application ID") private String applicationId;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorAppDeleteCommand.class);

    @Override
    protected JsonNode getJsonNode(AviatorAdminSessionDescriptor sessionDescriptor) throws AviatorSimpleException, AviatorTechnicalException {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(sessionDescriptor);
            ApplicationResponseMessage response = deleteApplication(client, sessionDescriptor, messageAndSignature);
            JsonNode result = processDeleteResponse(response);
            LOG.info("Application '{}' deleted successfully for tenant: {}", applicationId, sessionDescriptor.getTenant());
            return result;
        }
    }

    private String[] createMessageAndSignature(AviatorAdminSessionDescriptor sessionDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(sessionDescriptor, sessionDescriptor.getTenant(), applicationId);
    }

    private ApplicationResponseMessage deleteApplication(AviatorGrpcClient client, AviatorAdminSessionDescriptor sessionDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.deleteApplication(applicationId, signature, message, sessionDescriptor.getTenant());
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