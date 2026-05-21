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
package com.fortify.cli.ai_assist.mcp.cli.cmd;

import java.io.FilterInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.ai_assist.mcp.helper.MCPJobManager;
import com.fortify.cli.ai_assist.mcp.helper.arg.IMCPToolArgHandler;
import com.fortify.cli.ai_assist.mcp.helper.arg.MCPToolArgHandlerActionOption;
import com.fortify.cli.ai_assist.mcp.helper.arg.MCPToolArgHandlerPaging;
import com.fortify.cli.ai_assist.mcp.helper.arg.MCPToolArgHandlers;
import com.fortify.cli.ai_assist.mcp.helper.runner.IMCPToolRunner;
import com.fortify.cli.ai_assist.mcp.helper.runner.MCPResourceFcliRunnerFunction;
import com.fortify.cli.ai_assist.mcp.helper.runner.MCPToolFcliRunnerAction;
import com.fortify.cli.ai_assist.mcp.helper.runner.MCPToolFcliRunnerFunction;
import com.fortify.cli.ai_assist.mcp.helper.runner.MCPToolFcliRunnerFunctionStreaming;
import com.fortify.cli.ai_assist.mcp.helper.runner.MCPToolFcliRunnerPlainText;
import com.fortify.cli.ai_assist.mcp.helper.runner.MCPToolFcliRunnerRecords;
import com.fortify.cli.ai_assist.mcp.helper.runner.MCPToolFcliRunnerRecordsPaged;
import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.ActionCliOption;
import com.fortify.cli.common.action.model.ActionFunction;
import com.fortify.cli.common.action.model.ActionMcpIncludeExclude;
import com.fortify.cli.common.action.runner.ActionFunctionExecutor;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliActionState;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.cli.util.FcliExecutionContext;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.cli.util.FcliIsolationScope;
import com.fortify.cli.common.cli.util.IFcliExecutionContextManager;
import com.fortify.cli.common.cli.util.StdioHelper;
import com.fortify.cli.common.concurrent.job.AsyncJobManager;
import com.fortify.cli.common.concurrent.job.cli.mixin.AsyncJobManagerMixin;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.common.util.FcliBuildProperties;

import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceTemplateSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.ResourceTemplate;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

@Command(name = "start-stdio")
@MCPExclude // Doesn't make sense to allow mcp-server start command to be called from MCP server
@Slf4j
public class AiAssistMCPStartStdioCommand extends AbstractRunnableCommand implements IFcliExecutionContextManager {
    @Option(names={"--module", "-m"}, required = false) private McpModule module;
    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names={"--import"}, split=",") private List<String> importFiles;
    @Option(names={"--work-threads"}, defaultValue="10") private int workThreads;
    @Option(names={"--progress-threads"}, defaultValue="4") private int progressThreads;
    @Option(names={"--job-safe-return"}, defaultValue="25s") private String jobSafeReturnPeriod;
    @Option(names={"--progress-interval"}, defaultValue="5s") private String progressIntervalPeriod;
    @Mixin private AsyncJobManagerMixin asyncJobManagerMixin;
    private static final AsyncJobManager.Config MCP_ASYNC_DEFAULTS = AsyncJobManager.Config.builder().build();
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MILLISECONDS, Period.MINUTES);
    private final FcliIsolationScope sharedIsolationScope = new FcliIsolationScope();
    private final FcliActionState sharedFunctionActionState = new FcliActionState();
    private final Supplier<FcliExecutionContextHolder.ContextFrame> sharedFunctionFrameSupplier =
            () -> FcliExecutionContextHolder.push(new FcliExecutionContext(sharedIsolationScope, sharedFunctionActionState));
    private MCPJobManager jobManager;

    @Override
    public Integer call() throws Exception {
        if (module == null && (importFiles == null || importFiles.isEmpty())) {
            throw new FcliSimpleException("At least one of --module or --import must be specified");
        }
        var rawOut = StdioHelper.getRawOut();
        var rawErr = StdioHelper.getRawErr();
        // Redirect progress output to stderr to prevent progress messages
        // from corrupting the MCP protocol on the stdout channel
        StdioHelper.setProgressOut(rawErr);
        StdioHelper.setProgressErr(rawErr);

        long safeReturnMillis = PERIOD_HELPER.parsePeriodToMillis(jobSafeReturnPeriod);
        long progressIntervalMillis = PERIOD_HELPER.parsePeriodToMillis(progressIntervalPeriod);
        if ( safeReturnMillis<=0 ) {
            safeReturnMillis = 25000;
        }
        if ( progressIntervalMillis<=0 ) {
            progressIntervalMillis = 500;
        }
        var asyncConfig = asyncJobManagerMixin.buildAsyncJobManager(MCP_ASYNC_DEFAULTS);
        // Instantiate job manager prior to building tool specs so we can include job tool spec
        this.jobManager = new MCPJobManager(workThreads, progressThreads, safeReturnMillis, progressIntervalMillis, asyncConfig);
        var toolSpecs = createToolSpecs();
        var resourceTemplateSpecs = createResourceTemplateSpecs();
        var hasResources = !resourceTemplateSpecs.isEmpty();
        var objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Wrap System.in so we can detect EOF (client disconnect) and unblock the main thread.
        var latch = new CountDownLatch(1);
        var wrappedIn = new FilterInputStream(System.in) {
            @Override public int read() throws IOException {
                int b = super.read(); if (b == -1) { latch.countDown(); } return b;
            }
            @Override public int read(byte[] b, int off, int len) throws IOException {
                int n = super.read(b, off, len); if (n == -1) { latch.countDown(); } return n;
            }
        };
        // Use rawOut to bypass the delegation/masking stack, ensuring
        // MCP JSON-RPC responses are never corrupted by masking
        var serverBuilder = McpServer.sync(new StdioServerTransportProvider(new JacksonMcpJsonMapper(objectMapper), wrappedIn, rawOut))
                .serverInfo("fcli", FcliBuildProperties.INSTANCE.getFcliVersion())
                .requestTimeout(Duration.ofSeconds(120))
                .instructions("""
                        - For tools that accept a --*-session option and user hasn't asked for a specific \
                        session, inform the user that the 'default' session will be used.
                        """)
                .capabilities(getServerCapabilities(hasResources))
                .tools(toolSpecs);
        if (hasResources) {
            serverBuilder.resourceTemplates(resourceTemplateSpecs);
        }
        var server = serverBuilder.build();
        log.debug("Initialized MCP server instance: {}", server);
        log.info("Fcli MCP server running on stdio");
        System.err.println("Fcli MCP server running on stdio. Hit Ctrl-C to exit.");
        // Block the main thread until the MCP client disconnects (stdin EOF) or the process
        // receives SIGTERM/SIGINT. The StdioServerTransportProvider schedules its inbound/
        // outbound threads asynchronously; the old Thread.getAllStackTraces() join loop raced
        // against those threads not yet being visible in the JVM thread list, causing premature
        // process exit (System.exit is called after call() returns) before the initialization
        // handshake could complete. The latch approach eliminates that race entirely.
        Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown, "mcp-shutdown-hook"));
        latch.await();
        return 0;
    }

    private static ServerCapabilities getServerCapabilities(boolean hasResources) {
        return ServerCapabilities.builder()
                .resources(hasResources, false)
                .prompts(false)
                .tools(true)
                .build();
    }

    private List<SyncToolSpecification> createToolSpecs() {
        var result = new ArrayList<SyncToolSpecification>();
        if (module != null) {
            result.addAll(module.getSubcommandsStream()
                    .filter(spec->!FcliCommandSpecHelper.isMcpIgnored(spec))
                    .map(this::createCommandToolSpec)
                    .map(this::wrapToolSpec)
                    .peek(s->log.debug("Registering cmd tool: {}", s.tool().name()))
                    .toList());
            if ( module.hasActionCmd() ) {
                result.addAll(createActionToolSpecs());
            }
        }
        // Register function tools from --import files
        if (importFiles != null) {
            for (var importFile : importFiles) {
                var action = loadImportedAction(importFile);
                result.addAll(createImportedFunctionToolSpecs(action));
            }
        }
        // Job management tool
        result.add(wrapToolSpec(jobManager.getJobToolSpecification()));
        return result;
    }

    private List<SyncResourceTemplateSpecification> createResourceTemplateSpecs() {
        var result = new ArrayList<SyncResourceTemplateSpecification>();
        if (importFiles != null) {
            for (var importFile : importFiles) {
                var action = loadImportedAction(importFile);
                result.addAll(createImportedFunctionResourceTemplateSpecs(action));
            }
        }
        return result.stream().map(this::wrapResourceTemplateSpec).toList();
    }

    private List<SyncToolSpecification> createActionToolSpecs() {
        var actionSources = ActionSource.defaultActionSources(module.toString());
        var validationHandler = ActionValidationHandler.WARN;
        return ActionLoaderHelper.streamAsActions(actionSources, validationHandler)
                .filter(this::includeActionAsMcpTool)
            .map(a->wrapToolSpec(new ActionToolSpecHelper(module.toString(), a).createToolSpec()))
                .peek(s->log.debug("Registering action tool: {}", s.tool().name()))
                .toList();
    }

    private boolean includeActionAsMcpTool(Action action) {
        try {
            return action.getConfig()==null || action.getConfig().getMcp()!=ActionMcpIncludeExclude.exclude;
        } catch (Exception e) {
            log.warn("Error checking MCP include/exclude for action {}: {}", action!=null && action.getMetadata()!=null ? action.getMetadata().getName() : "<unknown>", e.toString());
            return false;
        }
    }

    private SyncToolSpecification createCommandToolSpec(CommandSpec spec) {
        return new CommandToolSpecHelper(spec).createToolSpec();
    }

    private SyncToolSpecification wrapToolSpec(SyncToolSpecification specification) {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(specification.tool())
                .callHandler((ctx, request) -> withSharedExecutionContext(() -> specification.callHandler().apply(ctx, request)))
                .build();
    }

    private SyncResourceTemplateSpecification wrapResourceTemplateSpec(SyncResourceTemplateSpecification specification) {
        return new SyncResourceTemplateSpecification(specification.resourceTemplate(),
                (ctx, request) -> withSharedExecutionContext(() -> specification.readHandler().apply(ctx, request)));
    }

    private <T> T withSharedExecutionContext(Supplier<T> supplier) {
        try (var frame = FcliExecutionContextHolder.push(new FcliExecutionContext(sharedIsolationScope, new FcliActionState()))) {
            return supplier.get();
        }
    }

    private Action loadImportedAction(String importFile) {
        var sources = ActionSource.externalActionSources(importFile);
        var validationHandler = ActionValidationHandler.WARN;
        return ActionLoaderHelper.load(sources, importFile, validationHandler).getAction();
    }

    private List<SyncToolSpecification> createImportedFunctionToolSpecs(Action action) {
        var result = new ArrayList<SyncToolSpecification>();
        for (var entry : action.getFunctions().entrySet()) {
            var function = entry.getValue();
            if (!function.isExported()) { continue; }
            if (hasMcpResourceMeta(function)) { continue; } // Resources handled separately
            var executor = new ActionFunctionExecutor(action, function, sharedFunctionFrameSupplier);
            var toolName = "fcli_fn_" + function.getKey().replace('-', '_');
            var schema = buildFunctionArgsSchema(function);
            var description = function.getDescription() != null ? function.getDescription() : function.getKey();
            IMCPToolRunner runner;
            if (function.isStreaming()) {
                new MCPToolArgHandlerPaging().updateSchema(schema);
                description = description + "\nPaging Guidance: This function streams records. "
                        + "When pagination.totalRecords is null, use the pagination.jobToken to wait for loading to complete.";
                runner = new MCPToolFcliRunnerFunctionStreaming(executor, jobManager, toolName);
            } else {
                runner = new MCPToolFcliRunnerFunction(executor, jobManager, toolName);
            }
            var tool = Tool.builder()
                    .name(toolName)
                    .description(description)
                    .inputSchema(schema)
                    .build();
            result.add(McpServerFeatures.SyncToolSpecification.builder()
                    .tool(tool)
                    .callHandler((ctx, request) -> withSharedExecutionContext(() -> runner.run(ctx, request)))
                    .build());
            log.debug("Registering function tool: {} (streaming={})", toolName, function.isStreaming());
        }
        return result;
    }

    private List<SyncResourceTemplateSpecification> createImportedFunctionResourceTemplateSpecs(Action action) {
        var result = new ArrayList<SyncResourceTemplateSpecification>();
        for (var entry : action.getFunctions().entrySet()) {
            var function = entry.getValue();
            if (!function.isExported()) { continue; }
            if (!hasMcpResourceMeta(function)) { continue; }
            var resourceMeta = getMcpResourceMeta(function);
            var uriTemplate = getMetaString(resourceMeta, "uri-template");
            if (uriTemplate == null) { continue; }
            var name = getMetaString(resourceMeta, "name");
            var mimeType = getMetaString(resourceMeta, "mime-type");
            var executor = new ActionFunctionExecutor(action, function, sharedFunctionFrameSupplier);
            var template = ResourceTemplate.builder()
                    .uriTemplate(uriTemplate)
                    .name(name != null ? name : function.getKey())
                    .description(function.getDescription())
                    .mimeType(mimeType != null ? mimeType : "application/json")
                    .build();
            var handler = new MCPResourceFcliRunnerFunction(executor, uriTemplate, mimeType);
            result.add(new SyncResourceTemplateSpecification(template, handler::read));
            log.debug("Registering function resource template: {}", uriTemplate);
        }
        return result;
    }

    private boolean hasMcpResourceMeta(ActionFunction function) {
        return function.getMeta() != null && function.getMeta().has("mcp.resource");
    }

    private JsonNode getMcpResourceMeta(ActionFunction function) {
        return function.getMeta().get("mcp.resource");
    }

    private String getMetaString(JsonNode meta, String key) {
        if (meta == null || !meta.has(key)) { return null; }
        var node = meta.get(key);
        return node.isTextual() ? node.asText() : null;
    }

    private JsonSchema buildFunctionArgsSchema(ActionFunction function) {
        var properties = new LinkedHashMap<String, Object>();
        var required = new ArrayList<String>();
        for (var argEntry : function.getArgsOrEmpty().entrySet()) {
            var argName = argEntry.getKey();
            var argDef = argEntry.getValue();
            var propNode = new LinkedHashMap<String, Object>();
            propNode.put("type", mapArgType(argDef.getType()));
            if (argDef.getDescription() != null) {
                propNode.put("description", argDef.getDescription());
            }
            properties.put(argName, propNode);
            if (Boolean.TRUE.equals(argDef.getRequired())) {
                required.add(argName);
            }
        }
        return new JsonSchema("object", properties, required, false,
                new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    private static String mapArgType(String type) {
        if (type == null) { return "string"; }
        return switch (type) {
            case "boolean" -> "boolean";
            case "int", "long" -> "integer";
            case "double", "float" -> "number";
            case "array" -> "string";
            default -> "string";
        };
    }


    private final class CommandToolSpecHelper {
        private final CommandSpec commandSpec;
        private final MCPToolArgHandlers toolSpecArgHelper;
        
        private CommandToolSpecHelper(CommandSpec commandSpec) {
            this.commandSpec = commandSpec;
            this.toolSpecArgHelper = new MCPToolArgHandlers(commandSpec);
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
            if ( toolSpecArgHelper.isPaged() ) {
                // Append paging guidance for LLM/client
                base = base + "\nPaging Guidance: This tool may return partial results if background record collection is still in progress. "
                        + "When pagination.totalRecords is null, call the job tool '"+MCPJobManager.JOB_TOOL_NAME+"' with operation=wait and the pagination.jobToken value to finalize loading and obtain totalRecords & totalPages.";
            }
            return base;
        }
        private IMCPToolRunner createRunner() {
            if ( FcliCommandSpecHelper.canCollectRecords(commandSpec) ) {
                if ( toolSpecArgHelper.isPaged() ) {
                    return new MCPToolFcliRunnerRecordsPaged(toolSpecArgHelper, commandSpec, jobManager);
                }
                return new MCPToolFcliRunnerRecords(toolSpecArgHelper, commandSpec, jobManager);
            }
            return new MCPToolFcliRunnerPlainText(toolSpecArgHelper, commandSpec, jobManager);
        }
    }

    private final class ActionToolSpecHelper {
        private final String moduleName;
        private final Action action;
        private final List<IMCPToolArgHandler> argHandlers;
        
        private ActionToolSpecHelper(String module, Action action) {
            this.moduleName = module;
            this.action = action;
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
            if ( action.getCliOptions()!=null ) {
                for ( Map.Entry<String, ActionCliOption> e : action.getCliOptions().entrySet() ) {
                    var opt = e.getValue();
                    if ( opt.getMcp()==ActionMcpIncludeExclude.exclude ) {
                        continue;
                    }
                    var name = getLongestName(opt);
                    if ( StringUtils.isBlank(name) ) {
                        continue;
                    }
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
            return "fcli_"+moduleName.replace('-', '_')+"_action_"+action.getMetadata().getName().replace('-', '_');
        }
        
        private String getDescription() {
            var usage = action.getUsage();
            return usage!=null ? usage.getHeader() : action.getMetadata().getName();
        }
        
        private String getLongestName(ActionCliOption opt) {
            var names = opt.getNamesAsArray();
            if ( names==null || names.length==0 ) {
                return null;
            }
            String longest=null;
            for ( var n : names ) {
                if ( longest==null || n.length()>longest.length() ) {
                    longest=n;
                }
            }
            return longest;
        }
    }

    public static enum McpModule {
        fod, ssc, sc_sast, sc_dast, aviator;
        
        @Override
        public String toString() {
            return name().replace('_', '-');
        }
        
        public boolean hasActionCmd() {
            return getModuleSpec().subcommands().containsKey("action");
        }
        
        public Stream<CommandSpec> getSubcommandsStream() {
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