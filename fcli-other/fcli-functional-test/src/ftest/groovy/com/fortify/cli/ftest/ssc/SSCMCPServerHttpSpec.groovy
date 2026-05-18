package com.fortify.cli.ftest.ssc

import java.nio.file.Files
import java.nio.file.Path

import com.fasterxml.jackson.databind.ObjectMapper
import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.MCPHttpServerTestHelper
import com.fortify.cli.ftest._common.MCPHttpServerTestHelper.HttpClientHandle
import com.fortify.cli.ftest._common.MCPHttpServerTestHelper.HttpServerConfig
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TempDir
import com.fortify.cli.ftest._common.spec.TestResource

import io.modelcontextprotocol.spec.McpSchema
import spock.lang.IgnoreIf
import spock.lang.Requires
import spock.lang.Shared

@IgnoreIf({ !sys["ft.fcli"] || sys["ft.fcli"] == "build" })
@Prefix("ssc.mcp-server.http")
class SSCMCPServerHttpSpec extends FcliBaseSpec {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()

    @Shared @TempDir("ssc/mcp-http") String tempDir
    @Shared @TestResource("runtime/actions/server-import-functions.yaml") String commonImportActionPath
    @Shared @TestResource("runtime/actions/server-import-http-ssc-functions.yaml") String sscImportActionPath

    @Requires({
        System.getProperty('ft.ssc.url') &&
            (System.getProperty('ft.ssc.token') ||
                (System.getProperty('ft.ssc.user') && System.getProperty('ft.ssc.password')))
    })
    def "http mcp supports ssc auth-backed tools"() {
        given:
            def auth = createSscAuth()
            def config = createSscConfig()
            def handle = MCPHttpServerTestHelper.startHttpClient(config, "X-AUTH-SSC", auth.headerValue as String)

        when:
            def toolNames = handle.client.listTools().tools().collect { it.name() } as Set
            def productResult = handle.client.callTool(new McpSchema.CallToolRequest("fcli_fn_sscRestCount", [:]))
            def productText = MCPHttpServerTestHelper.getText(productResult)
            def streamingResult = handle.client.callTool(new McpSchema.CallToolRequest("fcli_fn_generateItems", [items: [0, 1, 2]]))
            def streamingText = MCPHttpServerTestHelper.getText(streamingResult)

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

    private HttpServerConfig createSscConfig() {
        def port = MCPHttpServerTestHelper.getFreePort()
        def configPath = Path.of(tempDir, "mcp-http-ssc-${port}.yaml")
        def scSastClientAuthToken = System.getProperty("ft.ssc.client-auth-token")
        def config = new StringBuilder()
            .append("server:\n")
            .append("  port: ${port}\n")
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
            "ssc", "ac", "create-token", "UnifiedLoginToken",
            "--expire-in=5m",
            "--description=${tokenName}",
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
        def values = ["token=${MCPHttpServerTestHelper.escapeAuthHeaderValue(restToken)}"]
        def scSastClientAuthToken = System.getProperty("ft.ssc.client-auth-token")
        if ( scSastClientAuthToken ) {
            values << "sc-sast-token=${MCPHttpServerTestHelper.escapeAuthHeaderValue(scSastClientAuthToken)}"
        }
        return values.join(";")
    }
}
