package com.fortify.cli.aviator.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.application.Application;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class AviatorAppGetCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Parameters(index = "0", description = "Application ID") private String applicationId;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorAppGetCommand.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    protected JsonNode getJsonNode(AviatorAdminSessionDescriptor sessionDescriptor) throws AviatorSimpleException, AviatorTechnicalException {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(sessionDescriptor);
            Application application = getApplication(client, sessionDescriptor, messageAndSignature);
            JsonNode response = processGetApplicationResponse(AviatorGrpcUtils.grpcToJsonNode(application));
            LOG.info("Retrieved application '{}' for tenant: {}", applicationId, sessionDescriptor.getTenant());
            return response;
        }
    }

    private String[] createMessageAndSignature(AviatorAdminSessionDescriptor sessionDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(sessionDescriptor, sessionDescriptor.getTenant(), applicationId);
    }

    private JsonNode processGetApplicationResponse(JsonNode jsonNode) {
        if (jsonNode instanceof ObjectNode && jsonNode.has("updated_at")) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            String updatedAtStr = objectNode.get("updated_at").asText();
            Instant instant = Instant.parse(updatedAtStr);
            String formattedDate = DATE_FORMATTER.format(instant.atZone(ZoneId.of("UTC")));
            objectNode.put("updated_at", formattedDate);
        }
        return jsonNode;
    }

    private Application getApplication(AviatorGrpcClient client, AviatorAdminSessionDescriptor sessionDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.getApplication(applicationId, signature, message, sessionDescriptor.getTenant());
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}