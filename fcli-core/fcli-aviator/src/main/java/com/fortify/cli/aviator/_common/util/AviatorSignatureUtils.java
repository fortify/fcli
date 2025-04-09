package com.fortify.cli.aviator._common.util;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
// No longer need AbstractTextResolverMixin here
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