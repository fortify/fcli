package com.fortify.cli.util.ncd_report.config;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This GitHub-specific configuration class holds an organization name
 * to be processed, together with repository selection configuration.
 * 
 * @author rsenden
 *
 */
@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = true)
public class NcdReportGitHubOrganizationConfig extends AbstractNcdReportRepoSelectorConfig {
    private String name;
}
