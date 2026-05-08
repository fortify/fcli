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
package com.fortify.cli.agent.mcp.cli.cmd;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.agent.mcp.helper.MCPImportedActionMcpSpecsFactory;
import com.fortify.cli.agent.mcp.helper.MCPJobManager;
import com.fortify.cli.agent.mcp.helper.http.JdkHttpServerMcpStatelessTransport;
import com.fortify.cli.agent.mcp.helper.http.MCPServerHttpConfigLoader;
import com.fortify.cli.agent.mcp.helper.http.MCPServerHttpSessionDescriptorResolver;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliExecutionContext;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.concurrent.job.AsyncJobManager;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.mcp.MCPExclude;
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
public class AgentMCPStartHttpCommand extends AbstractRunnableCommand {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MILLISECONDS, Period.MINUTES);

    @Option(names = {"--config", "-c"}, required = true)
    private Path configPath;

    @Override
    public Integer call() throws Exception {
        var config = MCPServerHttpConfigLoader.load(configPath);

        var safeReturnMillis = PERIOD_HELPER.parsePeriodToMillis(config.getJobSafeReturn());
        var progressIntervalMillis = PERIOD_HELPER.parsePeriodToMillis(config.getProgressInterval());
        if ( safeReturnMillis <= 0 ) {
            safeReturnMillis = 25000;
        }
        if ( progressIntervalMillis <= 0 ) {
            progressIntervalMillis = 500;
        }

        var asyncJobManager = new AsyncJobManager(AsyncJobManager.Config.builder().bgThreads(config.getAsyncBgThreads()).build());
        var jobManager = new MCPJobManager(
                config.getWorkThreads(),
                config.getProgressThreads(),
                safeReturnMillis,
                progressIntervalMillis,
                asyncJobManager
        );

        var sharedFunctionContext = new FcliExecutionContext();
        var importSpecsFactory = new MCPImportedActionMcpSpecsFactory(jobManager, sharedFunctionContext);
        var sessionDescriptorResolver = new MCPServerHttpSessionDescriptorResolver(config);
        var toolSpecs = new ArrayList<McpStatelessServerFeatures.SyncToolSpecification>();
        var resourceTemplateSpecs = new ArrayList<McpStatelessServerFeatures.SyncResourceTemplateSpecification>();
        for ( var importPath : config.getResolvedImportPaths() ) {
            var importedSpecs = importSpecsFactory.create(importPath);
            importedSpecs.tools().forEach(tool -> toolSpecs.add(McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(tool.tool())
                .callHandler((ctx, request) -> withRequestExecutionContext(ctx, sessionDescriptorResolver,
                    () -> tool.callHandler().apply(ctx, request)))
                .build()));
            importedSpecs.resourceTemplates().forEach(resourceTemplate -> resourceTemplateSpecs.add(
                new McpStatelessServerFeatures.SyncResourceTemplateSpecification(
                    resourceTemplate.resourceTemplate(),
                    (ctx, request) -> withRequestExecutionContext(ctx, sessionDescriptorResolver,
                        () -> resourceTemplate.readHandler().apply(ctx, request))
                )));
        }
        var jobToolSpec = jobManager.getJobToolSpecification();
        toolSpecs.add(McpStatelessServerFeatures.SyncToolSpecification.builder()
            .tool(jobToolSpec.tool())
            .callHandler((ctx, request) -> withRequestExecutionContext(ctx, sessionDescriptorResolver,
                () -> jobToolSpec.callHandler().apply(null, request)))
            .build());

        if ( toolSpecs.size() == 1 ) {
            throw new FcliSimpleException("HTTP MCP config imports did not produce any exported functions");
        }

        var objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        var transport = new JdkHttpServerMcpStatelessTransport(config.getPort(), "/mcp", new JacksonMcpJsonMapper(objectMapper));

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
        log.info("Fcli HTTP MCP server running on port {} for product {}", config.getPort(), config.getProduct());
        System.err.println("Fcli HTTP MCP server running on port " + config.getPort() + " endpoint /mcp. Hit Ctrl-C to exit.");

        var latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            transport.close();
            asyncJobManager.shutdown();
            latch.countDown();
        }, "mcp-http-shutdown-hook"));
        latch.await();
        return 0;
    }

    private <T> T withRequestExecutionContext(McpTransportContext transportContext,
            MCPServerHttpSessionDescriptorResolver sessionDescriptorResolver,
            Supplier<T> supplier)
    {
        var executionContext = FcliExecutionContextHolder.pushNew();
        try {
            // HTTP MCP is stateless, so per-request auth/session data must be attached here
            // for downstream session resolution and paged/background job isolation.
            executionContext.setMcpRequestAuthScopeKey(sessionDescriptorResolver.getAuthScopeKey(transportContext));
            executionContext.setTransientSessionDescriptor(sessionDescriptorResolver.getOrCreateSessionDescriptor(transportContext));
            return supplier.get();
        } finally {
            FcliExecutionContextHolder.pop();
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