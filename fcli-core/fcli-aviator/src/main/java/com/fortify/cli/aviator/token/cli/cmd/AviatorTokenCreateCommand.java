/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
// File: /fcli-aviator/src/main/java/com/fortify/cli/aviator/token/cli/cmd/AviatorTokenCreateCommand.java
package com.fortify.cli.aviator.token.cli.cmd;

import java.io.File;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorAdminSessionOutputCommand;
import com.fortify.cli.aviator._common.util.AviatorGrpcUtils;
import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.aviator.util.FileUtil;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.grpc.token.TokenGenerationResponse;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.CreateWithDetailsOutput.CMD_NAME)
public class AviatorTokenCreateCommand extends AbstractAviatorAdminSessionOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.CreateWithDetailsOutput outputHelper;
    @Option(names = {"-e", "--email"}, required = true) private String email;
    @Option(names = {"-n", "--name"}) private String customTokenName;
    @Option(names = {"--end-date"}) private String endDate;
    @Option(names = {"--save-token"}, descriptionKey = "fcli.aviator.token.create.save-token", paramLabel = "<file>") private File saveTokenFile;

    private static final Logger LOG = LoggerFactory.getLogger(AviatorTokenCreateCommand.class);
    private static final DateTimeFormatter CURRENT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    protected JsonNode getJsonNode(AviatorAdminConfigDescriptor configDescriptor) throws AviatorSimpleException, AviatorTechnicalException {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(configDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = createMessageAndSignature(configDescriptor);
            TokenGenerationResponse response = generateToken(client, configDescriptor, messageAndSignature);
            return processTokenResponse(response);
        }
    }

    private String[] createMessageAndSignature(AviatorAdminConfigDescriptor configDescriptor) {
        String currentDate = CURRENT_DATE_FORMATTER.format(Instant.now().atOffset(ZoneOffset.UTC));
        String safeCustomTokenName = customTokenName == null ? "" : customTokenName;
        String safeEndDate = endDate == null ? "" : endDate;
        return AviatorSignatureUtils.createMessageAndSignature(configDescriptor, email, safeCustomTokenName, currentDate, safeEndDate);
    }

    private TokenGenerationResponse generateToken(AviatorGrpcClient client, AviatorAdminConfigDescriptor configDescriptor, String[] messageAndSignature) {
        String message = messageAndSignature[0];
        String signature = messageAndSignature[1];
        return client.generateToken(email, customTokenName, signature, message, configDescriptor.getTenant(), endDate);
    }

    private JsonNode processTokenResponse(TokenGenerationResponse response) {
        if (!response.getSuccess()) {
            String errorMessage = response.getErrorMessage().isBlank()
                    ? "Token creation failed: Unable to generate token for email '" + email + "' with name '" + customTokenName + "'. Please verify the provided details and try again."
                    : response.getErrorMessage();
            throw new AviatorSimpleException(errorMessage);
        }

        String generatedToken = response.getToken();
        if (saveTokenFile != null) {
            saveTokenToFile(generatedToken, saveTokenFile);
        }

        JsonNode jsonNode = AviatorGrpcUtils.grpcToJsonNode(response);
        if (jsonNode.has("expiry_date")) {
            try {
                long expiryDateEpoch = jsonNode.get("expiry_date").asLong();
                String formattedDate = DATE_FORMATTER.format(Instant.ofEpochSecond(expiryDateEpoch).atZone(ZoneId.of("UTC")));
                ((ObjectNode) jsonNode).put("expiry_date", formattedDate);
            } catch (DateTimeException | NumberFormatException e) {
                LOG.warn("WARN: Could not format expiry_date from epoch seconds: {}", jsonNode.get("expiry_date").asText(), e);
            }
        }
        LOG.info("Token '{}' created successfully for email: {}", customTokenName, email);
        return jsonNode;
    }

    private void saveTokenToFile(String token, File file) {
        FileUtil.writeStringToFile(file.toPath(), token, true);
        LOG.info("Token saved to file: {}", file.toPath().toAbsolutePath());
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    @Override
    public String getActionCommandResult() {
        return "CREATED";
    }
}