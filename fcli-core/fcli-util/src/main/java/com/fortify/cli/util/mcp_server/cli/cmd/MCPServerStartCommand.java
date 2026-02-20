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
package com.fortify.cli.util.mcp_server.cli.cmd;

import java.time.Duration;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPToolSpecFactory;

import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Start.CMD_NAME)
@MCPExclude // Doesn't make sense to allow mcp-server start command to be called from MCP server
@Slf4j
public class MCPServerStartCommand extends AbstractRunnableCommand {
    @Option(names={"--module", "-m"}, required = true) private McpModule module;
    @Option(names={"--work-threads"}, defaultValue="10") private int workThreads;
    @Option(names={"--progress-threads"}, defaultValue="4") private int progressThreads;
    @Option(names={"--job-safe-return"}, defaultValue="25s") private String jobSafeReturnPeriod;
    @Option(names={"--progress-interval"}, defaultValue="5s") private String progressIntervalPeriod;
    static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MILLISECONDS, Period.MINUTES);

    @Override
    public Integer call() throws Exception {
        long safeReturnMillis = PERIOD_HELPER.parsePeriodToMillis(jobSafeReturnPeriod);
        long progressIntervalMillis = PERIOD_HELPER.parsePeriodToMillis(progressIntervalPeriod);
        if ( safeReturnMillis<=0 ) {
            safeReturnMillis = 25000;
        }
        if ( progressIntervalMillis<=0 ) {
            progressIntervalMillis = 500;
        }
        var jobManager = new MCPJobManager(module.toString(), workThreads, progressThreads, safeReturnMillis, progressIntervalMillis);
        var toolSpecs = MCPToolSpecFactory.createToolSpecs(module, jobManager);
        var objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        var server = McpServer.sync(new StdioServerTransportProvider(new JacksonMcpJsonMapper(objectMapper)))
                .serverInfo("fcli", FcliBuildProperties.INSTANCE.getFcliVersion())
                .requestTimeout(Duration.ofSeconds(120))
                .instructions("""
                        - For tools that accept a --*-session option and user hasn't asked for a specific \
                        session, inform the user that the 'default' session will be used.
                        """)
                .capabilities(getServerCapabilities())
                .tools(toolSpecs)
                .build();
    log.debug("Initialized MCP server instance: {}", server);
    log.info("Fcli MCP server running on stdio");
        System.err.println("Fcli MCP server running on stdio. Hit Ctrl-C to exit.");
        Thread.getAllStackTraces().keySet().stream()
            .filter(t->!t.isDaemon() && t!=Thread.currentThread())
            .forEach(t-> {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    log.warn("Interrupted while joining thread {}", t.getName(), e);
                    Thread.currentThread().interrupt();
                }
            });
        return 0;
    }

    static ServerCapabilities getServerCapabilities() {
        return ServerCapabilities.builder()
                .resources(false, false)
                .prompts(false)
                .tools(true)
                .build();
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
