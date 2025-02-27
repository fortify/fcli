package com.fortify.cli.aviator.token.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorJsonNodeOutputCommand;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.grpc.token.RevokeTokenResponse;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "revoke")
public class AviatorTokenRevokeCommand extends AbstractAviatorJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Option(names = {"-e", "--email"}, required = true) private String email;
    @Option(names = {"--token"}, required = true) private String token;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorTokenRevokeCommand.class);

    @Override
    protected JsonNode getJsonNodeInternal() {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(sessionDescriptorSupplier, token, email, sessionDescriptor.getTenant());
            String message = messageAndSignature[0];
            String signature = messageAndSignature[1];
            RevokeTokenResponse response = client.revokeToken(token, email, sessionDescriptor.getTenant(), signature, message);

            if (response.getSuccess()) {
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode revokeTokenNode = objectMapper.createObjectNode();
                revokeTokenNode.put("message", "Token successfully revoked");
                LOG.info("Token revoked successfully for email: {}", email);
                return revokeTokenNode;
            } else {
                LOG.error("Failed to revoke token: {}", response.getErrorMessage());
                throw new AviatorSimpleException("Failed to revoke token: " + response.getErrorMessage());
            }
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to revoke token", e.getMessage());
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}