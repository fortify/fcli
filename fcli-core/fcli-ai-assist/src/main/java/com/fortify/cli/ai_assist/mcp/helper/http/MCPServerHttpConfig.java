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

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.concurrent.job.AsyncJobManager;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.rest.unirest.config.IConnectionConfig;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;

import kong.unirest.Config;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Configuration for the HTTP MCP server, loaded from a YAML file.
 *
 * <p><b>IMPORTANT — keep sample configs in sync:</b> whenever a field is added, renamed,
 * removed, or has its default value changed, update <em>both</em> sample templates:
 * <ul>
 *   <li>{@code src/main/resources/com/fortify/cli/agent/mcp/config/mcp-http-config-ssc.yaml}</li>
 *   <li>{@code src/main/resources/com/fortify/cli/agent/mcp/config/mcp-http-config-fod.yaml}</li>
 * </ul>
 * Optional fields should appear as commented-out lines showing the default value and a brief
 * description. Required fields (e.g. {@code url}) should appear uncommented with a placeholder.
 */
@Data @NoArgsConstructor @Reflectable
@JsonIgnoreProperties(ignoreUnknown = true)
public class MCPServerHttpConfig {
    private ServerConfig server = new ServerConfig();
    private JobsConfig jobs = new JobsConfig();
    private List<String> imports = new ArrayList<>();
    private SscConfig ssc;
    private FoDConfig fod;

    @JsonIgnore private Path configPath;

    public enum Product {
        ssc,
        fod
    }

    @Data @NoArgsConstructor @Reflectable
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServerConfig {
        private int port = 8080;
        private String bindAddress;
        private long maxRequestBodyBytes = 10 * 1024 * 1024;
        private TlsConfig tls;

        @JsonIgnore
        public InetSocketAddress getInetSocketAddress() {
            if ( StringUtils.isBlank(bindAddress) ) {
                return new InetSocketAddress(port);
            }
            return new InetSocketAddress(bindAddress, port);
        }
    }

    @Data @NoArgsConstructor @Reflectable
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TlsConfig {
        private Path keystoreFile;
        private String keystorePassword;
        private String keyPassword;
        private String keystoreType = "PKCS12";

        @JsonIgnore
        public char[] getEffectiveKeyPassword() {
            var pwd = keyPassword != null ? keyPassword : keystorePassword;
            return pwd != null ? pwd.toCharArray() : new char[0];
        }
    }

    @Data @NoArgsConstructor @Reflectable
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JobsConfig {
        private static final DateTimePeriodHelper TTL_PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.SECONDS, Period.HOURS);

        private int workThreads = 20;
        private int progressThreads = 4;
        private int asyncBgThreads = AsyncJobManager.DEFAULT_BG_THREADS;
        private String safeReturn = "25s";
        private String progressInterval = "5s";
        private String isolationScopeTtl = "4h";

        @JsonIgnore
        public long getIsolationScopeTtlInMillis() {
            return StringUtils.isBlank(isolationScopeTtl)
                    ? 4 * 3600_000L
                    : TTL_PERIOD_HELPER.parsePeriodToMillis(isolationScopeTtl);
        }
    }

    @Data @NoArgsConstructor @Reflectable
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class ConnectionConfig implements IConnectionConfig {
        private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.SECONDS, Period.MINUTES);

        private Boolean insecureModeEnabled = false;
        private String socketTimeout;
        private String connectTimeout;

        @Override
        public int getConnectTimeoutInMillis() {
            return StringUtils.isBlank(connectTimeout)
                    ? Config.DEFAULT_CONNECT_TIMEOUT
                    : (int)PERIOD_HELPER.parsePeriodToMillis(connectTimeout);
        }

        @Override
        public int getSocketTimeoutInMillis() {
            return StringUtils.isBlank(socketTimeout)
                    ? getDefaultSocketTimeoutInMillis()
                    : (int)PERIOD_HELPER.parsePeriodToMillis(socketTimeout);
        }

        protected abstract int getDefaultSocketTimeoutInMillis();
    }

    @Data @NoArgsConstructor @Reflectable @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SscConfig extends ConnectionConfig {
        private String url;
        private String scSastClientAuthToken;

        @Override
        protected int getDefaultSocketTimeoutInMillis() {
            return 600000;
        }
    }

    @Data @NoArgsConstructor @Reflectable @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FoDConfig extends ConnectionConfig {
        private String url;

        @Override
        protected int getDefaultSocketTimeoutInMillis() {
            return 600000;
        }
    }

    public void validate(Path configPath) {
        this.configPath = configPath;
        validateServerConfig();
        if ( imports == null || imports.isEmpty() ) {
            throw new FcliSimpleException("HTTP MCP config must specify at least one imports entry");
        }
        imports.forEach(this::validateImportPath);
        jobs.getIsolationScopeTtlInMillis(); // validates isolationScopeTtl period string
        switch ( getProduct() ) {
        case ssc -> validateSscConfig();
        case fod -> validateFoDConfig();
        }
    }

    @JsonIgnore
    public Product getProduct() {
        var hasSsc = ssc != null;
        var hasFod = fod != null;
        if ( hasSsc == hasFod ) {
            throw new FcliSimpleException("HTTP MCP config must specify exactly one of ssc or fod section");
        }
        return hasSsc ? Product.ssc : Product.fod;
    }

    @JsonIgnore
    public List<Path> getResolvedImportPaths() {
        if ( configPath == null ) {
            throw new IllegalStateException("Config path has not been set; validate() must be called first");
        }
        return imports.stream()
                .map(this::resolveImportPath)
                .toList();
    }

    private void validateImportPath(String importPath) {
        if ( StringUtils.isBlank(importPath) ) {
            throw new FcliSimpleException("HTTP MCP config imports entries must not be blank");
        }
        var resolvedPath = resolveImportPath(importPath);
        if ( !resolvedPath.toFile().isFile() ) {
            throw new FcliSimpleException("HTTP MCP import file not found: " + resolvedPath);
        }
    }

    private Path resolveImportPath(String importPath) {
        return resolveRelativePath(Path.of(importPath));
    }

    private Path resolveRelativePath(Path path) {
        if ( path.isAbsolute() ) {
            return path.normalize();
        }
        return configPath.getParent().resolve(path).normalize();
    }

    private void validateServerConfig() {
        var tls = server.getTls();
        if ( tls == null ) { return; }
        if ( tls.getKeystoreFile() == null ) {
            throw new FcliSimpleException("HTTP MCP config server.tls.keystoreFile must be specified");
        }
        var resolvedKeystoreFile = resolveRelativePath(tls.getKeystoreFile());
        if ( !resolvedKeystoreFile.toFile().isFile() ) {
            throw new FcliSimpleException("HTTP MCP config server.tls.keystoreFile not found: " + resolvedKeystoreFile);
        }
        tls.setKeystoreFile(resolvedKeystoreFile);
        if ( StringUtils.isBlank(tls.getKeystorePassword()) ) {
            throw new FcliSimpleException("HTTP MCP config server.tls.keystorePassword must be specified");
        }
    }

    private void validateSscConfig() {
        if ( fod != null ) {
            throw new FcliSimpleException("HTTP MCP config must not specify both ssc and fod sections");
        }
        if ( StringUtils.isBlank(ssc.getUrl()) ) {
            throw new FcliSimpleException("HTTP MCP config ssc.url must be specified");
        }
        ssc.getConnectTimeoutInMillis();
        ssc.getSocketTimeoutInMillis();
    }

    private void validateFoDConfig() {
        if ( ssc != null ) {
            throw new FcliSimpleException("HTTP MCP config must not specify both ssc and fod sections");
        }
        if ( StringUtils.isBlank(fod.getUrl()) ) {
            throw new FcliSimpleException("HTTP MCP config fod.url must be specified");
        }
        fod.getConnectTimeoutInMillis();
        fod.getSocketTimeoutInMillis();
    }
}