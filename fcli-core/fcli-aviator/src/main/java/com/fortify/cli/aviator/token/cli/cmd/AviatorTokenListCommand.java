package com.fortify.cli.aviator.token.cli.cmd;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
import com.fortify.cli.aviator._common.util.AviatorGrpcUtils;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.grpc.token.ListTokensResponse;
import com.fortify.grpc.token.TokenInfo;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class AviatorTokenListCommand extends AbstractAviatorAdminSessionOutputCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Option(names = {"-e", "--email"}, required = true) private String email;
    @Option(names = {"-p", "--page-size"}, defaultValue = "10") private int pageSize;
    @Option(names = {"--all-pages"}, defaultValue = "false", description = "Fetch all pages automatically (non-interactive)")
    private boolean fetchAllPages;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorTokenListCommand.class);

    // Use a formatter with explicit UTC timezone
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withZone(ZoneOffset.UTC);

    @Override
    protected JsonNode getJsonNode(AviatorAdminSessionDescriptor sessionDescriptor) {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl())) {
            ArrayNode tokensArray = fetchAllTokens(client, sessionDescriptor);
            logTokenCount(tokensArray.size());
            return tokensArray;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to list tokens: " + e.getMessage());
        }
    }

    private ArrayNode fetchAllTokens(AviatorGrpcClient client, AviatorAdminSessionDescriptor sessionDescriptor) {
        ArrayNode tokensArray = AviatorGrpcUtils.createArrayNode();
        String nextPageToken = "";
        boolean morePages;

        do {
            String[] messageAndSignature = createMessageAndSignature(sessionDescriptor);
            ListTokensResponse response = listTokens(client, sessionDescriptor, messageAndSignature, nextPageToken);
            appendTokensToArray(response, tokensArray);
            nextPageToken = response.getNextPageToken();
            morePages = !nextPageToken.isEmpty() && fetchAllPages;
            LOG.debug("Fetched page with {} tokens, nextPageToken: {}", response.getTokensList().size(), nextPageToken);
        } while (morePages);

        return tokensArray;
    }

    private String[] createMessageAndSignature(AviatorAdminSessionDescriptor sessionDescriptor) {
        return AviatorSignatureUtils.createMessageAndSignature(
                sessionDescriptor,
                email,
                sessionDescriptor.getTenant()
        );
    }

    private ListTokensResponse listTokens(AviatorGrpcClient client, AviatorAdminSessionDescriptor sessionDescriptor,
                                          String[] messageAndSignature, String nextPageToken) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        ListTokensResponse response = client.listTokens(email, sessionDescriptor.getTenant(), signature, message, pageSize, nextPageToken);
        if (!response.getSuccess()) {
            LOG.error("Failed to list tokens: {}", response.getErrorMessage());
            throw new FcliSimpleException("Failed to list tokens: " + response.getErrorMessage());
        }
        return response;
    }

    private void appendTokensToArray(ListTokensResponse response, ArrayNode tokensArray) {
        for (TokenInfo tokenInfo : response.getTokensList()) {
            JsonNode tokenNode = AviatorGrpcUtils.grpcToJsonNode(tokenInfo);
            ObjectNode mutableTokenNode = tokenNode.deepCopy();

            // Always format dates in UTC, not system default timezone
            mutableTokenNode.put("expiryDate", Instant.ofEpochSecond(tokenInfo.getExpiryDate())
                    .atZone(ZoneOffset.UTC) // Use UTC consistently instead of system default
                    .format(DATE_FORMATTER));

            // Also add a user-friendly display format with timezone indication
            mutableTokenNode.put("expiryDateLocal", Instant.ofEpochSecond(tokenInfo.getExpiryDate())
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")));

            tokensArray.add(mutableTokenNode);
        }
    }

    private void logTokenCount(int tokenCount) {
        if (tokenCount == 0) {
            LOG.info("No tokens found for email: {}", email);
        } else {
            LOG.info("Successfully listed {} tokens for email: {}", tokenCount, email);
        }
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}