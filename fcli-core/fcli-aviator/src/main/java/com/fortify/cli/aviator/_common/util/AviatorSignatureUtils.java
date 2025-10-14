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
package com.fortify.cli.aviator._common.util;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.common.crypto.helper.SignatureHelper;
import com.fortify.cli.common.exception.FcliSimpleException;

import lombok.SneakyThrows;

public class AviatorSignatureUtils {
    public static String createMessage(String... params) {
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        return String.join(";", params) + ";" + timestamp;
    }

    @SneakyThrows
    public static String createSignature(String message, AviatorAdminConfigDescriptor configDescriptor) {
        String privateKeyContent = configDescriptor.getPrivateKeyContents();
        if (privateKeyContent == null) {
            throw new FcliSimpleException("Private key content is missing in the admin configuration descriptor.");
        }
        try {
            return SignatureHelper.signer(privateKeyContent, (char[]) null).sign(message, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature using resolved private key", e);
        }
    }

    public static String[] createMessageAndSignature(AviatorAdminConfigDescriptor configDescriptor, String... params) {
        String message = createMessage(params);
        String signature = createSignature(message, configDescriptor);
        return new String[]{message, signature};
    }
}