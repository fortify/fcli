package com.fortify.cli.aviator._common.util;

import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
import com.fortify.cli.common.crypto.helper.SignatureHelper;


import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class AviatorSignatureUtils {

    public static String createMessage(String... params) {
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        String messageBody = String.join(";", params);
        String message = messageBody + ";" + timestamp;
        return message;
    }

    public static String createSignature(String message, AviatorAdminSessionDescriptor sessionDescriptor) {
        try {
            Path keyFile = Path.of(sessionDescriptor.getPrivateKeyFile());
            String signature = SignatureHelper.signer(keyFile, (char[]) null).sign(message, StandardCharsets.UTF_8);
            return signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    public static String[] createMessageAndSignature(AviatorAdminSessionDescriptor sessionDescriptor, String... params) {
        String message = createMessage(params);
        String signature = createSignature(message, sessionDescriptor);
        return new String[]{message, signature};
    }
}