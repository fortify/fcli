package com.fortify.cli.aviator.app.cli.cmd;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.fortify.aviator.application.Application;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
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
    protected JsonNode getJsonNode(AviatorAdminSessionDescriptor sessionDescriptor) throws AviatorSimpleException, AviatorTechnicalException {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(sessionDescriptor);
            List<Application> applications = listApplications(client, sessionDescriptor, messageAndSignature);
            return formatApplicationsArray(applications, sessionDescriptor.getTenant());
        }
    }

    private String[] createMessageAndSignature(AviatorAdminSessionDescriptor sessionDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(sessionDescriptor, sessionDescriptor.getTenant());
    }

    private List<Application> listApplications(AviatorGrpcClient client, AviatorAdminSessionDescriptor sessionDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.listApplication(sessionDescriptor.getTenant(), signature, message);
    }

    private JsonNode formatApplicationsArray(List<Application> applications, String tenant) {
        ArrayNode applicationsArray = AviatorGrpcUtils.createArrayNode();
        for (Application application : applications) {
            JsonNode applicationNode = AviatorGrpcUtils.grpcToJsonNode(application);
            String createdAt = applicationNode.get("createdAt") != null ? applicationNode.get("createdAt").asText() : "N/A";
            if (!"N/A".equals(createdAt)) {
                createdAt = ZonedDateTime.parse(createdAt).format(FORMATTER);
            }
            ((ObjectNode) applicationNode).put("createdAt", createdAt);
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