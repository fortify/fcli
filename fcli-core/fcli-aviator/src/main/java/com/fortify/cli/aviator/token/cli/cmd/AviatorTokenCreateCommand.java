package com.fortify.cli.aviator.token.cli.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
import com.fortify.cli.aviator.core.IssueAuditor;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.grpc.token.TokenGenerationResponse;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

@Command(name = "create")
public class AviatorTokenCreateCommand extends AbstractRunnableCommand implements Callable<Integer> {
    @Getter @Mixin private OutputHelperMixins.Create outputHelper;
    @Option(names = {"-e", "--email"}, required = true) private String email;
    @Option(names = {"-n", "--name"}, required = true) private String customTokenName;
    @Option(names = {"--end-date"}) private String endDate;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorTokenCreateCommand.class);

    @Override
    public Integer call() throws Exception {
        initMixins();
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {

            String message = String.format("%s;%s;%s;%s;%s", email, customTokenName == null ? "" : customTokenName, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(Instant.now().atOffset(ZoneOffset.UTC)),endDate==null ? "" : endDate, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
            Path keyFile = Path.of(sessionDescriptor.getPrivateKeyFile());
            String signature = SignatureHelper.signer(keyFile, (char[]) null).sign(message, StandardCharsets.UTF_8);
            if(email==null || email.isEmpty()) email = "";
            TokenGenerationResponse response = client.generateToken(email, customTokenName, signature, message, sessionDescriptor.getTenant(), endDate);

            if (response.getSuccess()) {
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode createTokenNode = objectMapper.createObjectNode();
                createTokenNode.put("token", response.getToken())
                        .put("tokenName", response.getTokenName())
                        .put("startDate", response.getStartDate())
                        .put("expiryDate", response.getExpiryDate());
                outputHelper.write(createTokenNode);
                return 0;
            } else {
                System.err.println("Error generating token: " + response.getErrorMessage());
                return 1;
            }
        }
    }
}
