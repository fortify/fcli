package com.fortify.cli.aviator.project.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.project.Project;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorJsonNodeOutputCommand;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
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

@Command(name = "list")
public class AviatorProjectListCommand extends AbstractAviatorJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorProjectListCommand.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    protected JsonNode getJsonNodeInternal() {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(
                    sessionDescriptorSupplier,
                    sessionDescriptor.getTenant()
            );
            String message = messageAndSignature[0];
            String signature = messageAndSignature[1];
            List<Project> projects = client.listProjects(sessionDescriptor.getTenant(), signature, message);

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

            if (projects.isEmpty()) {
                LOG.info("No projects found for tenant: {}", sessionDescriptor.getTenant());
            } else {
                LOG.info("Successfully listed {} projects for tenant: {}", projects.size(), sessionDescriptor.getTenant());
            }

            return projectsArray;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to list projects: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}