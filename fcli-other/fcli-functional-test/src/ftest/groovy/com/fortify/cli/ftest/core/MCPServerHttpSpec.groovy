package com.fortify.cli.ftest.core

import java.net.ServerSocket
import java.net.Socket
import java.net.http.HttpRequest
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir
import com.fortify.cli.ftest._common.spec.TestResource

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper
import io.modelcontextprotocol.spec.McpSchema
import spock.lang.IgnoreIf
import spock.lang.Requires
import spock.lang.Shared

@IgnoreIf({ !sys["ft.fcli"] || sys["ft.fcli"] == "build" })
@Prefix("core.mcp-server.http")
class MCPServerHttpSpec extends FcliBaseSpec {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()

    @Shared @TempDir("core/mcp-http") String tempDir
    @Shared @TestResource("runtime/actions/server-import-functions.yaml") String commonImportActionPath
    @Shared @TestResource("runtime/actions/server-import-http-ssc-functions.yaml") String sscImportActionPath
    @Shared @TestResource("runtime/actions/server-import-http-fod-functions.yaml") String fodImportActionPath

    @Requires({
        System.getProperty('ft.ssc.url') &&
            (System.getProperty('ft.ssc.token') ||
                (System.getProperty('ft.ssc.user') && System.getProperty('ft.ssc.password')))
    })
    def "http mcp supports ssc auth-backed tools"() {
        given:
            def auth = createSscAuth()
            def config = createSscConfig()
            def handle = startHttpClient(config, "X-AUTH-SSC", auth.headerValue as String)

        when:
            def toolNames = handle.client.listTools().tools().collect { it.name() } as Set
            def productResult = handle.client.callTool(new McpSchema.CallToolRequest("fcli_fn_sscRestCount", [:]))
            def productText = getText(productResult)
            def streamingResult = handle.client.callTool(new McpSchema.CallToolRequest("fcli_fn_generateItems", [items: [0, 1, 2]]))
            def streamingText = getText(streamingResult)

        then:
            toolNames.containsAll(["fcli_fn_echo", "fcli_fn_generateItems", "fcli_fn_sscRestCount", "fcli_mcp_job"])
            productText.contains("SSC-REST-OK count=")
            !productResult.isError()
            streamingText.contains("item-0")
            streamingText.contains("item-1")
            streamingText.contains("item-2")
            !streamingResult.isError()

        cleanup:
            handle?.close()
            auth?.cleanup?.call()
    }

    @Requires({
        System.getProperty('ft.fod.url') &&
            System.getProperty('ft.fod.tenant') &&
            System.getProperty('ft.fod.user') &&
            System.getProperty('ft.fod.password')
    })
    def "http mcp supports fod auth-backed tools"() {
        given:
            def config = createFoDConfig()
            def handle = startHttpClient(config, "X-AUTH-FOD", createFoDAuthHeaderValue())

        when:
            def toolNames = handle.client.listTools().tools().collect { it.name() } as Set
            def productResult = handle.client.callTool(new McpSchema.CallToolRequest("fcli_fn_fodRestCount", [:]))
            def productText = getText(productResult)
            def streamingResult = handle.client.callTool(new McpSchema.CallToolRequest("fcli_fn_generateItems", [items: [3, 4, 5]]))
            def streamingText = getText(streamingResult)

        then:
            toolNames.containsAll(["fcli_fn_echo", "fcli_fn_generateItems", "fcli_fn_fodRestCount", "fcli_mcp_job"])
            productText.contains("FOD-REST-OK count=")
            !productResult.isError()
            streamingText.contains("item-3")
            streamingText.contains("item-4")
            streamingText.contains("item-5")
            !streamingResult.isError()

        cleanup:
            handle?.close()
    }

    private HttpServerConfig createSscConfig() {
        def port = getFreePort()
        def configPath = Path.of(tempDir, "mcp-http-ssc-${port}.yaml")
        def scSastClientAuthToken = System.getProperty("ft.ssc.client-auth-token")
        def config = new StringBuilder()
            .append("port: ${port}\n")
            .append("imports:\n")
            .append("  - ${commonImportActionPath}\n")
            .append("  - ${sscImportActionPath}\n")
            .append("ssc:\n")
            .append("  url: ${System.getProperty('ft.ssc.url')}\n")
            .append("  connectTimeout: 30s\n")
            .append("  socketTimeout: 10m\n")
            .append("  insecureModeEnabled: false\n")
        if ( scSastClientAuthToken ) {
            config.append("  scSastClientAuthToken: ${scSastClientAuthToken}\n")
        }
        Files.writeString(configPath, config.toString())
        return new HttpServerConfig(configPath, port)
    }

    private HttpServerConfig createFoDConfig() {
        def port = getFreePort()
        def configPath = Path.of(tempDir, "mcp-http-fod-${port}.yaml")
        def config = """
            port: ${port}
            imports:
              - ${commonImportActionPath}
              - ${fodImportActionPath}
            fod:
              url: ${System.getProperty('ft.fod.url')}
              connectTimeout: 30s
              socketTimeout: 10m
              insecureModeEnabled: false
        """.stripIndent()
        Files.writeString(configPath, config)
        return new HttpServerConfig(configPath, port)
    }

    private Map<String, Object> createSscAuth() {
        def configuredToken = System.getProperty("ft.ssc.token")
        if ( configuredToken ) {
            return [
                headerValue: createSscAuthHeaderValue(configuredToken),
                cleanup: {}
            ]
        }

        def user = System.getProperty("ft.ssc.user")
        def password = System.getProperty("ft.ssc.password")
        def tokenName = "HttpMcpFtest-${System.currentTimeMillis()}"
        def result = Fcli.run([
            "ssc", "ac", "create-token", tokenName,
            "--expire-in=5m",
            "--user=${user}",
            "--password=${password}",
            "-o", "json"
        ])
        def tokenData = OBJECT_MAPPER.readTree(result.stdout.join("\n"))
        def restToken = tokenData.get("restToken").asText()
        return [
            headerValue: createSscAuthHeaderValue(restToken),
            cleanup: {
                Fcli.run([
                    "ssc", "ac", "revoke-token", restToken,
                    "--user=${user}",
                    "--password=${password}"
                ])
            }
        ]
    }

    private String createSscAuthHeaderValue(String restToken) {
        def values = ["token=${escapeAuthHeaderValue(restToken)}"]
        def scSastClientAuthToken = System.getProperty("ft.ssc.client-auth-token")
        if ( scSastClientAuthToken ) {
            values << "sc-sast-token=${escapeAuthHeaderValue(scSastClientAuthToken)}"
        }
        return values.join(";")
    }

    private String createFoDAuthHeaderValue() {
        return [
            "tenant=${escapeAuthHeaderValue(System.getProperty('ft.fod.tenant'))}",
            "user=${escapeAuthHeaderValue(System.getProperty('ft.fod.user'))}",
            "pat=${escapeAuthHeaderValue(System.getProperty('ft.fod.password'))}"
        ].join(";")
    }

    private HttpClientHandle startHttpClient(HttpServerConfig config, String authHeaderName, String authHeaderValue) {
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

    private Process startHttpServer(HttpServerConfig config) {
        def cmd = Fcli.buildExternalCommand(["agent", "mcp", "start-http", "--config", config.path.toString()])
        def process = new ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()
        waitForServerStartup(process, config.port)
        return process
    }

    private static void waitForServerStartup(Process process, int port) {
        def deadline = System.currentTimeMillis() + 30_000
        while ( System.currentTimeMillis() < deadline ) {
            if ( !process.alive() ) {
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

    private static boolean isPortOpen(int port) {
        try {
            new Socket("127.0.0.1", port).close()
            return true
        } catch ( IOException ignored ) {
            return false
        }
    }

    private static int getFreePort() {
        new ServerSocket(0).withCloseable { it.localPort }
    }

    private static String escapeAuthHeaderValue(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\;").replace("=", "\\=")
    }

    private static String getText(McpSchema.CallToolResult result) {
        return result.content().findAll { it instanceof McpSchema.TextContent }
            .collect { ((McpSchema.TextContent)it).text() }
            .join("")
    }

    private static final class HttpServerConfig {
        private final Path path
        private final int port

        private HttpServerConfig(Path path, int port) {
            this.path = path
            this.port = port
        }
    }

    private static final class HttpClientHandle implements Closeable {
        private final McpSyncClient client
        private final Process process

        private HttpClientHandle(McpSyncClient client, Process process) {
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