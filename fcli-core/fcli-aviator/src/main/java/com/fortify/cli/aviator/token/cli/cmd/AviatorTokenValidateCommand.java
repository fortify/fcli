package com.fortify.cli.aviator.token.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorJsonNodeOutputCommand;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.grpc.token.TokenValidationResponse;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "validate")
public class AviatorTokenValidateCommand extends AbstractAviatorJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Option(names = {"--token"}, description = "access token", required = true) private String token;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;

    @Override
    protected JsonNode getJsonNodeInternal() {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(sessionDescriptorSupplier, token, sessionDescriptor.getTenant());
            String message = messageAndSignature[0];
            String signature = messageAndSignature[1];
            TokenValidationResponse response = client.validateToken(token, sessionDescriptor.getTenant(), signature, message);

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode tokenNode = objectMapper.createObjectNode();

            if (response.getValid()) {
                tokenNode.put("message", "Token is Valid!");
            } else {
                tokenNode.put("message", "Token is Invalid");
            }

            return tokenNode;
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}