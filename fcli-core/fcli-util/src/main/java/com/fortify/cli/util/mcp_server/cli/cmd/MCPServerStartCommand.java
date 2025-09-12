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
package com.fortify.cli.util.mcp_server.cli.cmd;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.mcp.MCPIgnore;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.common.util.PicocliSpecHelper;
import com.fortify.cli.common.util.ReflectionHelper;
import com.fortify.cli.util.all_commands.cli.mixin.AllCommandsCommandSelectorMixin;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.MCPToolArgHandlers;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.IMCPToolRunner;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.MCPToolFcliRunnerPlainText;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.MCPToolFcliRunnerRecords;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.MCPToolFcliRunnerRecordsPaged;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.ArgSpec;
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
        System.err.println("Fcli MCP server running on stdio. Hit Ctrl-C to exit.");
        
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
                || PicocliSpecHelper.isHiddenSelfOrParent(cs)
                || hasRequiredSensitiveArgs(cs);
    }
    
    private static final boolean hasRequiredSensitiveArgs(CommandSpec cs) {
        return Stream.concat(cs.options().stream(), cs.positionalParameters().stream())
                .anyMatch(as->isRequiredSensitiveArg(as));
    }

    private static final boolean isRequiredSensitiveArg(ArgSpec as) {
       return as.required() && MCPToolArgHandlers.isSensitive(as);
    }

    private static final class CommandToolSpecHelper {
        private final CommandSpec commandSpec;
        private final MCPToolArgHandlers toolSpecArgHelper;
        
        private CommandToolSpecHelper(CommandSpec commandSpec) {
            this.commandSpec = commandSpec;
            this.toolSpecArgHelper = new MCPToolArgHandlers(commandSpec);
        }
        
        @SneakyThrows
        public final SyncToolSpecification createToolSpec() {
            return McpServerFeatures.SyncToolSpecification.builder()
                    .tool(createTool())
                    .callHandler(createRunner()::run)
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
            var cmdHeader = commandSpec.commandLine().getHelp().header();
            // Regular fcli usage help might show options/description contents that doesn't 
            // apply to LLM context (for example because some options are not rendered or 
            // use a different syntax, like --query). As such, we only include an MCP-specific
            // description (if defined).
            var mcpToolDescription = PicocliSpecHelper.getMessageString(commandSpec, "mcp.description");
            return StringUtils.isBlank(mcpToolDescription) ? cmdHeader : String.format("%s\n%s", cmdHeader, mcpToolDescription);
        }
        
        private final IMCPToolRunner createRunner() {
            if ( PicocliSpecHelper.canCollectRecords(commandSpec) ) {
                if ( toolSpecArgHelper.isPaged() ) {
                    return new MCPToolFcliRunnerRecordsPaged(toolSpecArgHelper, commandSpec);
                } else {
                    return new MCPToolFcliRunnerRecords(toolSpecArgHelper, commandSpec);
                }
            } else {
                return new MCPToolFcliRunnerPlainText(toolSpecArgHelper, commandSpec);
            }
        }

    }
}
