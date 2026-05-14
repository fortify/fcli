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

@Data @NoArgsConstructor @Reflectable
@JsonIgnoreProperties(ignoreUnknown = true)
public class MCPServerHttpConfig {
    private static final DateTimePeriodHelper TTL_PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.SECONDS, Period.HOURS);

    private int port = 8080;
    private int workThreads = 10;
    private int progressThreads = 4;
    private int asyncBgThreads = AsyncJobManager.DEFAULT_BG_THREADS;
    private String jobSafeReturn = "25s";
    private String progressInterval = "5s";
    private String isolationScopeTtl = "4h";
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
        if ( imports == null || imports.isEmpty() ) {
            throw new FcliSimpleException("HTTP MCP config must specify at least one imports entry");
        }
        imports.forEach(this::validateImportPath);
        getIsolationScopeTtlInMillis(); // validates isolationScopeTtl period string
        switch ( getProduct() ) {
        case ssc -> validateSscConfig();
        case fod -> validateFoDConfig();
        }
    }

    @JsonIgnore
    public long getIsolationScopeTtlInMillis() {
        return StringUtils.isBlank(isolationScopeTtl)
                ? 4 * 3600_000L
                : TTL_PERIOD_HELPER.parsePeriodToMillis(isolationScopeTtl);
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
        var path = Path.of(importPath);
        if ( path.isAbsolute() ) {
            return path.normalize();
        }
        return configPath.getParent().resolve(path).normalize();
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