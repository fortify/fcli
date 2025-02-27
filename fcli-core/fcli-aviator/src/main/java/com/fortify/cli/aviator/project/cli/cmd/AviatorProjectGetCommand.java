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
import picocli.CommandLine.Parameters;

@Command(name = "get")
public class AviatorProjectGetCommand extends AbstractAviatorJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    @Parameters(index = "0", description = "Project ID") private String projectId;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorProjectGetCommand.class);

    @Override
    protected JsonNode getJsonNodeInternal() {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(sessionDescriptorSupplier,sessionDescriptor.getTenant(), projectId);
            String message = messageAndSignature[0];
            String signature = messageAndSignature[1];
            Project project = client.getProject(projectId, signature, message, sessionDescriptor.getTenant());

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode projectNode = objectMapper.createObjectNode();
            projectNode.put("id", project.getId());
            projectNode.put("name", project.getName());
            projectNode.put("updatedAt", project.hasUpdatedAt());
            return projectNode;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to retrieve project", e);
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}