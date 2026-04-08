package com.fortify.cli.ftest.core

import java.time.Duration

import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.fortify.cli.ftest._common.spec.TestResource

import spock.lang.Shared
import spock.lang.IgnoreIf
/*
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.ServerParameters
import io.modelcontextprotocol.client.transport.StdioClientTransport
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper
import io.modelcontextprotocol.spec.McpSchema
*/
@IgnoreIf({ !sys["ft.fcli"] || sys["ft.fcli"] == "build" })
@Prefix("core.mcp-server.import")
class MCPServerImportSpec extends FcliBaseSpec {
    @Shared @TestResource("runtime/actions/server-import-functions.yaml") String importActionPath
/*
    private McpSyncClient createMcpClient(String extraArgs = "") {
        def fcli = Input.FcliCommand.get()
        def java = Input.JavaCommand.get() ?: "java"
        def serverArgs = ["util", "mcp-server", "start", "--import", importActionPath]
        if (extraArgs) {
            serverArgs.addAll(extraArgs.split(" ").toList())
        }
        String executable
        List<String> cmdArgs
        if (fcli.endsWith(".jar")) {
            executable = java
            cmdArgs = Fcli.FCLI_SYSTEM_PROPERTY_ARGS + ["-jar", fcli] + serverArgs
        } else {
            executable = fcli
            cmdArgs = Fcli.FCLI_SYSTEM_PROPERTY_ARGS + serverArgs
        }
        def serverParams = ServerParameters.builder(executable)
                .args(cmdArgs as List<String>)
                .build()
        def transport = new StdioClientTransport(serverParams, new JacksonMcpJsonMapper())
        def client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
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
            client?.close()
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
            client?.close()
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
            client?.close()
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
            client?.close()
    }

    def "callTool: non-existent tool returns error"() {
        given:
            def client = createMcpClient()
        when:
            client.callTool(new McpSchema.CallToolRequest("fcli_fn_nonexistent", [:]))
        then:
            thrown(Exception)
        cleanup:
            client?.close()
    }

    def "server capabilities include tools"() {
        given:
            def client = createMcpClient()
        when:
            def capabilities = client.getServerCapabilities()
        then:
            capabilities.tools() != null
        cleanup:
            client?.close()
    }
    */
}
