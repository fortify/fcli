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
import com.fortify.grpc.token.DeleteTokenResponse;
import com.fortify.grpc.token.TokenGenerationResponse;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Mixin;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "delete")
public class AviatorTokenDeleteCommand  extends AbstractRunnableCommand implements Callable<Integer>  {

    @Getter @Mixin private OutputHelperMixins.Delete outputHelper;
    @Option(names = {"-e", "--email"}, required = true) private String email;
    @Option(names = {"--token"}, required = true) private String token;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorTokenDeleteCommand.class);

    @Override
    public Integer call() throws Exception {
        initMixins();
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            String message = String.format("%s;%s;%s;%s", token, email, sessionDescriptor.getTenant(), ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
            Path keyFile = Path.of(sessionDescriptor.getPrivateKeyFile());
            String signature = SignatureHelper.signer(keyFile, (char[]) null).sign(message, StandardCharsets.UTF_8);

            DeleteTokenResponse response = client.deleteToken(token, email, sessionDescriptor.getTenant(), signature, message);

            if (response.getSuccess()) {
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode createTokenNode = objectMapper.createObjectNode();
                createTokenNode.put("message","Token Deleted Successfully");
                outputHelper.write(createTokenNode);
                return 0;
            } else {
                LOG.error("Error deleting token: {}", response.getErrorMessage());
                return 1;
            }
        }
    }
}
