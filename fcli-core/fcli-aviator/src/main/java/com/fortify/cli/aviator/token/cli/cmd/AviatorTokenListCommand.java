package com.fortify.cli.aviator.token.cli.cmd;

import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
import com.fortify.cli.aviator.core.IssueAuditor;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.aviator.project.cli.cmd.AviatorProjectListCommand;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.grpc.token.ListTokensResponse;
import com.fortify.grpc.token.TokenInfo;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

@Command(name = "list")
public class AviatorTokenListCommand extends AbstractRunnableCommand implements Callable<Integer> {

    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Option(names = {"-e", "--email"}, required = true) private String email;
    @Option(names = {"-p", "--page-size"}, defaultValue = "10") private int pageSize;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorTokenListCommand.class);

    @Override
    public Integer call() throws Exception {
        initMixins();
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {            Console console = System.console();
            if (console == null) {
                System.err.println("No console available for interactive pagination. Consider redirecting output to a file, or using '-o json'.");
                return 1;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            String nextPageToken = "";
            boolean more = true;

            do {
                String message = String.format("%s;%s;%s", email, sessionDescriptor.getTenant(), ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT));
                Path keyFile = Path.of(sessionDescriptor.getPrivateKeyFile());
                String signature = SignatureHelper.signer(keyFile, (char[]) null).sign(message, StandardCharsets.UTF_8);

                ListTokensResponse response = client.listTokens(email, sessionDescriptor.getTenant(), signature, message, pageSize, nextPageToken);

                if (response.getSuccess()) {
                    ArrayNode tokensArray = objectMapper.createArrayNode();
                    for (TokenInfo tokenInfo : response.getTokensList()) {
                        ObjectNode tokenNode = objectMapper.createObjectNode();
                        tokenNode.put("tokenName", tokenInfo.getTokenName());
                        tokenNode.put("token", tokenInfo.getToken());
                        tokenNode.put("startDate", tokenInfo.getStartDate());
                        tokenNode.put("expiryDate", Instant.ofEpochSecond(tokenInfo.getExpiryDate())
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        tokenNode.put("revoked", tokenInfo.getRevoked());
                        tokensArray.add(tokenNode);
                    }

                    outputHelper.write(tokensArray);

                    nextPageToken = response.getNextPageToken();
                    if (!nextPageToken.isEmpty()) {
                        String input = console.readLine("Press Enter to load more results, or type 'q' and press Enter to stop: ").trim().toLowerCase();
                        more = input.isEmpty();
                    } else {
                        more = false;
                    }
                } else {
                    LOG.error("Error listing tokens: {}", response.getErrorMessage());
                    return 1;
                }
            } while (more);

            return 0;
        } catch (Exception e) {
            LOG.error("An unexpected error occurred: {}", e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}
