package com.fortify.cli.util.ncd_report.config;

import java.util.Optional;

/** 
 * Interface to be implemented by configuration classes
 * describing what repositories to include in the report.
 * 
 * @author rsenden
 */
public interface INcdReportRepoSelectorConfig {
    /**
     * Get optional SpEL include expression; if configured,
     * only repositories matching the given expression 
     * (evaluated on the repository JSON object) will be 
     * included in the report. 
     * @return
     */
    Optional<String> getRepositoryIncludeExpression();
    /**
     * Get optional boolean describing whether to include
     * forks in the report. This can be used to override 
     * the global {@link NcdReportSourcesConfig#getIncludeForks()}
     * setting.
     * @return
     */
    Optional<Boolean> getIncludeForks();
}
