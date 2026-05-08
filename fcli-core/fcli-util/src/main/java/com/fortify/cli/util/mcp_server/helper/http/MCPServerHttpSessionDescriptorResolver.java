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
package com.fortify.cli.util.mcp_server.helper.http;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.rest.unirest.config.UrlConfig;
import com.fortify.cli.common.session.helper.ISessionDescriptor;
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

import io.modelcontextprotocol.common.McpTransportContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class MCPServerHttpSessionDescriptorResolver {
    public static final String HEADER_SSC_TOKEN = "X-FCLI-SSC-TOKEN";
    public static final String HEADER_SC_SAST_CLIENT_AUTH_TOKEN = "X-FCLI-SC-SAST-CLIENT-AUTH-TOKEN";
    public static final String HEADER_FOD_TENANT = "X-FCLI-FOD-TENANT";
    public static final String HEADER_FOD_USER = "X-FCLI-FOD-USER";
    public static final String HEADER_FOD_PAT = "X-FCLI-FOD-PAT";
    public static final String HEADER_FOD_CLIENT_ID = "X-FCLI-FOD-CLIENT-ID";
    public static final String HEADER_FOD_CLIENT_SECRET = "X-FCLI-FOD-CLIENT-SECRET";

    private static final int MAX_SESSION_DESCRIPTOR_CACHE_SIZE = 256;
    private static final String[] DEFAULT_FOD_SCOPES = new String[] {"api-tenant"};

    private final MCPServerHttpConfig config;
    private final Map<String, ISessionDescriptor> sessionDescriptorCache = new LinkedHashMap<>(16, 0.75f, true) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ISessionDescriptor> eldest) {
            return size() > MAX_SESSION_DESCRIPTOR_CACHE_SIZE;
        }
    };

    public ISessionDescriptor getOrCreateSessionDescriptor(McpTransportContext transportContext) {
        var cacheKey = createAuthCacheKey(transportContext);
        synchronized (sessionDescriptorCache) {
            return sessionDescriptorCache.computeIfAbsent(cacheKey, ignored -> createSessionDescriptor(transportContext));
        }
    }

    String createAuthCacheKey(McpTransportContext transportContext) {
        return switch (config.getProduct()) {
        case ssc -> createSscAuthCacheKey(transportContext);
        case fod -> createFoDAuthCacheKey(transportContext);
        };
    }

    private String createSscAuthCacheKey(McpTransportContext transportContext) {
        return createHashedCacheKey(
                "ssc",
                getRequiredHeader(transportContext, HEADER_SSC_TOKEN),
                StringUtils.defaultString(getOptionalHeader(transportContext, HEADER_SC_SAST_CLIENT_AUTH_TOKEN))
        );
    }

    private String createFoDAuthCacheKey(McpTransportContext transportContext) {
        var clientId = getOptionalHeader(transportContext, HEADER_FOD_CLIENT_ID);
        var clientSecret = getOptionalHeader(transportContext, HEADER_FOD_CLIENT_SECRET);
        var tenant = getOptionalHeader(transportContext, HEADER_FOD_TENANT);
        var user = getOptionalHeader(transportContext, HEADER_FOD_USER);
        var pat = getOptionalHeader(transportContext, HEADER_FOD_PAT);
        if ( StringUtils.isNotBlank(clientId) || StringUtils.isNotBlank(clientSecret) ) {
            validateFoDClientAuthHeaders(clientId, clientSecret, tenant, user, pat);
            return createHashedCacheKey("fod-client", clientId, clientSecret);
        }
        validateFoDUserAuthHeaders(tenant, user, pat);
        return createHashedCacheKey("fod-user", tenant, user, pat);
    }

    private void validateFoDClientAuthHeaders(String clientId, String clientSecret, String tenant, String user, String pat) {
        if ( StringUtils.isAnyBlank(clientId, clientSecret) ) {
            throw new FcliSimpleException("FoD client authentication requires both %s and %s",
                    HEADER_FOD_CLIENT_ID, HEADER_FOD_CLIENT_SECRET);
        }
        if ( StringUtils.isNotBlank(tenant) || StringUtils.isNotBlank(user) || StringUtils.isNotBlank(pat) ) {
            throw new FcliSimpleException("Specify either FoD client headers (%s, %s) or FoD user headers (%s, %s, %s)",
                    HEADER_FOD_CLIENT_ID, HEADER_FOD_CLIENT_SECRET,
                    HEADER_FOD_TENANT, HEADER_FOD_USER, HEADER_FOD_PAT);
        }
    }

    private void validateFoDUserAuthHeaders(String tenant, String user, String pat) {
        if ( StringUtils.isAnyBlank(tenant, user, pat) ) {
            throw new FcliSimpleException("FoD user authentication requires headers %s, %s, and %s",
                    HEADER_FOD_TENANT, HEADER_FOD_USER, HEADER_FOD_PAT);
        }
    }

    private String createHashedCacheKey(String prefix, String... values) {
        var digest = getDigest();
        for ( var value : values ) {
            if ( value != null ) {
                digest.update(value.getBytes(StandardCharsets.UTF_8));
            }
            digest.update((byte)0);
        }
        return prefix + "|" + HexFormat.of().formatHex(digest.digest());
    }

    private MessageDigest getDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch ( NoSuchAlgorithmException e ) {
            throw new IllegalStateException("SHA-256 digest algorithm is not available", e);
        }
    }

    private ISessionDescriptor createSessionDescriptor(McpTransportContext transportContext) {
        return switch (config.getProduct()) {
        case ssc -> createSscSessionDescriptor(transportContext);
        case fod -> createFoDSessionDescriptor(transportContext);
        };
    }

    private ISessionDescriptor createSscSessionDescriptor(McpTransportContext transportContext) {
        var tokenData = new SSCTokenData();
        tokenData.setToken(getRequiredHeader(transportContext, HEADER_SSC_TOKEN).toCharArray());
        var sscConfig = config.getSsc();
        var scSastClientAuthToken = StringUtils.firstNonBlank(
                sscConfig.getScSastClientAuthToken(),
                getOptionalHeader(transportContext, HEADER_SC_SAST_CLIENT_AUTH_TOKEN)
        );
        return SSCAndScanCentralSessionDescriptor.create(
                new HttpMcpSscUrlConfig(sscConfig),
                new HttpMcpSscCredentialsConfig(
                        tokenData.getToken(),
                        StringUtils.isBlank(scSastClientAuthToken) ? null : scSastClientAuthToken.toCharArray()
                )
        );
    }

    private ISessionDescriptor createFoDSessionDescriptor(McpTransportContext transportContext) {
        var fodConfig = config.getFod();
        var urlConfig = UrlConfig.builderFromConnectionConfig(fodConfig)
                .url(FoDProductHelper.INSTANCE.getApiUrl(fodConfig.getUrl()))
                .build();
        return new FoDSessionDescriptor(urlConfig, createFoDTokenResponse(transportContext, urlConfig));
    }

    private FoDTokenCreateResponse createFoDTokenResponse(McpTransportContext transportContext, UrlConfig urlConfig) {
        var clientId = getOptionalHeader(transportContext, HEADER_FOD_CLIENT_ID);
        var clientSecret = getOptionalHeader(transportContext, HEADER_FOD_CLIENT_SECRET);
        if ( StringUtils.isNotBlank(clientId) || StringUtils.isNotBlank(clientSecret) ) {
            return FoDOAuthHelper.createToken(
                    urlConfig,
                    new HttpMcpFoDClientCredentials(
                            getRequiredHeader(transportContext, HEADER_FOD_CLIENT_ID),
                            getRequiredHeader(transportContext, HEADER_FOD_CLIENT_SECRET)
                    ),
                    DEFAULT_FOD_SCOPES
            );
        }
        return FoDOAuthHelper.createToken(
                urlConfig,
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
        var headers = (Map<String, List<String>>)transportContext.get("headers");
        if ( headers == null || headers.isEmpty() ) {
            return null;
        }
        return headers.entrySet().stream()
                .filter(entry -> headerName.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(values -> values != null && !values.isEmpty())
                .map(values -> values.get(0))
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
}