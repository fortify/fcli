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

@Command(name = "create")
public class AviatorProjectCreateCommand extends AbstractAviatorJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    @Option(names = {"-n", "--name"}, required = true) private String projectName;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorProjectCreateCommand.class);

    @Override
    protected JsonNode getJsonNodeInternal() {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(sessionDescriptorSupplier,sessionDescriptor.getTenant(), projectName);
            String message = messageAndSignature[0];
            String signature = messageAndSignature[1];
            Project createdProject = client.createProject(projectName, sessionDescriptor.getTenant(), signature, message);

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode projectNode = objectMapper.createObjectNode();
            projectNode.put("id", createdProject.getId());
            projectNode.put("name", createdProject.getName());
            projectNode.put("isDeleted", createdProject.getIsDeleted());

            LOG.info("Project created successfully: {}", projectName);
            return projectNode;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to create project" + e.getMessage());
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}