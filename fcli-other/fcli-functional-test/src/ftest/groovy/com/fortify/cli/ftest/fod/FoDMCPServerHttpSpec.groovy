package com.fortify.cli.ftest.fod

import java.nio.file.Files
import java.nio.file.Path

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
@Prefix("fod.mcp-server.http")
class FoDMCPServerHttpSpec extends FcliBaseSpec {

    @Shared @TempDir("fod/mcp-http") String tempDir
    @Shared @TestResource("runtime/actions/server-import-functions.yaml") String commonImportActionPath
    @Shared @TestResource("runtime/actions/server-import-http-fod-functions.yaml") String fodImportActionPath

    @Requires({
        System.getProperty('ft.fod.url') &&
            System.getProperty('ft.fod.tenant') &&
            System.getProperty('ft.fod.user') &&
            System.getProperty('ft.fod.password')
    })
    def "http mcp supports fod auth-backed tools"() {
        given:
            def config = createFoDConfig()
            def handle = MCPHttpServerTestHelper.startHttpClient(config, "X-AUTH-FOD", createFoDAuthHeaderValue())

        when:
            def toolNames = handle.client.listTools().tools().collect { it.name() } as Set
            def productResult = handle.client.callTool(new McpSchema.CallToolRequest("fcli_fn_fodRestCount", [:]))
            def productText = MCPHttpServerTestHelper.getText(productResult)
            def streamingResult = handle.client.callTool(new McpSchema.CallToolRequest("fcli_fn_generateItems", [items: [3, 4, 5]]))
            def streamingText = MCPHttpServerTestHelper.getText(streamingResult)

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

    private HttpServerConfig createFoDConfig() {
        def port = MCPHttpServerTestHelper.getFreePort()
        def configPath = Path.of(tempDir, "mcp-http-fod-${port}.yaml")
        def config = """
            server:
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

    private String createFoDAuthHeaderValue() {
        return [
            "tenant=${MCPHttpServerTestHelper.escapeAuthHeaderValue(System.getProperty('ft.fod.tenant'))}",
            "user=${MCPHttpServerTestHelper.escapeAuthHeaderValue(System.getProperty('ft.fod.user'))}",
            "pat=${MCPHttpServerTestHelper.escapeAuthHeaderValue(System.getProperty('ft.fod.password'))}"
        ].join(";")
    }
}
