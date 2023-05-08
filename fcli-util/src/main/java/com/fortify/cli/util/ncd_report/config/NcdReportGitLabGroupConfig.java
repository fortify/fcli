package com.fortify.cli.util.ncd_report.config;

import java.util.Optional;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This GitLab-specific configuration class holds a group id to be processed, 
 * optionally allowing sub-groups to be processed as well, together with repository 
 * selection configuration.
 * 
 * @author rsenden
 *
 */
@ReflectiveAccess @Data @EqualsAndHashCode(callSuper = true)
public class NcdReportGitLabGroupConfig extends AbstractNcdReportRepoSelectorConfig {
    private String id;
    private Optional<Boolean> includeSubgroups = Optional.empty();
}
