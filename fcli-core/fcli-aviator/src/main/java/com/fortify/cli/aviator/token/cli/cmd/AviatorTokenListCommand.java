package com.fortify.cli.aviator.token.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorJsonNodeOutputCommand;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
import com.fortify.cli.aviator._common.util.AviatorGrpcUtils;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.grpc.token.ListTokensResponse;
import com.fortify.grpc.token.TokenInfo;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Command(name = "list")
public class AviatorTokenListCommand extends AbstractAviatorJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Option(names = {"-e", "--email"}, required = true) private String email;
    @Option(names = {"-p", "--page-size"}, defaultValue = "10") private int pageSize;
    @Option(names = {"--all-pages"}, defaultValue = "false", description = "Fetch all pages automatically (non-interactive)")
    private boolean fetchAllPages;
    @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorTokenListCommand.class);

    @Override
    protected JsonNode getJsonNodeInternal() {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            ArrayNode tokensArray = AviatorGrpcUtils.createArrayNode();
            String nextPageToken = "";
            boolean morePages = true;

            do {
                String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(
                        sessionDescriptorSupplier,
                        email,
                        sessionDescriptor.getTenant()
                );
                String message = messageAndSignature[0];
                String signature = messageAndSignature[1];

                ListTokensResponse response = client.listTokens(email, sessionDescriptor.getTenant(), signature, message, pageSize, nextPageToken);

                if (!response.getSuccess()) {
                    LOG.error("Failed to list tokens: {}", response.getErrorMessage());
                    throw new FcliSimpleException("Failed to list tokens: " + response.getErrorMessage());
                }

                for (TokenInfo tokenInfo : response.getTokensList()) {
                    JsonNode tokenNode = AviatorGrpcUtils.grpcToJsonNode(tokenInfo);
                    ObjectNode mutableTokenNode = tokenNode.deepCopy();
                    mutableTokenNode.put("expiryDate", Instant.ofEpochSecond(tokenInfo.getExpiryDate())
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    tokensArray.add(mutableTokenNode);
                }

                nextPageToken = response.getNextPageToken();
                morePages = !nextPageToken.isEmpty() && fetchAllPages;
                LOG.debug("Fetched page with {} tokens, nextPageToken: {}", response.getTokensList().size(), nextPageToken);
            } while (morePages);

            if (tokensArray.isEmpty()) {
                LOG.info("No tokens found for email: {}", email);
            } else {
                LOG.info("Successfully listed {} tokens for email: {}", tokensArray.size(), email);
            }

            return tokensArray;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to list tokens: " + e.getMessage());
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}