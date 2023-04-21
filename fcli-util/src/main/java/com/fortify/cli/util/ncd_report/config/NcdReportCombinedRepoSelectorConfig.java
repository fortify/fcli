package com.fortify.cli.util.ncd_report.config;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

/**
 * This class allows for combining two {@link INcdReportRepoSelectorConfig} 
 * instances. The repository selection properties are retrieved from the
 * child if available, otherwise they are retrieved from the parent.
 * 
 * @author rsenden
 *
 */
@RequiredArgsConstructor
public class NcdReportCombinedRepoSelectorConfig implements INcdReportRepoSelectorConfig {
    private final INcdReportRepoSelectorConfig parent;
    private final INcdReportRepoSelectorConfig child;
    
    @Override
    public Optional<String> getRepositoryIncludeExpression() {
        return child.getRepositoryIncludeExpression().or(parent::getRepositoryIncludeExpression);
    }
    
    @Override
    public Optional<Boolean> getIncludeForks() {
        return child.getIncludeForks().or(parent::getIncludeForks);
    }
}
