/*
 * Copyright 2021-2025 Open Text.
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.ActionCliOption;
import com.fortify.cli.common.action.model.ActionMcpIncludeExclude;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.IMCPToolArgHandler;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.MCPToolArgHandlerActionOption;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.MCPToolArgHandlers;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.IMCPToolRunner;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.MCPToolFcliRunnerAction;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.MCPToolFcliRunnerPlainText;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.MCPToolFcliRunnerRecords;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.MCPToolFcliRunnerRecordsPaged;

import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Start.CMD_NAME) 
@MCPExclude // Doesn't make sense to allow mcp-server start command to be called from MCP server
public class MCPServerStartCommand extends AbstractRunnableCommand {
    private static final Logger LOG = LoggerFactory.getLogger(MCPServerStartCommand.class);
    @Option(names={"--module", "-m"}, required = true) private McpModule module;
    
    public Integer call() throws Exception {
        super.initialize(); // Initialize mixins etc
        
        McpServer.sync(new StdioServerTransportProvider(new JacksonMcpJsonMapper(new ObjectMapper())))
                .serverInfo("fcli", FcliBuildProperties.INSTANCE.getFcliVersion())
                .requestTimeout(Duration.ofSeconds(120))
                .instructions("""
                        - For tools that accept a --*-session option and user hasn't asked for a specific \
                        session, inform the user that the 'default' session will be used.
                        """)
                .capabilities(getServerCapabilities())
                .tools(createToolSpecs())
                .build();

        LOG.info("Fcli MCP server running on stdio");
        System.err.println("Fcli MCP server running on stdio. Hit Ctrl-C to exit.");
        
        // Join all non-daemon threads to ensure proper shutdown of MCP server
        Thread.getAllStackTraces().keySet().stream()
            .filter(t->!t.isDaemon() && t!=Thread.currentThread())
            .forEach(t-> {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted while joining thread "+t.getName(), e);
                }
            });
        
        return 0;
    }

    private static final ServerCapabilities getServerCapabilities() {
        return ServerCapabilities.builder()
                .resources(false, false)
                .prompts(false)
                .tools(true)
                .build();
    }

    private List<SyncToolSpecification> createToolSpecs() {
        var result = new ArrayList<SyncToolSpecification>();
        // Picocli command-based tools
        result.addAll(module.getSubcommandsStream()
                .filter(spec->!FcliCommandSpecHelper.isMcpIgnored(spec))
                .map(cs->createCommandToolSpec(cs))
                .peek(s->LOG.debug("Registering cmd tool: {}", s.tool().name()))
                .toList());
        // Fcli action tools
        if ( module.hasActionCmd() ) {
            result.addAll(createActionToolSpecs());
        }
        return result;
    }
    
    private List<SyncToolSpecification> createActionToolSpecs() {
        var actionSources = ActionSource.defaultActionSources(module.toString());
        var validationHandler = ActionValidationHandler.WARN;
        return ActionLoaderHelper.streamAsActions(actionSources, validationHandler)
                .filter(this::includeActionAsMcpTool)
                .map(a->new ActionToolSpecHelper(module.toString(), a).createToolSpec())
                .peek(s->LOG.debug("Registering action tool: {}", s.tool().name()))
                .toList();
    }
    
    private boolean includeActionAsMcpTool(Action action) {
        try {
            return action.getConfig()==null || action.getConfig().getMcp()!=ActionMcpIncludeExclude.exclude;
        } catch (Exception e) { // Be defensive; never fail server start due to malformed action
            LOG.warn("Error checking MCP include/exclude for action {}: {}", 
                    action!=null && action.getMetadata()!=null ? action.getMetadata().getName() : "<unknown>", e.toString());
            return false;
        }
    }
    
    private static final SyncToolSpecification createCommandToolSpec(CommandSpec spec) {
        return new CommandToolSpecHelper(spec).createToolSpec();
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
                    .name(commandSpec.qualifiedName("_").replace('-', '_'))
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
            var mcpToolDescription = FcliCommandSpecHelper.getMessageString(commandSpec, "mcp.description");
            return StringUtils.isBlank(mcpToolDescription) ? cmdHeader : String.format("%s\n%s", cmdHeader, mcpToolDescription);
        }
        
        private final IMCPToolRunner createRunner() {
            if ( FcliCommandSpecHelper.canCollectRecords(commandSpec) ) {
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
    
    // Helper for action-based MCP tools
    private static final class ActionToolSpecHelper {
        private final String module;
        private final Action action;
        private final List<IMCPToolArgHandler> argHandlers;
        
        private ActionToolSpecHelper(String module, Action action) {
            this.module = module;
            this.action = action;
            this.argHandlers = createArgHandlers();
        }
        
        public final SyncToolSpecification createToolSpec() {
            return McpServerFeatures.SyncToolSpecification.builder()
                    .tool(createTool())
                    .callHandler(new MCPToolFcliRunnerAction(module, action, argHandlers)::run)
                    .build();
        }
        
        private final Tool createTool() {
            return Tool.builder()
                    .name(getToolName())
                    .description(getDescription())
                    .inputSchema(createSchema())
                    .build();
        }
        
        private List<IMCPToolArgHandler> createArgHandlers() {
            var result = new ArrayList<IMCPToolArgHandler>();
            if ( action.getCliOptions()!=null ) {
                for ( Map.Entry<String, ActionCliOption> e : action.getCliOptions().entrySet() ) {
                    var opt = e.getValue();
                    if ( opt.getMcp()==ActionMcpIncludeExclude.exclude ) { continue; }
                    var name = getLongestName(opt);
                    if ( StringUtils.isBlank(name) ) { continue; }
                    result.add(new MCPToolArgHandlerActionOption(name, opt.getDescription(), opt.isRequired(), opt.getType()));
                }
            }
            return result;
        }
        
        private JsonSchema createSchema() {
            var schema = new JsonSchema("object", new LinkedHashMap<String,Object>(), new ArrayList<String>(), false, new LinkedHashMap<String,Object>(), new LinkedHashMap<String,Object>());
            argHandlers.forEach(h->h.updateSchema(schema));
            return schema;
        }
        
        private String getToolName() {
            return "fcli_"+module.replace('-', '_')+"_action_"+action.getMetadata().getName().replace('-', '_');
        }
        
        private String getDescription() {
            var usage = action.getUsage();
            return usage!=null ? usage.getHeader() : action.getMetadata().getName();
        }
        
        private String getLongestName(ActionCliOption opt) {
            var names = opt.getNamesAsArray();
            if ( names==null || names.length==0 ) { return null; }
            String longest = null;
            for ( var n : names ) {
                if ( longest==null || n.length()>longest.length() ) { longest = n; }
            }
            return longest;
        }
    }
    
    /** Fcli modules for which MCP server commands are exposed */
    public static enum McpModule {
        fod, ssc, sc_sast, sc_dast, aviator;
        
        @Override
        public String toString() {
            return name().replace('_', '-');
        }
        
        public boolean hasActionCmd() {
            return getModuleSpec().subcommands().containsKey("action");
        }

        public final Stream<CommandSpec> getSubcommandsStream() {
            return FcliCommandSpecHelper.commandTreeStream(getModuleSpec());
        }

        private CommandSpec getModuleSpec() {
            var moduleName = this.toString();
            var moduleSpec = FcliCommandSpecHelper.getCommandSpec(moduleName);
            if ( moduleSpec==null ) {
                throw new FcliBugException("No command spec found for module: "+moduleName);
            }
            return moduleSpec;
        }
    }
}