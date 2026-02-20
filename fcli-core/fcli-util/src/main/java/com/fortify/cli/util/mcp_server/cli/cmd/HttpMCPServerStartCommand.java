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

import java.io.IOException;
import java.time.Duration;
import java.util.EnumSet;

import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.rest.unirest.HttpMcpAuthContext;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.util.mcp_server.cli.cmd.MCPServerStartCommand.McpModule;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPToolSpecFactory;

import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "http-start")
@MCPExclude
@Slf4j
public class HttpMCPServerStartCommand extends AbstractRunnableCommand {
    @Option(names = {"--ssc-url"}, required = true) private String sscUrl;
    @Option(names = {"--port"}, defaultValue = "8080") private int port;
    @Option(names = {"--work-threads"}, defaultValue = "10") private int workThreads;
    @Option(names = {"--progress-threads"}, defaultValue = "4") private int progressThreads;
    @Option(names = {"--job-safe-return"}, defaultValue = "25s") private String jobSafeReturnPeriod;
    @Option(names = {"--progress-interval"}, defaultValue = "5s") private String progressIntervalPeriod;

    @Override
    public Integer call() throws Exception {
        long safeReturnMillis = MCPServerStartCommand.PERIOD_HELPER.parsePeriodToMillis(jobSafeReturnPeriod);
        long progressIntervalMillis = MCPServerStartCommand.PERIOD_HELPER.parsePeriodToMillis(progressIntervalPeriod);
        if (safeReturnMillis <= 0) { safeReturnMillis = 25000; }
        if (progressIntervalMillis <= 0) { progressIntervalMillis = 500; }

        // Normalize SSC URL: strip trailing slashes
        var normalizedSscUrl = sscUrl.replaceAll("/+$", "");

        var jobManager = new MCPJobManager(McpModule.ssc.toString(), workThreads, progressThreads, safeReturnMillis, progressIntervalMillis);
        var toolSpecs = MCPToolSpecFactory.createToolSpecs(McpModule.ssc, jobManager);
        var objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        var transportProvider = HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .mcpEndpoint("/mcp")
                .build();

        McpServer.sync(transportProvider)
                .serverInfo("fcli", FcliBuildProperties.INSTANCE.getFcliVersion())
                .requestTimeout(Duration.ofSeconds(120))
                .instructions("HTTP MCP server for SSC. Authentication is provided via the Authorization header on each request.")
                .capabilities(MCPServerStartCommand.getServerCapabilities())
                .tools(toolSpecs)
                .build();

        // Set up embedded Jetty
        var server = new Server(port);
        var context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");

        // Auth filter: extract Bearer token, set ThreadLocal
        context.addFilter(
                new FilterHolder(new BearerAuthFilter(normalizedSscUrl)),
                "/mcp",
                EnumSet.of(DispatcherType.REQUEST));

        // Register the MCP transport servlet
        context.addServlet(new ServletHolder(transportProvider), "/mcp");
        server.setHandler(context);

        server.start();
        log.info("Fcli HTTP MCP server listening on port {}", port);
        System.err.println("Fcli HTTP MCP server listening on port " + port + ". Hit Ctrl-C to exit.");
        server.join();
        return 0;
    }

    /**
     * Servlet filter that extracts a Bearer token from the Authorization header
     * and sets the {@link HttpMcpAuthContext} ThreadLocal for downstream handlers.
     */
    private static class BearerAuthFilter implements Filter {
        private final String sscUrl;

        BearerAuthFilter(String sscUrl) {
            this.sscUrl = sscUrl;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            var httpReq = (HttpServletRequest) request;
            var httpResp = (HttpServletResponse) response;
            var authHeader = httpReq.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResp.setContentType("application/json");
                httpResp.getWriter().write("{\"error\":\"Missing or invalid Authorization header. Expected: Bearer <token>\"}");
                return;
            }
            var token = authHeader.substring("Bearer ".length()).toCharArray();
            HttpMcpAuthContext.set(new HttpMcpAuthContext.AuthInfo(sscUrl, token));
            try {
                chain.doFilter(request, response);
            } finally {
                HttpMcpAuthContext.clear();
            }
        }
    }
}
