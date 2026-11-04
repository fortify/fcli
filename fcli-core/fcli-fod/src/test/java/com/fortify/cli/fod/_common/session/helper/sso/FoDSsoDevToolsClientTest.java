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
package com.fortify.cli.fod._common.session.helper.sso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class FoDSsoDevToolsClientTest {
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":(\\d+)");

    @Test
    void shouldHandlePingEventsAndFragmentedLargeResponse() throws Exception {
        var big = "A".repeat(140000);
        var requestLine = new AtomicReference<String>();
        try ( var server = newServer() ) {
            var thread = serve(server, requestLine, id -> "{\"id\":" + id + ",\"result\":{\"result\":{\"value\":\"" + big + "\"}}}", true);
            try ( var client = FoDSsoDevToolsClient.connect(wsUri(server), 5000) ) {
                var result = client.call("Runtime.evaluate", null);
                assertEquals(big, result.path("result").path("value").asText());
            }
            thread.join(5000);
        }
        assertEquals("GET /devtools/page/1 HTTP/1.1", requestLine.get());
    }

    @Test
    void shouldFailOnDevToolsError() throws Exception {
        try ( var server = newServer() ) {
            serve(server, new AtomicReference<>(), id -> "{\"id\":" + id + ",\"error\":{\"message\":\"boom\"}}", false);
            try ( var client = FoDSsoDevToolsClient.connect(wsUri(server), 5000) ) {
                var e = assertThrows(IOException.class, () -> client.call("Runtime.evaluate", null));
                assertTrue(e.getMessage().contains("boom"));
            }
        }
    }

    @Test
    void shouldFailOnRejectedHandshake() throws Exception {
        try ( var server = newServer() ) {
            var thread = new Thread(() -> {
                try ( var s = server.accept() ) {
                    readHeaders(s.getInputStream());
                    s.getOutputStream().write("HTTP/1.1 403 Forbidden\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                    s.getOutputStream().flush();
                } catch ( IOException ignore ) {}
            });
            thread.start();
            assertThrows(IOException.class, () -> FoDSsoDevToolsClient.connect(wsUri(server), 5000));
        }
    }

    private static ServerSocket newServer() throws IOException {
        return new ServerSocket(0, 1, InetAddress.getByAddress(new byte[] {127, 0, 0, 1}));
    }

    private static URI wsUri(ServerSocket server) {
        return URI.create("ws://127.0.0.1:" + server.getLocalPort() + "/devtools/page/1");
    }

    private static Thread serve(ServerSocket server, AtomicReference<String> requestLine, IntFunction<String> responseForId, boolean pingAndFragment) {
        var thread = new Thread(() -> {
            try ( var s = server.accept() ) {
                var in = new BufferedInputStream(s.getInputStream());
                var out = s.getOutputStream();
                requestLine.set(readHeaders(in).lines().findFirst().orElse(""));
                out.write("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                var request = readClientFrame(in);
                var matcher = ID_PATTERN.matcher(request);
                matcher.find();
                var response = responseForId.apply(Integer.parseInt(matcher.group(1))).getBytes(StandardCharsets.UTF_8);
                if ( pingAndFragment ) {
                    out.write(new byte[] {(byte) 0x89, 0});
                    writeServerFrame(out, 0x81, "{\"method\":\"Page.loadEventFired\"}".getBytes(StandardCharsets.UTF_8));
                    int split = response.length / 2;
                    writeServerFrame(out, 0x01, java.util.Arrays.copyOfRange(response, 0, split));
                    writeServerFrame(out, 0x80, java.util.Arrays.copyOfRange(response, split, response.length));
                    out.flush();
                    readClientFrame(in); // pong
                } else {
                    writeServerFrame(out, 0x81, response);
                    out.flush();
                }
            } catch ( IOException ignore ) {}
        });
        thread.start();
        return thread;
    }

    private static String readHeaders(InputStream in) throws IOException {
        var sb = new StringBuilder();
        while ( !sb.toString().endsWith("\r\n\r\n") ) {
            int b = in.read();
            if ( b < 0 ) { throw new IOException("EOF"); }
            sb.append((char) b);
        }
        return sb.toString();
    }

    // Reads a masked client frame (payload length below 126) and returns the unmasked payload
    private static String readClientFrame(InputStream in) throws IOException {
        in.read();
        int length = in.read() & 0x7F;
        var mask = in.readNBytes(4);
        var payload = in.readNBytes(length);
        for ( int i = 0; i < payload.length; i++ ) { payload[i] ^= mask[i % 4]; }
        return new String(payload, StandardCharsets.UTF_8);
    }

    private static void writeServerFrame(OutputStream out, int firstByte, byte[] payload) throws IOException {
        out.write(firstByte);
        if ( payload.length < 126 ) {
            out.write(payload.length);
        } else if ( payload.length < 65536 ) {
            out.write(126);
            out.write(payload.length >> 8);
            out.write(payload.length);
        } else {
            out.write(127);
            for ( int shift = 56; shift >= 0; shift -= 8 ) { out.write((int) ((long) payload.length >> shift)); }
        }
        out.write(payload);
    }
}
