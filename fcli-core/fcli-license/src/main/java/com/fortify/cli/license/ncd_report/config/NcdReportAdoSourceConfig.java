/*
 * Copyright 2021-2025 Open Text.
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
// *******************************************************************************
// Copyright 2021, 2023 Open Text.
//
// The only warranties for products and services of Open Text 
// and its affiliates and licensors ("Open Text") are as may 
// be set forth in the express warranty statements accompanying 
// such products and services. Nothing herein should be construed 
// as constituting an additional warranty. Open Text shall not be 
// liable for technical or editorial errors or omissions contained 
// herein. The information contained herein is subject to change 
// without notice.
// *******************************************************************************/
package com.fortify.cli.license.ncd_report.config;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.report.generator.IReportResultsGenerator;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.license.ncd_report.collector.NcdReportResultsCollector;
import com.fortify.cli.license.ncd_report.generator.ado.NcdReportAdoResultsGenerator;

import kong.unirest.Config;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public class NcdReportAdoSourceConfig extends AbstractNcdReportRepoSelectorConfig implements INcdReportSourceConfig, IUrlConfig {
    private String baseUrl = "https://dev.azure.com"; // No trailing slash
    private String tokenExpression; // Personal Access Token expression
    private int connectTimeoutInMillis = Config.DEFAULT_CONNECT_TIMEOUT;
    private int socketTimeoutInMillis = Config.DEFAULT_SOCKET_TIMEOUT;
    private Boolean insecureModeEnabled;
    private String apiVersion = "7.1";
    private NcdReportAdoOrganizationConfig[] organizations;

    @Override
    public String getUrl() {
        return baseUrl;
    }

    public boolean hasUrlConfig() {
        return baseUrl != null;
    }

    @Override
    public IReportResultsGenerator generator(NcdReportResultsCollector resultsCollector) {
        return new NcdReportAdoResultsGenerator(this, resultsCollector);
    }
}