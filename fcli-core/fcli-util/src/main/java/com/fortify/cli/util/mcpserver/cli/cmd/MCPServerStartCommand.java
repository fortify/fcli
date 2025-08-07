/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.util.mcpserver.cli.cmd;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.mcp.MCPIgnore;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.common.util.PicocliSpecHelper;
import com.fortify.cli.common.util.ReflectionHelper;
import com.fortify.cli.util.all_commands.cli.mixin.AllCommandsCommandSelectorMixin;
import com.fortify.cli.util.mcpserver.helper.mcp.arg.CommandToolSpecArgHelper;
import com.fortify.cli.util.mcpserver.helper.mcp.exec.CommandToolSpecPagedRecordsBasedExecutor;
import com.fortify.cli.util.mcpserver.helper.mcp.exec.CommandToolSpecPlainExecutor;
import com.fortify.cli.util.mcpserver.helper.mcp.exec.CommandToolSpecSimpleRecordsBasedExecutor;
import com.fortify.cli.util.mcpserver.helper.mcp.exec.ICommandToolSpecExecutor;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;

@Command(name = OutputHelperMixins.Start.CMD_NAME) 
@MCPIgnore // Doesn't make sense to allow mcp-server command to be called from MCP server
public class MCPServerStartCommand extends AbstractRunnableCommand {
    private static final Logger LOG = LoggerFactory.getLogger(MCPServerStartCommand.class);
    @Mixin private AllCommandsCommandSelectorMixin selectorMixin;
    
    public Integer call() throws Exception {
        super.initialize(); // Initialize mixins etc
        
        McpServer.sync(new StdioServerTransportProvider())
                .serverInfo("fcli", FcliBuildProperties.INSTANCE.getFcliVersion())
                .requestTimeout(Duration.ofSeconds(120))
                .instructions("Fcli MCP Server")
                .capabilities(getServerCapabilities())
                .tools(createToolSpecs())
                .build();

        LOG.info("Fcli MCP server running on stdio");
        
        // Keep server running forever until killed
        while(true) {
            Thread.sleep(5000L);
        }
    }

    private ServerCapabilities getServerCapabilities() {
        return ServerCapabilities.builder()
                .resources(false, false)
                .prompts(false)
                .tools(true)
                .build();
    }

    private List<SyncToolSpecification> createToolSpecs() {
        return selectorMixin.getSelectedCommands().getSpecs().stream()
                .filter(cs->!ignore(cs))
                .map(cs->createToolSpec(cs))
                .peek(s->LOG.debug("Registering tool: {}", s.tool().name()))
                .toList();
    }
    
    private static final SyncToolSpecification createToolSpec(CommandSpec spec) {
        return new CommandToolSpecHelper(spec).createToolSpec();
    }
    
    private static final boolean ignore(CommandSpec cs) {
        return ReflectionHelper.hasAnnotation(cs, MCPIgnore.class)
                || !PicocliSpecHelper.isRunnable(cs) 
                || PicocliSpecHelper.isHiddenSelfOrParent(cs);
    }
    
    private static final class CommandToolSpecHelper {
        private final CommandSpec commandSpec;
        private final CommandToolSpecArgHelper toolSpecArgHelper;
        
        private CommandToolSpecHelper(CommandSpec commandSpec) {
            this.commandSpec = commandSpec;
            this.toolSpecArgHelper = new CommandToolSpecArgHelper(commandSpec);
        }
        
        @SneakyThrows
        public final SyncToolSpecification createToolSpec() {
            return McpServerFeatures.SyncToolSpecification.builder()
                    .tool(createTool())
                    .callHandler(createExecutor()::execute)
                    .build();
        }
        
        private final Tool createTool() {
            return Tool.builder()
                    .name(commandSpec.qualifiedName("_"))
                    .description(buildToolDescription())
                    .inputSchema(toolSpecArgHelper.getSchema())
                    .build();
        }
        
        private final String buildToolDescription() {
            var help = commandSpec.commandLine().getHelp();
            return String.format("%s - %s\n%s", commandSpec.qualifiedName(" "), help.header(), help.description());
        }
        
        private final ICommandToolSpecExecutor createExecutor() {
            if ( PicocliSpecHelper.canCollectRecords(commandSpec) ) {
                if ( toolSpecArgHelper.isPaged() ) {
                    return new CommandToolSpecPagedRecordsBasedExecutor(toolSpecArgHelper, commandSpec);
                } else {
                    return new CommandToolSpecSimpleRecordsBasedExecutor(toolSpecArgHelper, commandSpec);
                }
            } else {
                return new CommandToolSpecPlainExecutor(toolSpecArgHelper, commandSpec);
            }
        }

    }
}
