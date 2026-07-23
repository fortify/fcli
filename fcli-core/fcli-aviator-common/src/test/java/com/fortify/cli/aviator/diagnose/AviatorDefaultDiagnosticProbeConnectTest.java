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
    void shouldRejectNonHttpConnectStatusLine() {
        assertThrows(AviatorProxyConnectException.class,
            () -> AviatorDefaultDiagnosticProbe.validateProxyConnectStatusLine("Not an HTTP response"));
        assertThrows(AviatorProxyConnectException.class,
            () -> AviatorDefaultDiagnosticProbe.validateProxyConnectStatusLine(null));
    }

    @Test
    void shouldRejectNon2xxConnectStatusLine() {
        assertThrows(AviatorProxyConnectException.class,
            () -> AviatorDefaultDiagnosticProbe.validateProxyConnectStatusLine(
                "HTTP/1.1 407 Proxy Authentication Required"));
    }

    @Test
    void shouldAccept2xxConnectStatusLine() throws AviatorProxyConnectException {
        AviatorDefaultDiagnosticProbe.validateProxyConnectStatusLine("HTTP/1.1 200 Connection established");
        AviatorDefaultDiagnosticProbe.validateProxyConnectStatusLine("HTTP/1.0 200 OK");
    }
}
