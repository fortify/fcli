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
package com.fortify.cli.aviator.app.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fortify.aviator.application.Application;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;

import io.grpc.ManagedChannelBuilder;

class AviatorAppHelperTest {
    @Test
    void isCanonicalIdAcceptsOnlyDigitsWithoutLeadingZero() {
        assertTrue(AviatorAppHelper.isCanonicalId("123"));
        assertTrue(AviatorAppHelper.isCanonicalId("0"));
        assertFalse(AviatorAppHelper.isCanonicalId(null));
        assertFalse(AviatorAppHelper.isCanonicalId(""));
        assertFalse(AviatorAppHelper.isCanonicalId("-1"));
        assertFalse(AviatorAppHelper.isCanonicalId("12a"));
        assertFalse(AviatorAppHelper.isCanonicalId("000144"));
        assertFalse(AviatorAppHelper.isCanonicalId("00"));
        assertFalse(AviatorAppHelper.isCanonicalId("1234567890123456789"));
    }

    @Test
    void numericValuesAreReturnedAsIdWithoutContactingServer() {
        // null client and config descriptor prove that no server call is made for numeric values
        assertEquals("123", AviatorAppHelper.resolveApplicationId(null, null, "123"));
        assertEquals("2024", AviatorAppHelper.resolveApplicationId(null, null, "2024"));
    }

    @Test
    void canonicalIdsArePassedThroughUnchanged() {
        assertEquals("0", AviatorAppHelper.resolveApplicationId(null, null, "0"));
        assertEquals("999999999999999999", AviatorAppHelper.resolveApplicationId(null, null, "999999999999999999"));
    }

    @Test
    void leadingZeroValueIsLookedUpAsNameUnchangedAndReturnsResponseId() throws Exception {
        try (var client = new RecordingClient(Application.newBuilder().setId(777L).build())) {
            String id = AviatorAppHelper.resolveApplicationId(client, adminConfig(), "000144");
            assertEquals("777", id);
            assertEquals(List.of("000144"), client.requestedNames);
            // The signed message must also contain the name exactly as typed
            assertTrue(client.messages.get(0).contains("000144"));
        }
    }

    @Test
    void missingNameFailsWithoutFallingBackToId() throws Exception {
        try (var client = new RecordingClient(null)) {
            var e = assertThrows(AviatorSimpleException.class,
                    () -> AviatorAppHelper.resolveApplicationId(client, adminConfig(), "000144"));
            assertEquals("Application not found", e.getMessage());
            assertEquals(List.of("000144"), client.requestedNames);
        }
    }

    private static AviatorAdminConfigDescriptor adminConfig() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var encodedKey = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        return AviatorAdminConfigDescriptor.builder()
                .aviatorUrl("https://aviator.invalid")
                .tenant("demo")
                .privateKeyContents("-----BEGIN PRIVATE KEY-----\n" + encodedKey + "\n-----END PRIVATE KEY-----\n")
                .build();
    }

    /** Records name lookups instead of calling a server; a null response simulates an unknown application. */
    private static final class RecordingClient extends AviatorGrpcClient {
        private final Application response;
        private final List<String> requestedNames = new ArrayList<>();
        private final List<String> messages = new ArrayList<>();

        RecordingClient(Application response) {
            super(ManagedChannelBuilder.forAddress("localhost", 1).usePlaintext().build(), 1, null);
            this.response = response;
        }

        @Override
        public Application getApplicationByName(String applicationName, String signature, String message, String tenantName) {
            requestedNames.add(applicationName);
            messages.add(message);
            if (response == null) { throw new AviatorSimpleException("Application not found"); }
            return response;
        }
    }

    @Test
    void failsIfBlank() {
        assertThrows(AviatorSimpleException.class, () -> AviatorAppHelper.resolveApplicationId(null, null, " "));
        assertThrows(AviatorSimpleException.class, () -> AviatorAppHelper.resolveApplicationId(null, null, null));
    }
}
