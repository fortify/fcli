package com.fortify.cli.ftest._common

import java.net.ServerSocket
import java.net.Socket
import java.net.http.HttpRequest
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper
import io.modelcontextprotocol.spec.McpSchema

/**
 * Shared utilities for functional tests that interact with the fcli HTTP MCP server.
 */
class MCPHttpServerTestHelper {

    static HttpClientHandle startHttpClient(HttpServerConfig config, String authHeaderName, String authHeaderValue) {
        def process = startHttpServer(config)
        def transport = HttpClientStreamableHttpTransport.builder("http://127.0.0.1:${config.port}")
            .endpoint("/mcp")
            .connectTimeout(Duration.ofSeconds(10))
            .jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper()))
            .customizeRequest({ HttpRequest.Builder builder -> builder.header(authHeaderName, authHeaderValue) })
            .build()
        def client = McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(30))
            .initializationTimeout(Duration.ofSeconds(60))
            .build()
        client.initialize()
        return new HttpClientHandle(client, process)
    }

    static Process startHttpServer(HttpServerConfig config) {
        def cmd = Fcli.buildExternalCommand(["ai-assist", "mcp", "start-http", "--config", config.path.toString()])
        def process = new ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()
        waitForServerStartup(process, config.port)
        return process
    }

    static void waitForServerStartup(Process process, int port) {
        def deadline = System.currentTimeMillis() + 30_000
        while ( System.currentTimeMillis() < deadline ) {
            if ( !process.isAlive() ) {
                throw new RuntimeException("HTTP MCP server exited before startup completed (exit code ${process.exitValue()})")
            }
            if ( isPortOpen(port) ) {
                return
            }
            Thread.sleep(100)
        }
        process.destroyForcibly()
        process.waitFor(5, TimeUnit.SECONDS)
        throw new RuntimeException("HTTP MCP server did not start within 30 seconds on port ${port}")
    }

    static boolean isPortOpen(int port) {
        try {
            new Socket("127.0.0.1", port).close()
            return true
        } catch ( IOException ignored ) {
            return false
        }
    }

    static int getFreePort() {
        new ServerSocket(0).withCloseable { it.localPort }
    }

    static String escapeAuthHeaderValue(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\;").replace("=", "\\=")
    }

    static String getText(McpSchema.CallToolResult result) {
        return result.content().findAll { it instanceof McpSchema.TextContent }
            .collect { ((McpSchema.TextContent)it).text() }
            .join("")
    }

    static final class HttpServerConfig {
        final Path path
        final int port

        HttpServerConfig(Path path, int port) {
            this.path = path
            this.port = port
        }
    }

    static final class HttpClientHandle implements Closeable {
        final McpSyncClient client
        final Process process

        HttpClientHandle(McpSyncClient client, Process process) {
            this.client = client
            this.process = process
        }

        @Override
        void close() throws IOException {
            try {
                client?.closeGracefully()
            } finally {
                process?.destroyForcibly()
                process?.waitFor(5, TimeUnit.SECONDS)
            }
        }
    }
}
