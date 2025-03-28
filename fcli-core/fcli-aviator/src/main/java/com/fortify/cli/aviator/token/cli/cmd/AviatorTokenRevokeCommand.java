package com.fortify.cli.aviator.token.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.grpc.token.RevokeTokenResponse;
import io.grpc.StatusRuntimeException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Revoke.CMD_NAME)
public class AviatorTokenRevokeCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper; // Should be Revoke, fix mixin
    @Option(names = {"-e", "--email"}, required = true) private String email;
    @Option(names = {"--token"}, required = true) private String token;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorTokenRevokeCommand.class);

    @Override
    protected JsonNode getJsonNode(AviatorAdminSessionDescriptor sessionDescriptor) {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(sessionDescriptor);
            RevokeTokenResponse response = revokeToken(client, sessionDescriptor, messageAndSignature);
            return processRevokeResponse(response);
        } catch (StatusRuntimeException e) {
            String errorMessage = e.getStatus().getDescription() != null ? e.getStatus().getDescription() : "Unknown error occurred while revoking token";
            throw new FcliSimpleException(errorMessage);
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error occurred while revoking token";
            throw new FcliSimpleException(errorMessage);
        }
    }

    private String[] createMessageAndSignature(AviatorAdminSessionDescriptor sessionDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(sessionDescriptor, token, email, sessionDescriptor.getTenant());
    }

    private RevokeTokenResponse revokeToken(AviatorGrpcClient client, AviatorAdminSessionDescriptor sessionDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.revokeToken(token, email, sessionDescriptor.getTenant(), signature, message);
    }

    private JsonNode processRevokeResponse(RevokeTokenResponse response) {
        if (!response.getSuccess()) {
            String errorMessage = response.getErrorMessage().isBlank() ? "Token revocation failed" : response.getErrorMessage();
            throw new FcliSimpleException(errorMessage);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode revokeTokenNode = objectMapper.createObjectNode();
        revokeTokenNode.put("message", "Token successfully revoked");
        LOG.info("Token '{}' revoked successfully for email: {}", token, email);
        return revokeTokenNode;
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}