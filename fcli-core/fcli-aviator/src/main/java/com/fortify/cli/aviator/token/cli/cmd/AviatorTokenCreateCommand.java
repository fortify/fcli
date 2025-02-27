package com.fortify.cli.aviator.token.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorJsonNodeOutputCommand;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
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

@Command(name = "create")
public class AviatorTokenCreateCommand extends AbstractAviatorJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Option(names = {"-e", "--email"}, required = true) private String email;
    @Option(names = {"-n", "--name"}, required = true) private String customTokenName;
    @Option(names = {"--end-date"}) private String endDate;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;

    @Override
    protected JsonNode getJsonNodeInternal() {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(sessionDescriptorSupplier, email,
                    customTokenName == null ? "" : customTokenName,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd").format(Instant.now().atOffset(ZoneOffset.UTC)),
                    endDate == null ? "" : endDate);
            String message = messageAndSignature[0];
            String signature = messageAndSignature[1];
            TokenGenerationResponse response = client.generateToken(email, customTokenName, signature, message, sessionDescriptor.getTenant(), endDate);

            if (response.getSuccess()) {
                return AviatorGrpcUtils.grpcToJsonNode(response);
            } else {
                throw new AviatorTechnicalException("Failed to generate token: " + response.getErrorMessage());
            }
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to create token", e.getMessage());
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}