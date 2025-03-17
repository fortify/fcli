package com.fortify.cli.aviator.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.aviator.application.Application;
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
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class AviatorAppUpdateCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Parameters(index = "0", description = "Application ID") private String applicationId;
    @Option(names = {"-n", "--name"}, required = true) private String newName;

    @Override
    protected JsonNode getJsonNode(AviatorAdminSessionDescriptor sessionDescriptor) {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(sessionDescriptor);
            Application updatedProject = updateProject(client, sessionDescriptor, messageAndSignature);
            return AviatorGrpcUtils.grpcToJsonNode(updatedProject);
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to update project", e.getMessage());
        }
    }

    private String[] createMessageAndSignature(AviatorAdminSessionDescriptor sessionDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(
                sessionDescriptor,
                sessionDescriptor.getTenant(),
                applicationId,
                newName
        );
    }

    private Application updateProject(AviatorGrpcClient client, AviatorAdminSessionDescriptor sessionDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.updateApplication(applicationId, newName, signature, message, sessionDescriptor.getTenant());
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}