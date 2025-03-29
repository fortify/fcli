package com.fortify.cli.aviator.token.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException; // Import needed if caught separately
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
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
public class AviatorTokenValidateCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Option(names = {"--token"}, description = "access token", required = true) private String token;

    @Override
    protected JsonNode getJsonNode(AviatorAdminSessionDescriptor sessionDescriptor) throws AviatorSimpleException, AviatorTechnicalException {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(sessionDescriptor);
            TokenValidationResponse response = validateToken(client, sessionDescriptor, messageAndSignature);
            return createResponseNode(response);
        }
    }

    private String[] createMessageAndSignature(AviatorAdminSessionDescriptor sessionDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(
                sessionDescriptor,
                token,
                sessionDescriptor.getTenant()
        );
    }

    private TokenValidationResponse validateToken(AviatorGrpcClient client, AviatorAdminSessionDescriptor sessionDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.validateToken(token, sessionDescriptor.getTenant(), signature, message);
    }

    private JsonNode createResponseNode(TokenValidationResponse response) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode tokenNode = objectMapper.createObjectNode();
        String message;
        if (response.getValid()) {
            message = "Token is valid";
        } else {
            String errorMessage = response.getErrorMessage();
            message = (errorMessage == null || errorMessage.isBlank()) ? "Token is invalid" : errorMessage;
        }
        tokenNode.put("message", message);
        return tokenNode;
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}