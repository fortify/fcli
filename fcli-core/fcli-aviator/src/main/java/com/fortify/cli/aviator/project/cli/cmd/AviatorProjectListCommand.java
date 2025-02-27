package com.fortify.cli.aviator.project.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.aviator.project.Project;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorJsonNodeOutputCommand;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
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
import java.util.List;

@Command(name = "list")
public class AviatorProjectListCommand extends AbstractAviatorJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorProjectListCommand.class);

    @Override
    protected JsonNode getJsonNodeInternal() {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(sessionDescriptorSupplier,sessionDescriptor.getTenant());
            String message = messageAndSignature[0];
            String signature = messageAndSignature[1];
            List<Project> projects = client.listProjects(sessionDescriptor.getTenant(), signature, message);

            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode projectsArray = objectMapper.createArrayNode();

            for (Project project : projects) {
                ObjectNode projectNode = objectMapper.createObjectNode();
                projectNode.put("id", project.getId());
                projectNode.put("name", project.getName());
                projectNode.put("createdAt", project.hasCreatedAt());
                projectNode.put("isDeleted", project.getIsDeleted());
                projectsArray.add(projectNode);
            }

            if (projects.isEmpty()) {
                LOG.info("No projects found for tenant: {}", sessionDescriptor.getTenant());
            } else {
                LOG.info("Successfully listed {} projects for tenant: {}", projects.size(), sessionDescriptor.getTenant());
            }

            return projectsArray;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to list projects", e);
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}