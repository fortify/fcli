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
