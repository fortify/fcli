/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.license.ncd_report.config;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.report.generator.IReportResultsGenerator;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.license.ncd_report.collector.NcdReportResultsCollector;
import com.fortify.cli.license.ncd_report.generator.github.NcdReportGitHubResultsGenerator;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This GitHub-specific configuration class defines a GitHub source configuration,
 * holding GitHub URL, credentials and list of organizations to be processed, and 
 * providing a {@link NcdReportGitHubResultsGenerator} instance based on this 
 * configuration.
 * 
 * @author rsenden
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public class NcdReportGitHubSourceConfig extends AbstractNcdReportRepoSelectorConfig implements INcdReportSourceConfig, IUrlConfig {
    private String apiUrl = "https://api.github.com/";
    private String tokenExpression;
    private Boolean insecureModeEnabled;
    
    @Override
    public String getUrl() {
        return apiUrl;
    }
    
    public boolean hasUrlConfig() {
        return apiUrl!=null;
    }
    private NcdReportGitHubOrganizationConfig[] organizations;
    
    @Override
    public IReportResultsGenerator generator(NcdReportResultsCollector resultsCollector) {
        return new NcdReportGitHubResultsGenerator(this, resultsCollector);
    }
}
