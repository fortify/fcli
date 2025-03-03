package com.fortify.cli.aviator.project.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.project.Project;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
import com.fortify.cli.aviator._common.util.AviatorGrpcUtils;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class AviatorProjectListCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorProjectListCommand.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    protected JsonNode getJsonNode(AviatorAdminSessionDescriptor sessionDescriptor) {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(sessionDescriptor);
            List<Project> projects = listProjects(client, sessionDescriptor, messageAndSignature);
            return formatProjectsArray(projects, sessionDescriptor.getTenant());
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to list projects: " + e.getMessage(), e);
        }
    }

    private String[] createMessageAndSignature(AviatorAdminSessionDescriptor sessionDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(
                sessionDescriptor,
                sessionDescriptor.getTenant()
        );
    }

    private List<Project> listProjects(AviatorGrpcClient client, AviatorAdminSessionDescriptor sessionDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.listProjects(sessionDescriptor.getTenant(), signature, message);
    }

    private JsonNode formatProjectsArray(List<Project> projects, String tenant) {
        ArrayNode projectsArray = AviatorGrpcUtils.createArrayNode();
        for (Project project : projects) {
            JsonNode projectNode = AviatorGrpcUtils.grpcToJsonNode(project);
            String createdAt = projectNode.get("createdAt") != null ? projectNode.get("createdAt").asText() : "N/A";
            if (!"N/A".equals(createdAt)) {
                createdAt = ZonedDateTime.parse(createdAt).format(FORMATTER);
            }
            ((ObjectNode) projectNode).put("createdAt", createdAt);
            projectsArray.add(projectNode);
        }

        logProjectCount(projects.size(), tenant);
        return projectsArray;
    }

    private void logProjectCount(int projectCount, String tenant) {
        if (projectCount == 0) {
            LOG.info("No projects found for tenant: {}", tenant);
        } else {
            LOG.info("Successfully listed {} projects for tenant: {}", projectCount, tenant);
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}