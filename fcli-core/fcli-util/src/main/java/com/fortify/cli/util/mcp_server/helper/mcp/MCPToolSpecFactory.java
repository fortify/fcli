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
package com.fortify.cli.util.mcp_server.helper.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.ActionCliOption;
import com.fortify.cli.common.action.model.ActionMcpIncludeExclude;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.util.mcp_server.cli.cmd.MCPServerStartCommand.McpModule;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.IMCPToolArgHandler;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.MCPToolArgHandlerActionOption;
import com.fortify.cli.util.mcp_server.helper.mcp.arg.MCPToolArgHandlers;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.IMCPToolRunner;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.MCPToolFcliRunnerAction;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.MCPToolFcliRunnerPlainText;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.MCPToolFcliRunnerRecords;
import com.fortify.cli.util.mcp_server.helper.mcp.runner.MCPToolFcliRunnerRecordsPaged;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Model.CommandSpec;

@Slf4j
public final class MCPToolSpecFactory {
    private MCPToolSpecFactory() {}

    public static List<SyncToolSpecification> createToolSpecs(McpModule module, MCPJobManager jobManager) {
        var result = new ArrayList<SyncToolSpecification>();
        result.addAll(module.getSubcommandsStream()
                .filter(spec -> !FcliCommandSpecHelper.isMcpIgnored(spec))
                .map(spec -> new CommandToolSpecHelper(module, spec, jobManager).createToolSpec())
                .peek(s -> log.debug("Registering cmd tool: {}", s.tool().name()))
                .toList());
        if (module.hasActionCmd()) {
            result.addAll(createActionToolSpecs(module, jobManager));
        }
        result.add(jobManager.getJobToolSpecification());
        return result;
    }

    private static List<SyncToolSpecification> createActionToolSpecs(McpModule module, MCPJobManager jobManager) {
        var actionSources = ActionSource.defaultActionSources(module.toString());
        var validationHandler = ActionValidationHandler.WARN;
        return ActionLoaderHelper.streamAsActions(actionSources, validationHandler)
                .filter(MCPToolSpecFactory::includeActionAsMcpTool)
                .map(a -> new ActionToolSpecHelper(module.toString(), a, jobManager).createToolSpec())
                .peek(s -> log.debug("Registering action tool: {}", s.tool().name()))
                .toList();
    }

    private static boolean includeActionAsMcpTool(Action action) {
        try {
            return action.getConfig() == null || action.getConfig().getMcp() != ActionMcpIncludeExclude.exclude;
        } catch (Exception e) {
            log.warn("Error checking MCP include/exclude for action {}: {}",
                    action != null && action.getMetadata() != null ? action.getMetadata().getName() : "<unknown>", e.toString());
            return false;
        }
    }

    private static final class CommandToolSpecHelper {
        private final McpModule module;
        private final CommandSpec commandSpec;
        private final MCPToolArgHandlers toolSpecArgHelper;
        private final MCPJobManager jobManager;

        private CommandToolSpecHelper(McpModule module, CommandSpec commandSpec, MCPJobManager jobManager) {
            this.module = module;
            this.commandSpec = commandSpec;
            this.toolSpecArgHelper = new MCPToolArgHandlers(commandSpec);
            this.jobManager = jobManager;
        }

        @SneakyThrows
        public SyncToolSpecification createToolSpec() {
            return McpServerFeatures.SyncToolSpecification.builder().tool(createTool()).callHandler(createRunner()::run).build();
        }

        private Tool createTool() {
            return Tool.builder().name(commandSpec.qualifiedName("_")
                    .replace('-', '_')).description(buildToolDescription())
                    .inputSchema(toolSpecArgHelper.getSchema()).build();
        }

        private String buildToolDescription() {
            var cmdHeader = commandSpec.commandLine().getHelp().header();
            var mcpToolDescription = FcliCommandSpecHelper.getMessageString(commandSpec, "mcp.description");
            var base = StringUtils.isBlank(mcpToolDescription) ? cmdHeader : String.format("%s\n%s", cmdHeader, mcpToolDescription);
            if (toolSpecArgHelper.isPaged()) {
                base = base + "\nPaging Guidance: This tool may return partial results if background record collection is still in progress. "
                        + "When pagination.totalRecords is null, call the job tool 'fcli_" + module.toString().replace('-', '_') + "_mcp_job' with operation=wait and the pagination.jobToken value to finalize loading and obtain totalRecords & totalPages.";
            }
            return base;
        }

        private IMCPToolRunner createRunner() {
            if (FcliCommandSpecHelper.canCollectRecords(commandSpec)) {
                if (toolSpecArgHelper.isPaged()) {
                    return new MCPToolFcliRunnerRecordsPaged(toolSpecArgHelper, commandSpec, jobManager);
                }
                return new MCPToolFcliRunnerRecords(toolSpecArgHelper, commandSpec, jobManager);
            }
            return new MCPToolFcliRunnerPlainText(toolSpecArgHelper, commandSpec, jobManager);
        }
    }

    private static final class ActionToolSpecHelper {
        private final String moduleName;
        private final Action action;
        private final List<IMCPToolArgHandler> argHandlers;
        private final MCPJobManager jobManager;

        private ActionToolSpecHelper(String module, Action action, MCPJobManager jobManager) {
            this.moduleName = module;
            this.action = action;
            this.jobManager = jobManager;
            this.argHandlers = createArgHandlers();
        }

        public SyncToolSpecification createToolSpec() {
            return McpServerFeatures.SyncToolSpecification.builder().tool(createTool()).callHandler(new MCPToolFcliRunnerAction(moduleName, action, argHandlers, jobManager)::run).build();
        }

        private Tool createTool() {
            return Tool.builder().name(getToolName()).description(getDescription()).inputSchema(createSchema()).build();
        }

        private List<IMCPToolArgHandler> createArgHandlers() {
            var result = new ArrayList<IMCPToolArgHandler>();
            if (action.getCliOptions() != null) {
                for (Map.Entry<String, ActionCliOption> e : action.getCliOptions().entrySet()) {
                    var opt = e.getValue();
                    if (opt.getMcp() == ActionMcpIncludeExclude.exclude) {
                        continue;
                    }
                    var name = getLongestName(opt);
                    if (StringUtils.isBlank(name)) {
                        continue;
                    }
                    result.add(new MCPToolArgHandlerActionOption(name, opt.getDescription(), opt.isRequired(), opt.getType()));
                }
            }
            return result;
        }

        private JsonSchema createSchema() {
            var schema = new JsonSchema("object", new LinkedHashMap<String, Object>(), new ArrayList<String>(), false, new LinkedHashMap<String, Object>(), new LinkedHashMap<String, Object>());
            argHandlers.forEach(h -> h.updateSchema(schema));
            return schema;
        }

        private String getToolName() {
            return "fcli_" + moduleName.replace('-', '_') + "_action_" + action.getMetadata().getName().replace('-', '_');
        }

        private String getDescription() {
            var usage = action.getUsage();
            return usage != null ? usage.getHeader() : action.getMetadata().getName();
        }

        private String getLongestName(ActionCliOption opt) {
            var names = opt.getNamesAsArray();
            if (names == null || names.length == 0) {
                return null;
            }
            String longest = null;
            for (var n : names) {
                if (longest == null || n.length() > longest.length()) {
                    longest = n;
                }
            }
            return longest;
        }
    }
}
