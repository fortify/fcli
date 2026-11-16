/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.aviator._common.util;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Stream;

import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator.grpc.AviatorRequestSigner;
import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.exception.FcliSimpleException;

public class AviatorSignatureUtils {
    public static String createMessage(String... params) {
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        return String.join(";", params) + ";" + timestamp;
    }

    /**
     * Builds the signed message of a state-changing request.
     * <ul>
     *   <li>operation id present: {@code params;fullMethodName;operationId;timestamp}, so the signature binds the
     *       method and the operation id in addition to the business parameters;</li>
     *   <li>operation id {@code null} (server without replay protection): the legacy {@code params;timestamp}.</li>
     * </ul>
     * A fresh timestamp is used on every call; retries keep their operation id but are always re-signed.
     */
    public static String createOperationMessage(String fullMethodName, String operationId, String... params) {
        if (operationId == null) {
            return createMessage(params);
        }
        return createMessage(Stream.concat(Arrays.stream(params), Stream.of(fullMethodName, operationId)).toArray(String[]::new));
    }

    /** Signer that re-signs with a fresh timestamp for every attempt of one logical operation. */
    public static AviatorRequestSigner operationSigner(AviatorAdminConfigDescriptor configDescriptor, String... params) {
        return (fullMethodName, operationId) -> {
            String message = createOperationMessage(fullMethodName, operationId, params);
            return new AviatorRequestSigner.SignedRequest(message, createSignature(message, configDescriptor));
        };
    }

    public static String createSignature(String message, AviatorAdminConfigDescriptor configDescriptor) {
        String privateKeyContent = configDescriptor.getPrivateKeyContents();
        if (privateKeyContent == null) {
            throw new FcliSimpleException("Private key content is missing in the admin configuration descriptor.");
        }
        try {
            return SignatureHelper.signer(privateKeyContent, (char[]) null).sign(message, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new FcliSimpleException(
                    "Unable to sign the Aviator admin request with the provided private key. Verify that --private-key points to a valid PEM private key",
                    e);
        }
    }

    public static String[] createMessageAndSignature(AviatorAdminConfigDescriptor configDescriptor, String... params) {
        String message = createMessage(params);
        String signature = createSignature(message, configDescriptor);
        return new String[]{message, signature};
    }
}