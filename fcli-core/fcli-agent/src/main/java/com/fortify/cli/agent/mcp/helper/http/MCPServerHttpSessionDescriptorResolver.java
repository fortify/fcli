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
package com.fortify.cli.agent.mcp.helper.http;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.cli.util.FcliActionState;
import com.fortify.cli.common.cli.util.FcliExecutionContext;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.cli.util.FcliIsolationScope;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.rest.unirest.config.UrlConfig;
import com.fortify.cli.common.session.helper.ISessionDescriptor;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.FileUtils;
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
import com.fortify.cli.ssc._common.session.helper.SSCSessionValidationHelper;
import com.fortify.cli.ssc.access_control.helper.SSCTokenGetOrCreateResponse.SSCTokenData;

import io.modelcontextprotocol.common.McpTransportContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public final class MCPServerHttpSessionDescriptorResolver {
    public static final String HEADER_AUTH_SSC = "X-AUTH-SSC";
    public static final String HEADER_AUTH_FOD = "X-AUTH-FOD";

    private static final String SSC_TOKEN_KEY = "token";
    private static final String SSC_SC_SAST_CLIENT_AUTH_TOKEN_KEY = "sc-sast-token";
    private static final String FOD_TENANT_KEY = "tenant";
    private static final String FOD_USER_KEY = "user";
    private static final String FOD_PAT_KEY = "pat";
    private static final String FOD_CLIENT_ID_KEY = "client-id";
    private static final String FOD_CLIENT_SECRET_KEY = "client-secret";

    private static final String[] DEFAULT_FOD_SCOPES = new String[] {"api-tenant"};

    private final MCPServerHttpConfig config;
    private final Path mcpScopedVarsRootPath = FcliDataHelper.getFcliStatePath().resolve("mcp-http-vars");
    private final ConcurrentHashMap<String, IsolationScopeEntry> isolationScopeCache = new ConcurrentHashMap<>();

    private static final class IsolationScopeEntry {
        final FcliIsolationScope scope;
        volatile long lastAccessTime;

        IsolationScopeEntry(FcliIsolationScope scope) {
            this.scope = scope;
            this.lastAccessTime = System.currentTimeMillis();
        }

        void updateLastAccess() {
            lastAccessTime = System.currentTimeMillis();
        }

        boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - lastAccessTime > ttlMillis;
        }
    }

    private static final class FunctionContextState {
        private final FcliActionState actionState = new FcliActionState();
    }

    /**
     * Pushes a new {@link FcliExecutionContext} (fresh {@code UnirestContext}) for the given
     * auth scope key and returns the associated {@link FcliExecutionContextHolder.ContextFrame}.
     * The per-auth-scope {@link FcliActionState} is reused across calls so that
     * {@code global.*} action variables persist within the same authenticated identity.
     */
    public FcliExecutionContextHolder.ContextFrame getOrCreateFunctionFrame(String authScopeKey) {
        var isolationScope = getExistingIsolationScope(authScopeKey);
        var actionState = isolationScope.getOrCreateScopedState(FunctionContextState.class,
                FunctionContextState::new).actionState;
        return FcliExecutionContextHolder.push(new FcliExecutionContext(isolationScope, actionState));
    }

    /**
     * Gets or creates the {@link FcliIsolationScope} for the request's authenticated identity,
     * then validates and refreshes the session token before returning.
     * For FoD, a new OAuth token is obtained if the cached token has expired.
     * For SSC, the token is actively validated against SSC on every request; a
     * {@link com.fortify.cli.common.exception.FcliSimpleException} is thrown if the token
     * is invalid or has been revoked.
     */
    public FcliIsolationScope getOrCreateIsolationScope(McpTransportContext transportContext) {
        var authScopeKey = createAuthCacheKey(transportContext);
        var entry = isolationScopeCache.get(authScopeKey);
        if ( entry == null ) {
            var newEntry = new IsolationScopeEntry(createIsolationScope(authScopeKey, transportContext));
            var existing = isolationScopeCache.putIfAbsent(authScopeKey, newEntry);
            entry = existing != null ? existing : newEntry;
        }
        entry.updateLastAccess();
        validateAndRefreshSession(entry, transportContext);
        return entry.scope;
    }

    public String getAuthScopeKey(McpTransportContext transportContext) {
        return createAuthCacheKey(transportContext);
    }

    /**
     * Schedules periodic eviction of isolation scopes that have not been accessed within
     * {@code ttlMillis}. The cleanup interval is {@code max(1 minute, ttlMillis / 4)}.
     */
    public void scheduleCleanup(long ttlMillis, ScheduledExecutorService scheduler) {
        var periodMillis = Math.max(60_000L, ttlMillis / 4);
        scheduler.scheduleWithFixedDelay(
                () -> evictExpiredScopes(ttlMillis),
                periodMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Shuts down the resolver: deletes all per-scope variable directories and the shared
     * root directory. Should be called from the server shutdown hook.
     */
    public void shutdown() {
        isolationScopeCache.values().forEach(e -> deleteDirQuietly(e.scope.getScopedVarsPath()));
        isolationScopeCache.clear();
        deleteDirQuietly(mcpScopedVarsRootPath);
    }

    private void evictExpiredScopes(long ttlMillis) {
        isolationScopeCache.entrySet().removeIf(e -> {
            if ( e.getValue().isExpired(ttlMillis) ) {
                deleteDirQuietly(e.getValue().scope.getScopedVarsPath());
                return true;
            }
            return false;
        });
    }

    private FcliIsolationScope getExistingIsolationScope(String authScopeKey) {
        var entry = isolationScopeCache.get(authScopeKey);
        if ( entry == null ) {
            throw new IllegalStateException("No isolation scope found for auth scope key");
        }
        return entry.scope;
    }

    private FcliIsolationScope createIsolationScope(String authScopeKey, McpTransportContext transportContext) {
        var result = new FcliIsolationScope();
        result.setMcpRequestAuthScopeKey(authScopeKey);
        result.setTransientSessionDescriptor(createSessionDescriptor(transportContext));
        result.setScopedVarsPath(mcpScopedVarsRootPath.resolve(authScopeKey.replace("|", "_")));
        return result;
    }

    /**
     * Validates and if necessary refreshes the session token for the given entry.
     * Synchronized on the entry to prevent concurrent refreshes for the same user.
     */
    private void validateAndRefreshSession(IsolationScopeEntry entry, McpTransportContext transportContext) {
        synchronized (entry) {
            var descriptors = entry.scope.getTransientSessionDescriptors();
            if ( descriptors.isEmpty() ) {
                return;
            }
            var descriptor = descriptors.values().iterator().next();
            if ( descriptor instanceof FoDSessionDescriptor fodDescriptor ) {
                refreshFoDTokenIfExpired(fodDescriptor, transportContext);
            } else if ( descriptor instanceof SSCAndScanCentralSessionDescriptor sscDescriptor ) {
                validateSscToken(sscDescriptor);
            }
        }
    }

    private void refreshFoDTokenIfExpired(FoDSessionDescriptor descriptor, McpTransportContext transportContext) {
        if ( descriptor.hasActiveCachedTokenResponse() ) {
            return;
        }
        var auth = parseAuthHeader(transportContext);
        var fodConfig = config.getFod();
        var urlConfig = UrlConfig.builderFromConnectionConfig(fodConfig)
                .url(FoDProductHelper.INSTANCE.getApiUrl(fodConfig.getUrl()))
                .build();
        descriptor.setCachedTokenResponse(createFoDTokenResponse(auth, urlConfig));
    }

    private void validateSscToken(SSCAndScanCentralSessionDescriptor descriptor) {
        var token = descriptor.getActiveSSCToken();
        if ( token == null ) {
            throw new FcliSimpleException("SSC session token has expired; please provide a valid token in the %s header", HEADER_AUTH_SSC);
        }
        var status = SSCSessionValidationHelper.checkTokenStatus(descriptor.getSscUrlConfig(), token);
        if ( !status.valid() ) {
            throw new FcliSimpleException("SSC session token is invalid or has been revoked; please provide a valid token in the %s header", HEADER_AUTH_SSC);
        }
        descriptor.setSscTokenData(status.tokenData());
    }

    private void deleteDirQuietly(Path absolutePath) {
        if ( absolutePath == null || !Files.exists(absolutePath) ) {
            return;
        }
        try {
            FileUtils.deleteRecursive(absolutePath);
        } catch ( Exception e ) {
            log.warn("Failed to delete scoped vars directory {}: {}", absolutePath, e.getMessage());
        }
    }

    String createAuthCacheKey(McpTransportContext transportContext) {
        var auth = parseAuthHeader(transportContext);
        return switch (auth.product()) {
        case ssc -> createSscAuthCacheKey(auth);
        case fod -> createFoDAuthCacheKey(auth);
        };
    }

    private String createSscAuthCacheKey(ParsedAuthorization auth) {
        return createHashedCacheKey(
                "ssc",
                auth.sscToken(),
                StringUtils.defaultString(auth.scSastClientAuthToken())
        );
    }

    private String createFoDAuthCacheKey(ParsedAuthorization auth) {
        var clientId = auth.fodClientId();
        var clientSecret = auth.fodClientSecret();
        var tenant = auth.fodTenant();
        var user = auth.fodUser();
        var pat = auth.fodPat();
        if ( StringUtils.isNotBlank(clientId) || StringUtils.isNotBlank(clientSecret) ) {
            validateFoDClientAuthHeaders(clientId, clientSecret, tenant, user, pat);
            return createHashedCacheKey("fod-client", clientId, clientSecret);
        }
        validateFoDUserAuthHeaders(tenant, user, pat);
        return createHashedCacheKey("fod-user", tenant, user, pat);
    }

    private void validateFoDClientAuthHeaders(String clientId, String clientSecret, String tenant, String user, String pat) {
        if ( StringUtils.isAnyBlank(clientId, clientSecret) ) {
            throw new FcliSimpleException("FoD client authentication requires keys %s and %s in %s header",
                FOD_CLIENT_ID_KEY, FOD_CLIENT_SECRET_KEY, HEADER_AUTH_FOD);
        }
        if ( StringUtils.isNotBlank(tenant) || StringUtils.isNotBlank(user) || StringUtils.isNotBlank(pat) ) {
            throw new FcliSimpleException("Specify either FoD client keys (%s, %s) or FoD user keys (%s, %s, %s) in %s header",
                FOD_CLIENT_ID_KEY, FOD_CLIENT_SECRET_KEY,
                FOD_TENANT_KEY, FOD_USER_KEY, FOD_PAT_KEY, HEADER_AUTH_FOD);
        }
    }

    private void validateFoDUserAuthHeaders(String tenant, String user, String pat) {
        if ( StringUtils.isAnyBlank(tenant, user, pat) ) {
            throw new FcliSimpleException("FoD user authentication requires keys %s, %s, and %s in %s header",
                FOD_TENANT_KEY, FOD_USER_KEY, FOD_PAT_KEY, HEADER_AUTH_FOD);
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
        var auth = parseAuthHeader(transportContext);
        return switch (auth.product()) {
        case ssc -> createSscSessionDescriptor(auth);
        case fod -> createFoDSessionDescriptor(auth);
        };
    }

    private ISessionDescriptor createSscSessionDescriptor(ParsedAuthorization auth) {
        var tokenData = new SSCTokenData();
        tokenData.setToken(auth.sscToken().toCharArray());
        var sscConfig = config.getSsc();
        var scSastClientAuthToken = StringUtils.firstNonBlank(
                sscConfig.getScSastClientAuthToken(),
                auth.scSastClientAuthToken()
        );
        return SSCAndScanCentralSessionDescriptor.create(
                new HttpMcpSscUrlConfig(sscConfig),
                new HttpMcpSscCredentialsConfig(
                        tokenData.getToken(),
                        StringUtils.isBlank(scSastClientAuthToken) ? null : scSastClientAuthToken.toCharArray()
                )
        );
    }

    private ISessionDescriptor createFoDSessionDescriptor(ParsedAuthorization auth) {
        var fodConfig = config.getFod();
        var urlConfig = UrlConfig.builderFromConnectionConfig(fodConfig)
                .url(FoDProductHelper.INSTANCE.getApiUrl(fodConfig.getUrl()))
                .build();
        return new FoDSessionDescriptor(urlConfig, createFoDTokenResponse(auth, urlConfig));
    }

    private FoDTokenCreateResponse createFoDTokenResponse(ParsedAuthorization auth, UrlConfig urlConfig) {
        var clientId = auth.fodClientId();
        var clientSecret = auth.fodClientSecret();
        if ( StringUtils.isNotBlank(clientId) || StringUtils.isNotBlank(clientSecret) ) {
            return FoDOAuthHelper.createToken(
                    urlConfig,
                    new HttpMcpFoDClientCredentials(
                            auth.fodClientId(),
                            auth.fodClientSecret()
                    ),
                    DEFAULT_FOD_SCOPES
            );
        }
        return FoDOAuthHelper.createToken(
                urlConfig,
                new HttpMcpFoDUserCredentials(
                        auth.fodTenant(),
                        auth.fodUser(),
                        auth.fodPat().toCharArray()
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

    private ParsedAuthorization parseAuthHeader(McpTransportContext transportContext) {
        var product = config.getProduct();
        var headerName = getAuthHeaderName(product);
        var headerValue = getRequiredHeader(transportContext, headerName);
        var keyValues = parseAuthHeaderKeyValues(headerValue, headerName);
        return switch (product) {
        case ssc -> parseSscAuthorization(keyValues);
        case fod -> parseFoDAuthorization(keyValues);
        };
    }

    private String getAuthHeaderName(MCPServerHttpConfig.Product product) {
        return switch (product) {
        case ssc -> HEADER_AUTH_SSC;
        case fod -> HEADER_AUTH_FOD;
        };
    }

    private Map<String, String> parseAuthHeaderKeyValues(String valuePart, String headerName) {
        var result = new LinkedHashMap<String, String>();
        for ( var segment : splitEscapedSegments(valuePart, headerName) ) {
            var trimmedSegment = StringUtils.trimToNull(segment);
            if ( trimmedSegment == null ) {
                continue;
            }
            var separatorIndex = findUnescapedSeparator(trimmedSegment, '=');
            if ( separatorIndex <= 0 || separatorIndex == trimmedSegment.length() - 1 ) {
                throw new FcliSimpleException("Invalid %s header segment '%s'; expected key=value", headerName, trimmedSegment);
            }
            var key = StringUtils.trimToNull(unescapeHeaderValue(trimmedSegment.substring(0, separatorIndex), headerName));
            var value = StringUtils.trimToNull(unescapeHeaderValue(trimmedSegment.substring(separatorIndex + 1), headerName));
            if ( key == null || value == null ) {
                throw new FcliSimpleException("Invalid %s header segment '%s'; expected key=value", headerName, trimmedSegment);
            }
            var normalizedKey = key.toLowerCase(Locale.ROOT);
            if ( result.containsKey(normalizedKey) ) {
                throw new FcliSimpleException("Duplicate %s header key: %s", headerName, key);
            }
            result.put(normalizedKey, value);
        }
        if ( result.isEmpty() ) {
            throw new FcliSimpleException("%s header doesn't contain any key/value entries", headerName);
        }
        return result;
    }

    private List<String> splitEscapedSegments(String valuePart, String headerName) {
        var result = new ArrayList<String>();
        var current = new StringBuilder();
        var escaping = false;
        for ( var i = 0; i < valuePart.length(); i++ ) {
            var c = valuePart.charAt(i);
            if ( escaping ) {
                validateEscapeCharacter(c, headerName);
                current.append('\\').append(c);
                escaping = false;
            } else if ( c == '\\' ) {
                escaping = true;
            } else if ( c == ';' ) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if ( escaping ) {
            throw new FcliSimpleException("Invalid %s header value; trailing escape character", headerName);
        }
        result.add(current.toString());
        return result;
    }

    private int findUnescapedSeparator(String value, char separator) {
        var escaping = false;
        for ( var i = 0; i < value.length(); i++ ) {
            var c = value.charAt(i);
            if ( escaping ) {
                escaping = false;
            } else if ( c == '\\' ) {
                escaping = true;
            } else if ( c == separator ) {
                return i;
            }
        }
        return -1;
    }

    private String unescapeHeaderValue(String value, String headerName) {
        var result = new StringBuilder();
        var escaping = false;
        for ( var i = 0; i < value.length(); i++ ) {
            var c = value.charAt(i);
            if ( escaping ) {
                validateEscapeCharacter(c, headerName);
                result.append(c);
                escaping = false;
            } else if ( c == '\\' ) {
                escaping = true;
            } else {
                result.append(c);
            }
        }
        if ( escaping ) {
            throw new FcliSimpleException("Invalid %s header value; trailing escape character", headerName);
        }
        return result.toString();
    }

    private void validateEscapeCharacter(char c, String headerName) {
        if ( c != '\\' && c != ';' && c != '=' ) {
            throw new FcliSimpleException("Invalid %s header escape sequence '\\%s'; supported escapes are \\\\, \\; and \\=", headerName, c);
        }
    }

    private ParsedAuthorization parseSscAuthorization(Map<String, String> keyValues) {
        var token = keyValues.get(SSC_TOKEN_KEY);
        if ( StringUtils.isBlank(token) ) {
            throw new FcliSimpleException("%s header requires key '%s'", HEADER_AUTH_SSC, SSC_TOKEN_KEY);
        }
        return new ParsedAuthorization(
                MCPServerHttpConfig.Product.ssc,
                token,
                keyValues.get(SSC_SC_SAST_CLIENT_AUTH_TOKEN_KEY),
                null,
                null,
                null,
                null,
                null
        );
    }

    private ParsedAuthorization parseFoDAuthorization(Map<String, String> keyValues) {
        var clientId = keyValues.get(FOD_CLIENT_ID_KEY);
        var clientSecret = keyValues.get(FOD_CLIENT_SECRET_KEY);
        var tenant = keyValues.get(FOD_TENANT_KEY);
        var user = keyValues.get(FOD_USER_KEY);
        var pat = keyValues.get(FOD_PAT_KEY);
        return new ParsedAuthorization(
                MCPServerHttpConfig.Product.fod,
                null,
                null,
                clientId,
                clientSecret,
                tenant,
                user,
                pat
        );
    }

    private record ParsedAuthorization(
            MCPServerHttpConfig.Product product,
            String sscToken,
            String scSastClientAuthToken,
            String fodClientId,
            String fodClientSecret,
            String fodTenant,
            String fodUser,
            String fodPat
    ) {}

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