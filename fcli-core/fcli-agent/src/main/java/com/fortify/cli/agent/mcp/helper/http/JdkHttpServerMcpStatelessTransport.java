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
package com.fortify.cli.agent.mcp.helper.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

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
    private volatile McpStatelessServerHandler mcpHandler;
    private volatile boolean closing;

    public JdkHttpServerMcpStatelessTransport(int port, String mcpEndpoint, McpJsonMapper jsonMapper) throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
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
        if ( closing ) {
            sendPlainError(exchange, 503, "Server is shutting down");
            return;
        }
        if ( !exchange.getRequestURI().getPath().equals(mcpEndpoint) ) {
            sendPlainError(exchange, 404, "Not found");
            return;
        }
        if ( !"POST".equalsIgnoreCase(exchange.getRequestMethod()) ) {
            sendPlainError(exchange, 405, "Method not allowed");
            return;
        }
        if ( mcpHandler == null ) {
            sendPlainError(exchange, 503, "MCP handler not initialized");
            return;
        }

        var accept = getFirstHeader(exchange, "Accept");
        if ( accept == null || !(accept.contains(APPLICATION_JSON) && accept.contains(TEXT_EVENT_STREAM)) ) {
            sendMcpError(exchange, 400, McpError.builder(McpSchema.ErrorCodes.METHOD_NOT_FOUND)
                    .message("Both application/json and text/event-stream required in Accept header")
                    .build());
            return;
        }

        var transportContext = McpTransportContext.create(Map.of(
                "method", exchange.getRequestMethod(),
                "path", exchange.getRequestURI().getPath(),
                "headers", exchange.getRequestHeaders().entrySet().stream()
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())))));
        try {
            var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var message = McpSchema.deserializeJsonRpcMessage(jsonMapper, body);
            if ( message instanceof McpSchema.JSONRPCRequest request ) {
                var response = mcpHandler.handleRequest(transportContext, request)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                sendJson(exchange, 200, response);
            } else if ( message instanceof McpSchema.JSONRPCNotification notification ) {
                if ( INITIALIZED_NOTIFICATION_METHOD.equals(notification.method()) ) {
                    log.debug("Ignoring MCP initialized notification");
                } else {
                    mcpHandler.handleNotification(transportContext, notification)
                            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                            .block();
                }
                sendEmpty(exchange, 202);
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