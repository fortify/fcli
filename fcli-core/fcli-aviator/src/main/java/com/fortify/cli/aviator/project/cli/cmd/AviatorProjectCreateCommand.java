package com.fortify.cli.aviator.project.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.aviator.project.Project;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
import com.fortify.cli.aviator._common.util.AviatorGrpcUtils;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class AviatorProjectCreateCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Option(names = {"-n", "--name"}, required = true) private String projectName;

    @Override
    protected JsonNode getJsonNode(AviatorAdminSessionDescriptor sessionDescriptor) {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(sessionDescriptor);
            Project createdProject = createProject(client, sessionDescriptor, messageAndSignature);
            return AviatorGrpcUtils.grpcToJsonNode(createdProject);
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to create project: " + e.getMessage());
        }
    }

    private String[] createMessageAndSignature(AviatorAdminSessionDescriptor sessionDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(
                sessionDescriptor,
                sessionDescriptor.getTenant(),
                projectName
        );
    }

    private Project createProject(AviatorGrpcClient client, AviatorAdminSessionDescriptor sessionDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.createProject(projectName, sessionDescriptor.getTenant(), signature, message);

    }

    @Override
    public boolean isSingular() {
        return true;
    }
}