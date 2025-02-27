package com.fortify.cli.aviator.project.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
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
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(name = "get")
public class AviatorProjectGetCommand extends AbstractAviatorJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    @Parameters(index = "0", description = "Project ID") private String projectId;

    @Override
    protected JsonNode getJsonNodeInternal() {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(sessionDescriptorSupplier,sessionDescriptor.getTenant(), projectId);
            String message = messageAndSignature[0];
            String signature = messageAndSignature[1];
            Project project = client.getProject(projectId, signature, message, sessionDescriptor.getTenant());

            return AviatorGrpcUtils.grpcToJsonNode(project);
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to retrieve project", e);
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}