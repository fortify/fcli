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
package com.fortify.cli.ai_assist.mcp.helper.http;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.cli.util.FcliActionState;
import com.fortify.cli.common.cli.util.FcliExecutionContext;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.cli.util.FcliIsolationScope;
import com.fortify.cli.common.exception.FcliBugException;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public final class MCPServerHttpSessionDescriptorResolver {
    public static final String HEADER_AUTH_SSC = "X-AUTH-SSC";
    public static final String HEADER_AUTH_FOD = "X-AUTH-FOD";

    // Package-visible so MCPServerHttpAuthHeaderParser can reference them as constants
    static final String SSC_TOKEN_KEY = "token";
    static final String SSC_SC_SAST_CLIENT_AUTH_TOKEN_KEY = "sc-sast-token";
    static final String FOD_TENANT_KEY = "tenant";
    static final String FOD_USER_KEY = "user";
    static final String FOD_PAT_KEY = "pat";
    static final String FOD_CLIENT_ID_KEY = "client-id";
    static final String FOD_CLIENT_SECRET_KEY = "client-secret";

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
    public FcliIsolationScope getOrCreateIsolationScope(ParsedAuthorization auth) {
        var authScopeKey = createAuthCacheKey(auth);
        var entry = isolationScopeCache.get(authScopeKey);
        if ( entry == null ) {
            var newEntry = new IsolationScopeEntry(createIsolationScope(authScopeKey, auth));
            var existing = isolationScopeCache.putIfAbsent(authScopeKey, newEntry);
            entry = existing != null ? existing : newEntry;
        }
        entry.updateLastAccess();
        validateAndRefreshSession(entry, auth);
        return entry.scope;
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
            throw new FcliBugException("No isolation scope found for auth scope key");
        }
        return entry.scope;
    }

    private FcliIsolationScope createIsolationScope(String authScopeKey, ParsedAuthorization auth) {
        var result = new FcliIsolationScope();
        result.setMcpRequestAuthScopeKey(authScopeKey);
        result.setTransientSessionDescriptor(createSessionDescriptor(auth));
        result.setScopedVarsPath(mcpScopedVarsRootPath.resolve(authScopeKey.replace("|", "_")));
        return result;
    }

    /**
     * Validates and if necessary refreshes the session token for the given entry.
     * Synchronized on the entry to prevent concurrent refreshes for the same user.
     * A new descriptor instance is published into the scope rather than mutating the existing one,
     * keeping the descriptor classes free of concurrency concerns.
     */
    private void validateAndRefreshSession(IsolationScopeEntry entry, ParsedAuthorization auth) {
        synchronized (entry) {
            for ( var descriptor : entry.scope.getTransientSessionDescriptors().values() ) {
                if ( descriptor instanceof FoDSessionDescriptor fodDescriptor ) {
                    // OAuth tokens expire; replace the descriptor with one carrying a fresh token if needed
                    entry.scope.setTransientSessionDescriptor(refreshFoDTokenIfExpired(fodDescriptor, auth));
                } else if ( descriptor instanceof SSCAndScanCentralSessionDescriptor sscDescriptor ) {
                    // Client always provides its own token; just validate it — no descriptor replacement needed
                    // If we every add support for user/pwd-based SSC auth, we may need to add token refresh
                    // logic here as well
                    validateSscToken(sscDescriptor);
                }
            }
        }
    }

    private FoDSessionDescriptor refreshFoDTokenIfExpired(FoDSessionDescriptor descriptor, ParsedAuthorization auth) {
        if ( descriptor.hasActiveCachedTokenResponse() ) {
            return descriptor;
        }
        var fodConfig = config.getFod();
        var urlConfig = UrlConfig.builderFromConnectionConfig(fodConfig)
                .url(FoDProductHelper.INSTANCE.getApiUrl(fodConfig.getUrl()))
                .build();
        return descriptor.withCachedTokenResponse(createFoDTokenResponse(auth, urlConfig));
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

    String createAuthCacheKey(ParsedAuthorization auth) {
        return switch ( auth.product() ) {
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
            throw new FcliBugException("SHA-256 digest algorithm is not available", e);
        }
    }

    private ISessionDescriptor createSessionDescriptor(ParsedAuthorization auth) {
        return switch ( auth.product() ) {
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
        var pwd = auth.fodPat().toCharArray();
        try {
            return FoDOAuthHelper.createToken(
                    urlConfig,
                    new HttpMcpFoDUserCredentials(
                            auth.fodTenant(),
                            auth.fodUser(),
                            pwd
                    ),
                    DEFAULT_FOD_SCOPES
            );
        } finally {
            Arrays.fill(pwd, '\0');
        }
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
            return Set.of();
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

        @Override
        public java.util.List<String> getHeaders() {
            return config.getHeaders();
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
