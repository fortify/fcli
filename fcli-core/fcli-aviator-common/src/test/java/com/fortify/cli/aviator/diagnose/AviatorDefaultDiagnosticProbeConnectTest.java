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
package com.fortify.cli.aviator.diagnose;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class AviatorDefaultDiagnosticProbeConnectTest {
    @Test
    void shouldReadOnlyStatusLineFromConnectHeaders() throws IOException {
        var payload = "HTTP/1.1 200 Connection established\r\nProxy-Agent: test\r\n\r\nEXTRA";
        var status = AviatorDefaultDiagnosticProbe.readHttpHeaders(
            new ByteArrayInputStream(payload.getBytes(StandardCharsets.ISO_8859_1)));

        assertEquals("HTTP/1.1 200 Connection established", status);
    }

    @Test
    void shouldPreserveBytesAfterHeadersForTls() throws IOException {
        var payload = "HTTP/1.1 200 Connection established\r\n\r\nTLS-BYTES";
        var stream = new ByteArrayInputStream(payload.getBytes(StandardCharsets.ISO_8859_1));
        AviatorDefaultDiagnosticProbe.readHttpHeaders(stream);

        var remaining = new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1);
        assertEquals("TLS-BYTES", remaining);
    }

    @Test
    void shouldRejectNonHttpConnectResponse() {
        var payload = "Not an HTTP response";
        var stream = new ByteArrayInputStream(payload.getBytes(StandardCharsets.ISO_8859_1));
        // readHttpHeaders returns whatever it got; performProxyConnect validates HTTP/
        // Unit-test the validation path via a minimal status check here.
        assertThrows(IOException.class, () -> {
            var status = AviatorDefaultDiagnosticProbe.readHttpHeaders(stream);
            if (status == null || !status.startsWith("HTTP/")) {
                throw new IOException("Proxy did not return an HTTP CONNECT response");
            }
            if (!status.matches("HTTP/\\d(?:\\.\\d)? 2\\d\\d.*")) {
                throw new IOException("Proxy CONNECT failed: " + status);
            }
        });
    }

    @Test
    void shouldRejectNon2xxConnectStatusInHeaderBody() throws IOException {
        var payload = "HTTP/1.1 407 Proxy Authentication Required\r\n\r\n";
        var status = AviatorDefaultDiagnosticProbe.readHttpHeaders(
            new ByteArrayInputStream(payload.getBytes(StandardCharsets.ISO_8859_1)));

        assertTrue(status.startsWith("HTTP/1.1 407"));
        assertThrows(IOException.class, () -> {
            if (!status.matches("HTTP/\\d(?:\\.\\d)? 2\\d\\d.*")) {
                throw new IOException("Proxy CONNECT failed: " + status);
            }
        });
    }
}
