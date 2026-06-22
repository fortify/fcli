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

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.ai_assist.mcp.helper.MCPImportedActionMcpSpecsFactory;
import com.fortify.cli.ai_assist.mcp.helper.MCPJobManager;
import com.fortify.cli.ai_assist.mcp.helper.http.JdkHttpServerMcpStatelessTransport;
import com.fortify.cli.ai_assist.mcp.helper.http.MCPServerHttpAuthHeaderParser;
import com.fortify.cli.ai_assist.mcp.helper.http.MCPServerHttpConfig;
import com.fortify.cli.ai_assist.mcp.helper.http.MCPServerHttpConfigLoader;
import com.fortify.cli.ai_assist.mcp.helper.http.MCPServerHttpSessionDescriptorResolver;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliActionState;
import com.fortify.cli.common.cli.util.FcliExecutionContext;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.cli.util.FcliIsolationScope;
import com.fortify.cli.common.cli.util.IFcliExecutionContextManager;
import com.fortify.cli.common.cli.util.StdioHelper;
import com.fortify.cli.common.concurrent.job.AsyncJobManager;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.log.LogMaskContext;
import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.session.helper.AbstractSessionHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.util.FcliBuildProperties;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "start-http")
@MCPExclude
@Slf4j
public class AiAssistMCPStartHttpCommand extends AbstractRunnableCommand implements IFcliExecutionContextManager {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MILLISECONDS, Period.MINUTES);

    @Option(names = {"--config", "-c"}, required = true)
    private Path configPath;

    @Override
    public Integer call() throws Exception {
        suppressProgressOutput();
        var config = MCPServerHttpConfigLoader.load(configPath);
        var asyncJobManager = new AsyncJobManager(AsyncJobManager.Config.builder()
                .bgThreads(config.getJobs().getAsyncBgThreads()).build());
        var jobManager = createJobManager(config, asyncJobManager);
        var authHeaderParser = new MCPServerHttpAuthHeaderParser(config);
        var sessionDescriptorResolver = new MCPServerHttpSessionDescriptorResolver(config);
        var scopeCleanupScheduler = scheduleScopeCleanup(config, sessionDescriptorResolver);
        var specs = collectMcpSpecs(config, jobManager, sessionDescriptorResolver, authHeaderParser);
        var transport = createTransport(config);
        buildAndStartServer(config, transport, specs);
        awaitShutdown(transport, asyncJobManager, scopeCleanupScheduler, sessionDescriptorResolver);
        return 0;
    }

    private void suppressProgressOutput() {
        StdioHelper.setProgressOut(null);
        StdioHelper.setProgressErr(null);
    }

    private MCPJobManager createJobManager(MCPServerHttpConfig config, AsyncJobManager asyncJobManager) {
        var jobsConfig = config.getJobs();
        var safeReturnMillis = PERIOD_HELPER.parsePeriodToMillis(jobsConfig.getSafeReturn());
        var progressIntervalMillis = PERIOD_HELPER.parsePeriodToMillis(jobsConfig.getProgressInterval());
        if (safeReturnMillis <= 0) { safeReturnMillis = 25000; }
        if (progressIntervalMillis <= 0) { progressIntervalMillis = 500; }
        return new MCPJobManager(
                jobsConfig.getWorkThreads(),
                jobsConfig.getProgressThreads(),
                safeReturnMillis,
                progressIntervalMillis,
                asyncJobManager);
    }

    private ScheduledExecutorService scheduleScopeCleanup(MCPServerHttpConfig config,
            MCPServerHttpSessionDescriptorResolver sessionDescriptorResolver) {
        var scheduler = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "mcp-http-scope-cleanup"));
        sessionDescriptorResolver.scheduleCleanup(config.getJobs().getIsolationScopeTtlInMillis(), scheduler);
        return scheduler;
    }

    private record McpSpecs(
            ArrayList<McpStatelessServerFeatures.SyncToolSpecification> tools,
            ArrayList<McpStatelessServerFeatures.SyncResourceTemplateSpecification> resourceTemplates) {}

    private McpSpecs collectMcpSpecs(MCPServerHttpConfig config, MCPJobManager jobManager,
            MCPServerHttpSessionDescriptorResolver sessionDescriptorResolver,
            MCPServerHttpAuthHeaderParser authHeaderParser) {
        var importSpecsFactory = new MCPImportedActionMcpSpecsFactory(jobManager,
                () -> sessionDescriptorResolver.getOrCreateFunctionFrame(FcliExecutionContextHolder.getMcpRequestAuthScopeKey()));
        var toolSpecs = new ArrayList<McpStatelessServerFeatures.SyncToolSpecification>();
        var resourceTemplateSpecs = new ArrayList<McpStatelessServerFeatures.SyncResourceTemplateSpecification>();
        for (var importPath : config.getResolvedImportPaths()) {
            var importedSpecs = importSpecsFactory.create(importPath);
            importedSpecs.tools().forEach(tool -> toolSpecs.add(McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(tool.tool())
                .callHandler((ctx, request) -> withRequestExecutionContext(ctx, sessionDescriptorResolver, authHeaderParser,
                    () -> tool.callHandler().apply(ctx, request)))
                .build()));
            importedSpecs.resourceTemplates().forEach(resourceTemplate -> resourceTemplateSpecs.add(
                new McpStatelessServerFeatures.SyncResourceTemplateSpecification(
                    resourceTemplate.resourceTemplate(),
                    (ctx, request) -> withRequestExecutionContext(ctx, sessionDescriptorResolver, authHeaderParser,
                        () -> resourceTemplate.readHandler().apply(ctx, request))
                )));
        }
        var jobToolSpec = jobManager.getJobToolSpecification();
        toolSpecs.add(McpStatelessServerFeatures.SyncToolSpecification.builder()
            .tool(jobToolSpec.tool())
            .callHandler((ctx, request) -> withRequestExecutionContext(ctx, sessionDescriptorResolver, authHeaderParser,
                () -> jobToolSpec.callHandler().apply(null, request)))
            .build());
        if (toolSpecs.size() == 1) {
            throw new FcliSimpleException("HTTP MCP config imports did not produce any exported functions");
        }
        return new McpSpecs(toolSpecs, resourceTemplateSpecs);
    }

    private JdkHttpServerMcpStatelessTransport createTransport(MCPServerHttpConfig config) throws IOException {
        var objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new JdkHttpServerMcpStatelessTransport(config.getServer(), "/mcp", new JacksonMcpJsonMapper(objectMapper));
    }

    private void buildAndStartServer(MCPServerHttpConfig config,
            JdkHttpServerMcpStatelessTransport transport, McpSpecs specs) {
        var tlsConfigured = config.getServer().getTls() != null;
        var serverBuilder = McpServer.sync(transport)
                .serverInfo("fcli", FcliBuildProperties.INSTANCE.getFcliVersion())
                .requestTimeout(Duration.ofSeconds(120))
                .instructions("HTTP MCP server exposing imported fcli action functions")
                .capabilities(getServerCapabilities(!specs.resourceTemplates().isEmpty()))
                .tools(specs.tools());
        if (!specs.resourceTemplates().isEmpty()) {
            serverBuilder.resourceTemplates(specs.resourceTemplates());
        }
        var mcpServer = serverBuilder.build();
        log.debug("Initialized HTTP MCP server instance: {}", mcpServer);
        transport.start();
        if (!tlsConfigured) {
            log.warn("Starting HTTP MCP server without TLS certificates. Use this mode for testing only; use HTTPS with certificates in production");
        }
        log.info("Fcli HTTP MCP server running on port {} for product {}", config.getServer().getPort(), config.getProduct());
        System.err.println("Fcli HTTP MCP server running on port " + config.getServer().getPort() + " endpoint /mcp. Hit Ctrl-C to exit.");
    }

    private void awaitShutdown(JdkHttpServerMcpStatelessTransport transport,
            AsyncJobManager asyncJobManager,
            ScheduledExecutorService scopeCleanupScheduler,
            MCPServerHttpSessionDescriptorResolver sessionDescriptorResolver) throws InterruptedException {
        var latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            transport.close();
            asyncJobManager.shutdown();
            scopeCleanupScheduler.shutdown();
            sessionDescriptorResolver.shutdown();
            latch.countDown();
        }, "mcp-http-shutdown-hook"));
        latch.await();
    }

    private <T> T withRequestExecutionContext(McpTransportContext transportContext,
            MCPServerHttpSessionDescriptorResolver sessionDescriptorResolver,
            MCPServerHttpAuthHeaderParser authHeaderParser,
            Supplier<T> supplier)
    {
        var requestLogMaskCtx = new LogMaskContext();
        // Temp frame: push an empty scope so activeContext() = requestLogMaskCtx.
        // This ensures X-AUTH credentials and any values discovered by global patterns
        // (e.g. FoD OAuth token from the token-fetch response) are captured per-request.
        try (var tempFrame = FcliExecutionContextHolder.push(
                new FcliExecutionContext(new FcliIsolationScope(), new FcliActionState(), requestLogMaskCtx))) {
            var auth = authHeaderParser.parseAndRegister(transportContext);
            var isolationScope = sessionDescriptorResolver.getOrCreateIsolationScope(auth);
            // Real frame: same requestLogMaskCtx, real isolation scope.
            try (var frame = FcliExecutionContextHolder.push(
                    new FcliExecutionContext(isolationScope, new FcliActionState(), requestLogMaskCtx))) {
                // Register current tokens from transient session descriptor so they are
                // masked in this request's log output (mirrors AbstractSessionHelper.get() for
                // disk-backed sessions; needed here because transient sessions bypass that path).
                isolationScope.getTransientSessionDescriptors().values()
                        .forEach(AbstractSessionHelper::registerLogMasks);
                return supplier.get();
            }
        }
    }

    private static ServerCapabilities getServerCapabilities(boolean hasResources) {
        return ServerCapabilities.builder()
                .resources(hasResources, false)
                .prompts(false)
                .tools(true)
                .build();
    }
}