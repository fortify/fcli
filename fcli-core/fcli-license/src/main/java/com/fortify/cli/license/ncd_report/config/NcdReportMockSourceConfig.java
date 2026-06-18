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
package com.fortify.cli.license.ncd_report.config;

import java.util.Optional;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.report.generator.IReportResultsGenerator;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.license.ncd_report.collector.NcdReportContext;
import com.fortify.cli.license.ncd_report.generator.mock.NcdReportMockResultsGenerator;

import kong.unirest.Config;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Mock SCM source configuration for testing NCD report generation.
 * Supports both auto-generated mock commits and reading from external files.
 */
@Reflectable @NoArgsConstructor @AllArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public class NcdReportMockSourceConfig extends AbstractNcdReportRepoSelectorConfig implements INcdReportSourceConfig, IUrlConfig {
    /** Number of repositories to generate (default 3) */
    private Integer repositoryCount = 3;
    
    /** Number of authors per repository (default 5) */
    private Integer authorsPerRepository = 5;
    
    /** Number of commits per author (default 10) */
    private Integer commitsPerAuthor = 10;
    
    /** Optional path to JSON/YAML/CSV file with custom mock commit data */
    private Optional<String> dataFile = Optional.empty();
    
    /** Custom HTTP headers */
    private java.util.List<String> headers = new java.util.ArrayList<>();
    
    /** Connection timeout in milliseconds */
    private int connectTimeoutInMillis = Config.DEFAULT_CONNECT_TIMEOUT;
    
    /** Socket timeout in milliseconds */
    private int socketTimeoutInMillis = Config.DEFAULT_SOCKET_TIMEOUT;
    
    /** Insecure mode enabled flag */
    private Boolean insecureModeEnabled = false;
    
    @Override
    public String getUrl() {
        return "mock://";
    }
    
    @Override
    public Boolean getInsecureModeEnabled() {
        return insecureModeEnabled;
    }
    
    @Override
    public IReportResultsGenerator generator(NcdReportContext reportContext) {
        return new NcdReportMockResultsGenerator(this, reportContext);
    }
}
