/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
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
