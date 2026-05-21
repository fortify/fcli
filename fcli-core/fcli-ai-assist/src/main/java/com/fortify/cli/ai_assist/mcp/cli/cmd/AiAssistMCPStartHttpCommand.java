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

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.ai_assist.mcp.helper.MCPImportedActionMcpSpecsFactory;
import com.fortify.cli.ai_assist.mcp.helper.MCPJobManager;
import com.fortify.cli.ai_assist.mcp.helper.http.JdkHttpServerMcpStatelessTransport;
import com.fortify.cli.ai_assist.mcp.helper.http.MCPServerHttpAuthHeaderParser;
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
        // Suppress progress output — HTTP server has no stdio protocol channel to protect,
        // so progress messages on stdout/stderr are unwanted console noise
        StdioHelper.setProgressOut(null);
        StdioHelper.setProgressErr(null);

        var config = MCPServerHttpConfigLoader.load(configPath);

        var safeReturnMillis = PERIOD_HELPER.parsePeriodToMillis(config.getJobs().getSafeReturn());
        var progressIntervalMillis = PERIOD_HELPER.parsePeriodToMillis(config.getJobs().getProgressInterval());
        if ( safeReturnMillis <= 0 ) {
            safeReturnMillis = 25000;
        }
        if ( progressIntervalMillis <= 0 ) {
            progressIntervalMillis = 500;
        }

        var asyncJobManager = new AsyncJobManager(AsyncJobManager.Config.builder().bgThreads(config.getJobs().getAsyncBgThreads()).build());
        var jobManager = new MCPJobManager(
                config.getJobs().getWorkThreads(),
                config.getJobs().getProgressThreads(),
                safeReturnMillis,
                progressIntervalMillis,
                asyncJobManager
        );

        var authHeaderParser = new MCPServerHttpAuthHeaderParser(config);
        var sessionDescriptorResolver = new MCPServerHttpSessionDescriptorResolver(config);
        var scopeCleanupScheduler = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "mcp-http-scope-cleanup"));
        sessionDescriptorResolver.scheduleCleanup(config.getJobs().getIsolationScopeTtlInMillis(), scopeCleanupScheduler);
        var importSpecsFactory = new MCPImportedActionMcpSpecsFactory(jobManager,
                () -> sessionDescriptorResolver.getOrCreateFunctionFrame(FcliExecutionContextHolder.getMcpRequestAuthScopeKey()));
        var toolSpecs = new ArrayList<McpStatelessServerFeatures.SyncToolSpecification>();
        var resourceTemplateSpecs = new ArrayList<McpStatelessServerFeatures.SyncResourceTemplateSpecification>();
        for ( var importPath : config.getResolvedImportPaths() ) {
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

        if ( toolSpecs.size() == 1 ) {
            throw new FcliSimpleException("HTTP MCP config imports did not produce any exported functions");
        }

        var objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        var transport = new JdkHttpServerMcpStatelessTransport(config.getServer(), "/mcp", new JacksonMcpJsonMapper(objectMapper));

        var serverBuilder = McpServer.sync(transport)
                .serverInfo("fcli", FcliBuildProperties.INSTANCE.getFcliVersion())
                .requestTimeout(Duration.ofSeconds(120))
                .instructions("HTTP MCP server exposing imported fcli action functions")
                .capabilities(getServerCapabilities(!resourceTemplateSpecs.isEmpty()))
                .tools(toolSpecs);
        if ( !resourceTemplateSpecs.isEmpty() ) {
            serverBuilder.resourceTemplates(resourceTemplateSpecs);
        }
        var mcpServer = serverBuilder.build();
        log.debug("Initialized HTTP MCP server instance: {}", mcpServer);

        transport.start();
        log.info("Fcli HTTP MCP server running on port {} for product {}", config.getServer().getPort(), config.getProduct());
        System.err.println("Fcli HTTP MCP server running on port " + config.getServer().getPort() + " endpoint /mcp. Hit Ctrl-C to exit.");

        var latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            transport.close();
            asyncJobManager.shutdown();
            scopeCleanupScheduler.shutdown();
            sessionDescriptorResolver.shutdown();
            latch.countDown();
        }, "mcp-http-shutdown-hook"));
        latch.await();
        return 0;
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