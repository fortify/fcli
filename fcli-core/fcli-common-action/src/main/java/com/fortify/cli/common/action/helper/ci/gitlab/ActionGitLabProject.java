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
package com.fortify.cli.common.action.helper.ci.gitlab;

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.ci;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.ci.gitlab.GitLabEnvironment;
import com.fortify.cli.common.ci.gitlab.GitLabProject;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctions;

import lombok.RequiredArgsConstructor;

/**
 * Action wrapper for GitLabProject providing environment-aware GitLab project operations.
 * Used by ActionGitLabSpelFunctions to provide SpEL functions that automatically 
 * use GitLab CI environment defaults.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
@SpelFunctions
public class ActionGitLabProject {
    private final GitLabProject project;
    private final GitLabEnvironment env;
    
    @SpelFunction(cat=ci, desc="Uploads security report to GitLab (paid tier, requires Ultimate/Premium) using project/pipeline detected from the current run",
            returns="Response from GitLab API")
    public ObjectNode uploadSecurityReport(
            @SpelFunctionParam(name="reportType", desc="report type: sast, dast, dependency_scanning, container_scanning, etc.") String reportType,
            @SpelFunctionParam(name="reportContent", desc="security report in GitLab schema format") String reportContent) {
        return project.uploadSecurityReport(requirePipelineId("uploadSecurityReport"), reportType, reportContent);
    }
    
    @SpelFunction(cat=ci, desc="Uploads code quality report to the detected merge request (free tier, all GitLab tiers)",
            returns="Response from GitLab API")
    public ObjectNode uploadCodeQualityReport(
            @SpelFunctionParam(name="reportContent", desc="code quality report as JSON array") String reportContent) {
        if (!env.pullRequest().active()) {
            throw new FcliSimpleException("Not running in merge request context. CI_MERGE_REQUEST_IID is not set.");
        }
        return project.uploadCodeQualityReport(env.pullRequest().id(), reportContent);
    }
    
    @SpelFunction(cat=ci, desc="(PREVIEW) Adds a comment to the merge request detected from the current pipeline run. This function is not yet used by any built-in fcli actions; signature and implementation may change in future fcli versions based on new insights as to how to best integrate this functionality into fcli built-in actions.",
            returns="Created note object")
    public ObjectNode addMrComment(
            @SpelFunctionParam(name="body", desc="comment body (Markdown supported)") String body) {
        if (!env.pullRequest().active()) {
            throw new FcliSimpleException("Not running in merge request context. CI_MERGE_REQUEST_IID is not set.");
        }
        return project.createMergeRequestNote(env.pullRequest().id(), body);
    }
    
    private String requirePipelineId(String operation) {
        var pipelineId = env.pipelineId();
        if (StringUtils.isBlank(pipelineId)) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires CI_PIPELINE_ID to be available in the environment.");
        }
        return pipelineId;
    }
}
