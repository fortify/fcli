package com.fortify.cli.ftest.core

import java.time.Duration

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource

import spock.lang.Shared
import spock.lang.IgnoreIf

import com.fasterxml.jackson.databind.ObjectMapper

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.ServerParameters
import io.modelcontextprotocol.client.transport.StdioClientTransport
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper
import io.modelcontextprotocol.spec.McpSchema

@IgnoreIf({ !sys["ft.fcli"] || sys["ft.fcli"] == "build" })
@Prefix("core.mcp-server.import")
class MCPServerImportSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/actions/server-import-functions.yaml") String importActionPath

    private McpSyncClient createMcpClient(String extraArgs = "") {
        def serverArgs = ["util", "mcp-server", "start", "--import", importActionPath]
        if (extraArgs) {
            serverArgs.addAll(extraArgs.split(" ").toList())
        }
        def cmd = Fcli.buildExternalCommand(serverArgs)
        def serverParams = ServerParameters.builder(cmd[0])
                .args(cmd.tail())
                .build()
        def transport = new StdioClientTransport(serverParams, new JacksonMcpJsonMapper(new ObjectMapper()))
        def client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .initializationTimeout(Duration.ofSeconds(60))
                .build()
        client.initialize()
        return client
    }

    def "listTools shows exported function tools and job tool"() {
        given:
            def client = createMcpClient()
        when:
            def result = client.listTools()
            def toolNames = result.tools().collect { it.name() } as Set
        then:
            // Exported functions should be registered as tools
            toolNames.contains("fcli_fn_echo")
            toolNames.contains("fcli_fn_multiply")
            toolNames.contains("fcli_fn_generateItems")
            // Internal function should NOT be registered
            !toolNames.contains("fcli_fn__helperInternal")
        cleanup:
            client?.closeGracefully()
    }

    def "callTool: non-streaming echo function"() {
        given:
            def client = createMcpClient()
        when:
            def result = client.callTool(new McpSchema.CallToolRequest("fcli_fn_echo", [message: "hello-mcp"]))
            def text = result.content().findAll { it instanceof McpSchema.TextContent }
                    .collect { ((McpSchema.TextContent) it).text() }.join("")
        then:
            text.contains("hello-mcp")
            !result.isError()
        cleanup:
            client?.closeGracefully()
    }

    def "callTool: non-streaming multiply function"() {
        given:
            def client = createMcpClient()
        when:
            def result = client.callTool(new McpSchema.CallToolRequest("fcli_fn_multiply", [x: 7, y: 6]))
            def text = result.content().findAll { it instanceof McpSchema.TextContent }
                    .collect { ((McpSchema.TextContent) it).text() }.join("")
        then:
            text.contains("42")
            !result.isError()
        cleanup:
            client?.closeGracefully()
    }

    def "callTool: streaming generateItems function"() {
        given:
            def client = createMcpClient()
        when:
            def result = client.callTool(new McpSchema.CallToolRequest("fcli_fn_generateItems", [items: [0, 1, 2]]))
            def text = result.content().findAll { it instanceof McpSchema.TextContent }
                    .collect { ((McpSchema.TextContent) it).text() }.join("")
        then:
            text.contains("item-0")
            text.contains("item-1")
            text.contains("item-2")
            !result.isError()
        cleanup:
            client?.closeGracefully()
    }

    def "callTool: non-existent tool returns error"() {
        given:
            def client = createMcpClient()
        when:
            client.callTool(new McpSchema.CallToolRequest("fcli_fn_nonexistent", [:]))
        then:
            thrown(Exception)
        cleanup:
            client?.closeGracefully()
    }

    def "server capabilities include tools"() {
        given:
            def client = createMcpClient()
        when:
            def capabilities = client.getServerCapabilities()
        then:
            capabilities.tools() != null
        cleanup:
            client?.closeGracefully()
    }
}
