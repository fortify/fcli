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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.json.JsonHelper;

/**
 * Minimal Chrome DevTools Protocol client over a plain WebSocket (RFC 6455) connection. This is
 * implemented on top of java.net.Socket, as java.net.http is not reliably available in native
 * images. Connections are only ever made to the loopback interface, regardless of the host
 * in the provided URI.
 */
final class FoDSsoDevToolsClient implements AutoCloseable {
    private static final ObjectMapper OM = JsonHelper.getObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_MESSAGE_BYTES = 8 * 1024 * 1024;
    private static final int MAX_HEADER_BYTES = 16 * 1024;
    private static final int OPCODE_CONTINUATION = 0x0, OPCODE_TEXT = 0x1, OPCODE_BINARY = 0x2,
            OPCODE_CLOSE = 0x8, OPCODE_PING = 0x9, OPCODE_PONG = 0xA;

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private int nextId = 1;

    private FoDSsoDevToolsClient(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedInputStream(socket.getInputStream());
        this.out = new BufferedOutputStream(socket.getOutputStream());
    }

    static FoDSsoDevToolsClient connect(URI wsUri, int timeoutMillis) throws IOException {
        var socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), wsUri.getPort()), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);
            var client = new FoDSsoDevToolsClient(socket);
            var path = wsUri.getRawPath() + (wsUri.getRawQuery() == null ? "" : "?" + wsUri.getRawQuery());
            client.handshake(wsUri.getPort(), path);
            return client;
        } catch ( IOException | RuntimeException e ) {
            closeQuietly(socket);
            throw e;
        }
    }

    JsonNode call(String method, JsonNode params) throws IOException {
        int id = nextId++;
        var request = OM.createObjectNode();
        request.put("id", id);
        request.put("method", method);
        if ( params != null ) { request.set("params", params); }
        sendFrame(OPCODE_TEXT, OM.writeValueAsBytes(request));
        while ( true ) {
            var message = OM.readTree(readMessage());
            // Skip protocol events and responses to other requests
            if ( message.path("id").asInt(-1) != id ) { continue; }
            if ( message.has("error") ) {
                throw new IOException("DevTools error for " + method + ": " + message.path("error").path("message").asText());
            }
            return message.path("result");
        }
    }

    @Override
    public void close() {
        closeQuietly(socket);
    }

    private void handshake(int port, String path) throws IOException {
        var key = new byte[16];
        RANDOM.nextBytes(key);
        var request = "GET " + path + " HTTP/1.1\r\n"
                + "Host: 127.0.0.1:" + port + "\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Key: " + Base64.getEncoder().encodeToString(key) + "\r\n"
                + "Sec-WebSocket-Version: 13\r\n\r\n";
        out.write(request.getBytes(StandardCharsets.US_ASCII));
        out.flush();
        var statusLine = readHeaders().lines().findFirst().orElse("");
        if ( !statusLine.contains(" 101") ) {
            throw new IOException("WebSocket handshake failed: " + statusLine);
        }
    }

    private String readHeaders() throws IOException {
        var buf = new ByteArrayOutputStream();
        while ( !endsWithBlankLine(buf.toByteArray()) ) {
            buf.write(readByte());
            if ( buf.size() > MAX_HEADER_BYTES ) { throw new IOException("WebSocket handshake response too large"); }
        }
        return buf.toString(StandardCharsets.ISO_8859_1);
    }

    private static boolean endsWithBlankLine(byte[] b) {
        int n = b.length;
        return n >= 4 && b[n - 4] == '\r' && b[n - 3] == '\n' && b[n - 2] == '\r' && b[n - 1] == '\n';
    }

    private void sendFrame(int opcode, byte[] payload) throws IOException {
        var mask = new byte[4];
        RANDOM.nextBytes(mask);
        out.write(0x80 | opcode);
        if ( payload.length < 126 ) {
            out.write(0x80 | payload.length);
        } else if ( payload.length < 65536 ) {
            out.write(0x80 | 126);
            out.write(payload.length >> 8);
            out.write(payload.length);
        } else {
            out.write(0x80 | 127);
            for ( int shift = 56; shift >= 0; shift -= 8 ) { out.write((int) ((long) payload.length >> shift)); }
        }
        out.write(mask);
        var masked = new byte[payload.length];
        for ( int i = 0; i < payload.length; i++ ) { masked[i] = (byte) (payload[i] ^ mask[i % 4]); }
        out.write(masked);
        out.flush();
    }

    private String readMessage() throws IOException {
        var message = new ByteArrayOutputStream();
        while ( true ) {
            int b0 = readByte();
            int b1 = readByte();
            boolean fin = (b0 & 0x80) != 0;
            int opcode = b0 & 0x0F;
            boolean masked = (b1 & 0x80) != 0;
            long length = b1 & 0x7F;
            if ( length == 126 ) {
                length = (readByte() << 8) | readByte();
            } else if ( length == 127 ) {
                length = 0;
                for ( int i = 0; i < 8; i++ ) { length = (length << 8) | readByte(); }
            }
            if ( length < 0 || length > MAX_MESSAGE_BYTES ) { throw new IOException("WebSocket frame too large"); }
            var mask = masked ? readFully(4) : null;
            var payload = readFully((int) length);
            if ( mask != null ) {
                for ( int i = 0; i < payload.length; i++ ) { payload[i] ^= mask[i % 4]; }
            }
            switch ( opcode ) {
            case OPCODE_CONTINUATION, OPCODE_TEXT, OPCODE_BINARY -> {
                message.write(payload);
                if ( message.size() > MAX_MESSAGE_BYTES ) { throw new IOException("WebSocket message too large"); }
                if ( fin ) { return message.toString(StandardCharsets.UTF_8); }
            }
            case OPCODE_CLOSE -> throw new IOException("WebSocket closed by browser");
            case OPCODE_PING -> sendFrame(OPCODE_PONG, payload);
            default -> { /* ignore pong and unknown control frames */ }
            }
        }
    }

    private int readByte() throws IOException {
        int result = in.read();
        if ( result < 0 ) { throw new EOFException("Connection to browser closed"); }
        return result;
    }

    private byte[] readFully(int length) throws IOException {
        var result = in.readNBytes(length);
        if ( result.length < length ) { throw new EOFException("Connection to browser closed"); }
        return result;
    }

    private static void closeQuietly(Socket socket) {
        try { socket.close(); } catch ( IOException ignore ) {}
    }
}
