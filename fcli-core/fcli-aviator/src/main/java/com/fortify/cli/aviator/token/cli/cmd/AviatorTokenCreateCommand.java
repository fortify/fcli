package com.fortify.cli.aviator.token.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
import com.fortify.cli.aviator._common.util.AviatorGrpcUtils;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.grpc.token.TokenGenerationResponse;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Command(name = OutputHelperMixins.Create.CMD_NAME)
public class AviatorTokenCreateCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Option(names = {"-e", "--email"}, required = true) private String email;
    @Option(names = {"-n", "--name"}, required = true) private String customTokenName;
    @Option(names = {"--end-date"}) private String endDate;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    protected JsonNode getJsonNode(AviatorAdminSessionDescriptor sessionDescriptor) {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(sessionDescriptor);
            TokenGenerationResponse response = generateToken(client, sessionDescriptor, messageAndSignature);
            return processTokenResponse(response);
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to create token", e.getMessage());
        }
    }

    private String[] createMessageAndSignature(AviatorAdminSessionDescriptor sessionDescriptor) {
        String currentDate = DATE_FORMATTER.format(Instant.now().atOffset(ZoneOffset.UTC));
        String safeCustomTokenName = customTokenName == null ? "" : customTokenName;
        String safeEndDate = endDate == null ? "" : endDate;
        return AviatorSignatureUtils.createMessageAndSignature(
                sessionDescriptor,
                email,
                safeCustomTokenName,
                currentDate,
                safeEndDate
        );
    }

    private TokenGenerationResponse generateToken(AviatorGrpcClient client, AviatorAdminSessionDescriptor sessionDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.generateToken(email, customTokenName, signature, message, sessionDescriptor.getTenant(), endDate);
    }

    private JsonNode processTokenResponse(TokenGenerationResponse response) throws AviatorSimpleException {
        if (response.getSuccess()) {
            return AviatorGrpcUtils.grpcToJsonNode(response);
        } else {
            throw new AviatorSimpleException("Failed to generate token: " + response.getErrorMessage());
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}