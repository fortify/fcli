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
package com.fortify.cli.ai_assist.mcp.helper.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import com.fortify.cli.ai_assist.mcp.helper.http.MCPServerHttpConfig.ServerConfig;
import com.fortify.cli.ai_assist.mcp.helper.http.MCPServerHttpConfig.TlsConfig;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * JDK {@link HttpServer}-based MCP stateless transport implementation.
 */
@Slf4j
public class JdkHttpServerMcpStatelessTransport implements McpStatelessServerTransport {
    private static final String APPLICATION_JSON = "application/json";
    private static final String TEXT_EVENT_STREAM = "text/event-stream";
    private static final String INITIALIZED_NOTIFICATION_METHOD = "notifications/initialized";

    private final HttpServer httpServer;
    private final String mcpEndpoint;
    private final McpJsonMapper jsonMapper;
    private final long maxRequestBodyBytes;
    private volatile McpStatelessServerHandler mcpHandler;
    private volatile boolean closing;

    public JdkHttpServerMcpStatelessTransport(ServerConfig serverConfig, String mcpEndpoint, McpJsonMapper jsonMapper) throws IOException {
        this.maxRequestBodyBytes = serverConfig.getMaxRequestBodyBytes();
        var address = serverConfig.getInetSocketAddress();
        var tls = serverConfig.getTls();
        if ( tls != null ) {
            var httpsServer = HttpsServer.create(address, 0);
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(buildSslContext(tls)));
            this.httpServer = httpsServer;
        } else {
            this.httpServer = HttpServer.create(address, 0);
        }
        this.httpServer.setExecutor(Executors.newCachedThreadPool());
        this.mcpEndpoint = normalizeEndpoint(mcpEndpoint);
        this.jsonMapper = jsonMapper;
        this.httpServer.createContext(this.mcpEndpoint, this::handleExchange);
    }

    public void start() {
        httpServer.start();
    }

    @Override
    public void setMcpHandler(McpStatelessServerHandler mcpHandler) {
        this.mcpHandler = mcpHandler;
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            closing = true;
            httpServer.stop(1);
        });
    }

    private void handleExchange(HttpExchange exchange) throws IOException {
        if (!validateRequest(exchange)) { return; }
        var transportContext = buildTransportContext(exchange);
        dispatchMessage(exchange, transportContext);
    }

    private boolean validateRequest(HttpExchange exchange) throws IOException {
        if (closing) {
            sendPlainError(exchange, 503, "Server is shutting down");
            return false;
        }
        if (!exchange.getRequestURI().getPath().equals(mcpEndpoint)) {
            sendPlainError(exchange, 404, "Not found");
            return false;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlainError(exchange, 405, "Method not allowed");
            return false;
        }
        if (mcpHandler == null) {
            sendPlainError(exchange, 503, "MCP handler not initialized");
            return false;
        }
        var accept = getFirstHeader(exchange, "Accept");
        if (accept == null || !(accept.contains(APPLICATION_JSON) && accept.contains(TEXT_EVENT_STREAM))) {
            sendMcpError(exchange, 400, McpError.builder(McpSchema.ErrorCodes.METHOD_NOT_FOUND)
                    .message("Both application/json and text/event-stream required in Accept header")
                    .build());
            return false;
        }
        return true;
    }

    private McpTransportContext buildTransportContext(HttpExchange exchange) {
        return McpTransportContext.create(Map.of(
                "method", exchange.getRequestMethod(),
                "path", exchange.getRequestURI().getPath(),
                "headers", exchange.getRequestHeaders().entrySet().stream()
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())))));
    }

    private void dispatchMessage(HttpExchange exchange, McpTransportContext transportContext) throws IOException {
        try {
            var bodyBytes = readRequestBody(exchange);
            if (bodyBytes == null) { return; }
            var body = new String(bodyBytes, StandardCharsets.UTF_8);
            var message = McpSchema.deserializeJsonRpcMessage(jsonMapper, body);
            if (message instanceof McpSchema.JSONRPCRequest request) {
                handleJsonRpcRequest(exchange, transportContext, request);
            } else if (message instanceof McpSchema.JSONRPCNotification notification) {
                handleJsonRpcNotification(exchange, transportContext, notification);
            } else {
                sendMcpError(exchange, 400, McpError.builder(McpSchema.ErrorCodes.INVALID_REQUEST)
                        .message("The server accepts either requests or notifications")
                        .build());
            }
        } catch (IllegalArgumentException e) {
            sendMcpError(exchange, 400, McpError.builder(McpSchema.ErrorCodes.INVALID_REQUEST)
                    .message("Invalid message format")
                    .build());
        } catch (Exception e) {
            log.error("Unexpected error while handling MCP HTTP request", e);
            sendMcpError(exchange, 500, McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                    .message("Unexpected server error")
                    .build());
        }
    }

    private void handleJsonRpcRequest(HttpExchange exchange, McpTransportContext transportContext,
            McpSchema.JSONRPCRequest request) throws IOException {
        var response = mcpHandler.handleRequest(transportContext, request)
                .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                .block();
        sendJson(exchange, 200, response);
    }

    private void handleJsonRpcNotification(HttpExchange exchange, McpTransportContext transportContext,
            McpSchema.JSONRPCNotification notification) throws IOException {
        if (INITIALIZED_NOTIFICATION_METHOD.equals(notification.method())) {
            log.debug("Ignoring MCP initialized notification");
        } else {
            mcpHandler.handleNotification(transportContext, notification)
                    .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                    .block();
        }
        sendEmpty(exchange, 202);
    }

    private static final long DEFAULT_MAX_REQUEST_BODY_BYTES = 10 * 1024 * 1024; // 10 MB

    private byte[] readRequestBody(HttpExchange exchange) throws IOException {
        var effectiveLimit = maxRequestBodyBytes > 0 ? maxRequestBodyBytes : DEFAULT_MAX_REQUEST_BODY_BYTES;
        // Read one extra byte to detect oversized bodies without loading them fully
        var limit = (int) Math.min(effectiveLimit + 1, Integer.MAX_VALUE);
        var bytes = exchange.getRequestBody().readNBytes(limit);
        if ( bytes.length > effectiveLimit ) {
            sendPlainError(exchange, 413, "Request entity too large");
            return null;
        }
        return bytes;
    }

    private static SSLContext buildSslContext(TlsConfig tls) {
        try {
            var keyStore = KeyStore.getInstance(tls.getKeystoreType());
            try ( InputStream is = Files.newInputStream(tls.getKeystoreFile()) ) {
                keyStore.load(is, tls.getKeystorePassword().toCharArray());
            }
            var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, tls.getEffectiveKeyPassword());
            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);
            return sslContext;
        } catch (GeneralSecurityException | IOException e) {
            throw new FcliSimpleException("Failed to initialize TLS from keystore: " + e.getMessage(), e);
        }
    }

    private String normalizeEndpoint(String endpoint) {
        if ( endpoint == null || endpoint.isBlank() ) {
            return "/mcp";
        }
        return endpoint.startsWith("/") ? endpoint : "/" + endpoint;
    }

    private String getFirstHeader(HttpExchange exchange, String name) {
        var values = exchange.getRequestHeaders().get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private void sendPlainError(HttpExchange exchange, int status, String message) throws IOException {
        var bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try ( var outputStream = exchange.getResponseBody() ) {
            outputStream.write(bytes);
        }
    }

    private void sendMcpError(HttpExchange exchange, int status, McpError error) throws IOException {
        sendJson(exchange, status, error);
    }

    private void sendJson(HttpExchange exchange, int status, Object payload) throws IOException {
        var bytes = jsonMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try ( var outputStream = exchange.getResponseBody() ) {
            outputStream.write(bytes);
        }
    }

    private void sendEmpty(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

}