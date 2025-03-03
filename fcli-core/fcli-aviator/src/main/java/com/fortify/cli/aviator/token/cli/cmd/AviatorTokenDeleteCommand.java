package com.fortify.cli.aviator.token.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.grpc.token.DeleteTokenResponse;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Delete.CMD_NAME)
public class AviatorTokenDeleteCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Delete outputHelper;
    @Option(names = {"-e", "--email"}, required = true) private String email;
    @Option(names = {"--token"}, required = true) private String token;

    @Override
    protected JsonNode getJsonNode(AviatorAdminSessionDescriptor sessionDescriptor) {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(sessionDescriptor);
            DeleteTokenResponse response = deleteToken(client, sessionDescriptor, messageAndSignature);
            return processDeleteResponse(response);
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to delete token", e.getMessage());
        }
    }

    private String[] createMessageAndSignature(AviatorAdminSessionDescriptor sessionDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(
                sessionDescriptor,
                token,
                email,
                sessionDescriptor.getTenant()
        );
    }

    private DeleteTokenResponse deleteToken(AviatorGrpcClient client, AviatorAdminSessionDescriptor sessionDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.deleteToken(token, email, sessionDescriptor.getTenant(), signature, message);
    }

    private JsonNode processDeleteResponse(DeleteTokenResponse response) throws AviatorSimpleException {
        if (response.getSuccess()) {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode deleteTokenNode = objectMapper.createObjectNode();
            deleteTokenNode.put("message", "Token Deleted Successfully");
            return deleteTokenNode;
        } else {
            throw new AviatorSimpleException("Failed to delete token: " + response.getErrorMessage());
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}