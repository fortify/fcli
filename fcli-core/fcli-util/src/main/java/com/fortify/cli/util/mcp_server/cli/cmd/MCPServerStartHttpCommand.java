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

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliExecutionContext;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.rest.unirest.config.UrlConfig;
import com.fortify.cli.common.session.helper.ISessionDescriptor;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;
import com.fortify.cli.fod._common.session.helper.FoDSessionDescriptor;
import com.fortify.cli.fod._common.session.helper.oauth.FoDOAuthHelper;
import com.fortify.cli.fod._common.session.helper.oauth.FoDTokenCreateResponse;
import com.fortify.cli.fod._common.session.helper.oauth.IFoDClientCredentials;
import com.fortify.cli.fod._common.session.helper.oauth.IFoDUserCredentials;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCAndScanCentralSessionLoginOptions.SSCAndScanCentralUrlConfigOptions.SSCComponentDisable;
import com.fortify.cli.ssc._common.session.helper.ISSCAndScanCentralCredentialsConfig;
import com.fortify.cli.ssc._common.session.helper.ISSCAndScanCentralUrlConfig;
import com.fortify.cli.ssc._common.session.helper.ISSCUserCredentialsConfig;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionDescriptor;
import com.fortify.cli.ssc.access_control.helper.SSCTokenGetOrCreateResponse.SSCTokenData;
import com.fortify.cli.util._common.helper.AsyncJobManager;
import com.fortify.cli.util.mcp_server.helper.http.JdkHttpServerMcpStatelessTransport;
import com.fortify.cli.util.mcp_server.helper.http.MCPServerHttpConfig;
import com.fortify.cli.util.mcp_server.helper.http.MCPServerHttpConfig.Product;
import com.fortify.cli.util.mcp_server.helper.http.MCPServerHttpConfigLoader;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPImportedActionMcpSpecsFactory;
import com.fortify.cli.util.mcp_server.helper.mcp.MCPJobManager;

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
public class MCPServerStartHttpCommand extends AbstractRunnableCommand {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MILLISECONDS, Period.MINUTES);
    private static final String HEADER_SSC_TOKEN = "X-FCLI-SSC-TOKEN";
    private static final String HEADER_SC_SAST_CLIENT_AUTH_TOKEN = "X-FCLI-SC-SAST-CLIENT-AUTH-TOKEN";
    private static final String HEADER_FOD_TENANT = "X-FCLI-FOD-TENANT";
    private static final String HEADER_FOD_USER = "X-FCLI-FOD-USER";
    private static final String HEADER_FOD_PAT = "X-FCLI-FOD-PAT";
    private static final String HEADER_FOD_CLIENT_ID = "X-FCLI-FOD-CLIENT-ID";
    private static final String HEADER_FOD_CLIENT_SECRET = "X-FCLI-FOD-CLIENT-SECRET";
    private static final String[] DEFAULT_FOD_SCOPES = new String[] {"api-tenant"};

    @Option(names = {"--config"}, required = true)
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
        var toolSpecs = new ArrayList<McpStatelessServerFeatures.SyncToolSpecification>();
        var resourceTemplateSpecs = new ArrayList<McpStatelessServerFeatures.SyncResourceTemplateSpecification>();
        var sessionDescriptorCache = new ConcurrentHashMap<String, ISessionDescriptor>();
        for ( var importPath : config.getResolvedImportPaths() ) {
            var importedSpecs = importSpecsFactory.create(importPath);
            importedSpecs.tools().forEach(tool -> toolSpecs.add(McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(tool.tool())
                .callHandler((ctx, request) -> withRequestExecutionContext(ctx, config.getProduct(), sessionDescriptorCache,
                    () -> tool.callHandler().apply(ctx, request), config))
                .build()));
            importedSpecs.resourceTemplates().forEach(resourceTemplate -> resourceTemplateSpecs.add(
                new McpStatelessServerFeatures.SyncResourceTemplateSpecification(
                    resourceTemplate.resourceTemplate(),
                    (ctx, request) -> withRequestExecutionContext(ctx, config.getProduct(), sessionDescriptorCache,
                        () -> resourceTemplate.readHandler().apply(ctx, request), config)
                )));
        }
        var jobToolSpec = jobManager.getJobToolSpecification();
        toolSpecs.add(McpStatelessServerFeatures.SyncToolSpecification.builder()
            .tool(jobToolSpec.tool())
            .callHandler((ctx, request) -> withRequestExecutionContext(ctx, config.getProduct(), sessionDescriptorCache,
                () -> jobToolSpec.callHandler().apply(null, request), config))
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

    private <T> T withRequestExecutionContext(McpTransportContext transportContext, Product product,
            Map<String, ISessionDescriptor> sessionDescriptorCache, Supplier<T> supplier,
            MCPServerHttpConfig config)
    {
        var executionContext = FcliExecutionContextHolder.pushNew();
        try {
            executionContext.setTransientSessionDescriptor(getOrCreateSessionDescriptor(transportContext, product, sessionDescriptorCache, config));
            return supplier.get();
        } finally {
            FcliExecutionContextHolder.pop();
        }
    }

    private ISessionDescriptor getOrCreateSessionDescriptor(McpTransportContext transportContext, Product product,
            Map<String, ISessionDescriptor> sessionDescriptorCache,
            MCPServerHttpConfig config)
    {
        var cacheKey = createAuthCacheKey(transportContext, product);
        return sessionDescriptorCache.computeIfAbsent(cacheKey, k -> createSessionDescriptor(product, transportContext, config));
    }

    private String createAuthCacheKey(McpTransportContext transportContext, Product product) {
        return switch (product) {
        case ssc -> String.format("ssc|%s|%s",
                getRequiredHeader(transportContext, HEADER_SSC_TOKEN),
                StringUtils.defaultString(getOptionalHeader(transportContext, HEADER_SC_SAST_CLIENT_AUTH_TOKEN)));
        case fod -> createFoDAuthCacheKey(transportContext);
        };
    }

    private String createFoDAuthCacheKey(McpTransportContext transportContext) {
        var clientId = getOptionalHeader(transportContext, HEADER_FOD_CLIENT_ID);
        var clientSecret = getOptionalHeader(transportContext, HEADER_FOD_CLIENT_SECRET);
        var tenant = getOptionalHeader(transportContext, HEADER_FOD_TENANT);
        var user = getOptionalHeader(transportContext, HEADER_FOD_USER);
        var pat = getOptionalHeader(transportContext, HEADER_FOD_PAT);
        if ( StringUtils.isNotBlank(clientId) || StringUtils.isNotBlank(clientSecret) ) {
            if ( StringUtils.isAnyBlank(clientId, clientSecret) ) {
                throw new FcliSimpleException("FoD client authentication requires both %s and %s", HEADER_FOD_CLIENT_ID, HEADER_FOD_CLIENT_SECRET);
            }
            if ( StringUtils.isNotBlank(tenant) || StringUtils.isNotBlank(user) || StringUtils.isNotBlank(pat) ) {
                throw new FcliSimpleException("Specify either FoD client headers (%s, %s) or FoD user headers (%s, %s, %s)",
                        HEADER_FOD_CLIENT_ID, HEADER_FOD_CLIENT_SECRET,
                        HEADER_FOD_TENANT, HEADER_FOD_USER, HEADER_FOD_PAT);
            }
            return "fod-client|" + clientId + "|" + clientSecret;
        }
        if ( StringUtils.isAnyBlank(tenant, user, pat) ) {
            throw new FcliSimpleException("FoD user authentication requires headers %s, %s, and %s",
                    HEADER_FOD_TENANT, HEADER_FOD_USER, HEADER_FOD_PAT);
        }
        return "fod-user|" + tenant + "|" + user + "|" + pat;
    }

    private ISessionDescriptor createSessionDescriptor(Product product, McpTransportContext transportContext,
            MCPServerHttpConfig config)
    {
        return switch (product) {
        case ssc -> createSscSessionDescriptor(transportContext, config);
        case fod -> createFoDSessionDescriptor(transportContext, config);
        };
    }

    private ISessionDescriptor createSscSessionDescriptor(McpTransportContext transportContext, MCPServerHttpConfig config)
    {
        var token = getRequiredHeader(transportContext, HEADER_SSC_TOKEN);
        var tokenData = new SSCTokenData();
        tokenData.setToken(token.toCharArray());
        var sscConfig = config.getSsc();
        var scSastClientAuthToken = StringUtils.firstNonBlank(
                sscConfig.getScSastClientAuthToken(),
                getOptionalHeader(transportContext, HEADER_SC_SAST_CLIENT_AUTH_TOKEN)
        );
        return SSCAndScanCentralSessionDescriptor.create(
            new HttpMcpSscUrlConfig(sscConfig),
                new HttpMcpSscCredentialsConfig(tokenData.getToken(),
                        StringUtils.isBlank(scSastClientAuthToken) ? null : scSastClientAuthToken.toCharArray())
        );
    }

    private ISessionDescriptor createFoDSessionDescriptor(McpTransportContext transportContext, MCPServerHttpConfig config)
    {
        var fodConfig = config.getFod();
        var urlConfig = UrlConfig.builderFromConnectionConfig(fodConfig)
            .url(FoDProductHelper.INSTANCE.getApiUrl(fodConfig.getUrl()))
            .build();
        var fodTokenResponse = createFoDTokenResponse(transportContext, urlConfig);
        return new FoDSessionDescriptor(urlConfig, fodTokenResponse);
    }

    private FoDTokenCreateResponse createFoDTokenResponse(McpTransportContext transportContext, UrlConfig urlConfig) {
        var clientId = getOptionalHeader(transportContext, HEADER_FOD_CLIENT_ID);
        var clientSecret = getOptionalHeader(transportContext, HEADER_FOD_CLIENT_SECRET);
        if ( StringUtils.isNotBlank(clientId) || StringUtils.isNotBlank(clientSecret) ) {
            return FoDOAuthHelper.createToken(urlConfig,
                    new HttpMcpFoDClientCredentials(
                            getRequiredHeader(transportContext, HEADER_FOD_CLIENT_ID),
                            getRequiredHeader(transportContext, HEADER_FOD_CLIENT_SECRET)
                    ),
                    DEFAULT_FOD_SCOPES
            );
        }
        return FoDOAuthHelper.createToken(urlConfig,
                new HttpMcpFoDUserCredentials(
                        getRequiredHeader(transportContext, HEADER_FOD_TENANT),
                        getRequiredHeader(transportContext, HEADER_FOD_USER),
                        getRequiredHeader(transportContext, HEADER_FOD_PAT).toCharArray()
                ),
                DEFAULT_FOD_SCOPES
        );
    }

    @SuppressWarnings("unchecked")
    private String getOptionalHeader(McpTransportContext transportContext, String headerName) {
        var headers = (Map<String, java.util.List<String>>)transportContext.get("headers");
        if ( headers == null || headers.isEmpty() ) {
            return null;
        }
        return headers.entrySet().stream()
                .filter(e -> headerName.equalsIgnoreCase(e.getKey()))
                .map(Map.Entry::getValue)
                .filter(v -> v != null && !v.isEmpty())
                .map(v -> v.get(0))
                .map(StringUtils::trimToNull)
                .findFirst().orElse(null);
    }

    private String getRequiredHeader(McpTransportContext transportContext, String headerName) {
        var value = getOptionalHeader(transportContext, headerName);
        if ( StringUtils.isBlank(value) ) {
            throw new FcliSimpleException("Missing required HTTP header: %s", headerName);
        }
        return value;
    }

    private static final class HttpMcpFoDClientCredentials implements IFoDClientCredentials {
        private final String clientId;
        private final String clientSecret;

        private HttpMcpFoDClientCredentials(String clientId, String clientSecret) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }

        @Override
        public String getClientId() {
            return clientId;
        }

        @Override
        public String getClientSecret() {
            return clientSecret;
        }
    }

    private static final class HttpMcpFoDUserCredentials implements IFoDUserCredentials {
        private final String tenant;
        private final String user;
        private final char[] password;

        private HttpMcpFoDUserCredentials(String tenant, String user, char[] password) {
            this.tenant = tenant;
            this.user = user;
            this.password = password;
        }

        @Override
        public String getUser() {
            return user;
        }

        @Override
        public char[] getPassword() {
            return password;
        }

        @Override
        public String getTenant() {
            return tenant;
        }
    }

    private static final class HttpMcpSscUrlConfig implements ISSCAndScanCentralUrlConfig {
        private final MCPServerHttpConfig.SscConfig config;

        private HttpMcpSscUrlConfig(MCPServerHttpConfig.SscConfig config) {
            this.config = config;
        }

        @Override
        public String getSscUrl() {
            return config.getUrl();
        }

        @Override
        public String getScSastControllerUrl() {
            return null;
        }

        @Override
        public Set<SSCComponentDisable> getDisabledComponents() {
            return new HashSet<>();
        }

        @Override
        public int getConnectTimeoutInMillis() {
            return config.getConnectTimeoutInMillis();
        }

        @Override
        public int getSocketTimeoutInMillis() {
            return config.getSocketTimeoutInMillis();
        }

        @Override
        public Boolean getInsecureModeEnabled() {
            return config.getInsecureModeEnabled();
        }
    }

    private static final class HttpMcpSscCredentialsConfig implements ISSCAndScanCentralCredentialsConfig {
        private final char[] sscToken;
        private final char[] scSastClientAuthToken;

        private HttpMcpSscCredentialsConfig(char[] sscToken, char[] scSastClientAuthToken) {
            this.sscToken = sscToken;
            this.scSastClientAuthToken = scSastClientAuthToken;
        }

        @Override
        public char[] getSscToken() {
            return sscToken;
        }

        @Override
        public ISSCUserCredentialsConfig getSscUserCredentialsConfig() {
            return null;
        }

        @Override
        public char[] getScSastClientAuthToken() {
            return scSastClientAuthToken;
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