package com.fortify.cli.util.ncd_report.config;

import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.ncd_report.collector.NcdReportResultsCollector;
import com.fortify.cli.util.ncd_report.generator.github.NcdReportGitHubResultsGenerator;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This GitHub-specific configuration class defines a GitHub source configuration,
 * holding GitHub URL, credentials and list of organizations to be processed, and 
 * providing a {@link NcdReportGitHubResultsGenerator} instance based on this 
 * configuration.
 * 
 * @author rsenden
 */
@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = true)
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
    public Runnable generator(NcdReportResultsCollector resultsCollector) {
        return new NcdReportGitHubResultsGenerator(this, resultsCollector);
    }
}
