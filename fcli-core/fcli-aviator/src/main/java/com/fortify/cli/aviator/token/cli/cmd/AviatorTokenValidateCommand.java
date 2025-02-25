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
import com.fortify.grpc.token.TokenValidationResponse;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

@Command(name = "validate")
public class AviatorTokenValidateCommand extends AbstractRunnableCommand implements Callable<Integer> {

    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Option(names = {"--token"}, description = "access token")  private String token;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorTokenValidateCommand.class);

    @Override
    public Integer call() throws Exception {
        initMixins();
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String message = String.format("%s;%s;%s", token, sessionDescriptor.getTenant(), ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
            Path keyFile = Path.of(sessionDescriptor.getPrivateKeyFile());
            String signature = SignatureHelper.signer(keyFile, (char[]) null).sign(message, StandardCharsets.UTF_8);
            TokenValidationResponse response = client.validateToken(token, sessionDescriptor.getTenant(), signature, message);
            if (response.getValid()) {
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode validTokenNode = objectMapper.createObjectNode();
                validTokenNode.put("Message", "Token is Valid !");
                outputHelper.write(validTokenNode);
                return 0;
            } else {
                LOG.error("Token is invalid: {}", response.getErrorMessage());
                return 1;
            }
        }
    }
}