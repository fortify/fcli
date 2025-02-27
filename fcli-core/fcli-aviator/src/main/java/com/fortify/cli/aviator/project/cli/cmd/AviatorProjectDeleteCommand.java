package com.fortify.cli.aviator.project.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.aviator.project.ProjectResponseMessage;
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
import picocli.CommandLine.Parameters;

@Command(name = "delete")
public class AviatorProjectDeleteCommand extends AbstractAviatorJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Delete outputHelper;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    @Parameters(index = "0", description = "Project ID") private String projectId;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorProjectDeleteCommand.class);

    @Override
    protected JsonNode getJsonNodeInternal() {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(sessionDescriptorSupplier,sessionDescriptor.getTenant(), projectId);
            String message = messageAndSignature[0];
            String signature = messageAndSignature[1];
            ProjectResponseMessage response = client.deleteProject(projectId, signature, message, sessionDescriptor.getTenant());

            return AviatorGrpcUtils.grpcToJsonNode(response);
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to delete project", e);
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}