package com.fortify.cli.aviator.project.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "update")
public class AviatorProjectUpdateCommand extends AbstractAviatorJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    @Parameters(index = "0", description = "Project ID") private String projectId;
    @Option(names = {"-n", "--name"}, required = true) private String newName;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorProjectUpdateCommand.class);

    @Override
    protected JsonNode getJsonNodeInternal() {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(sessionDescriptorSupplier,sessionDescriptor.getTenant(), projectId, newName);
            String message = messageAndSignature[0];
            String signature = messageAndSignature[1];
            Project updatedProject = client.updateProject(projectId, newName, signature, message, sessionDescriptor.getTenant());

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode projectNode = objectMapper.createObjectNode();
            projectNode.put("id", updatedProject.getId());
            projectNode.put("name", updatedProject.getName());
            projectNode.put("updatedAt", updatedProject.hasUpdatedAt());

            LOG.info("Project updated successfully: {} to new name '{}'", projectId, newName);
            return projectNode;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to update project", e);
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}