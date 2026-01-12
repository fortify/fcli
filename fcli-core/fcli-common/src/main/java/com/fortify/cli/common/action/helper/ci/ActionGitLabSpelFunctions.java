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
package com.fortify.cli.common.action.helper.ci;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.ci.gitlab.GitLabEnvironment;
import com.fortify.cli.common.rest.ci.gitlab.GitLabRestHelper;
import com.fortify.cli.common.rest.ci.gitlab.GitLabUnirestInstanceSupplier;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;

import lombok.RequiredArgsConstructor;

/**
 * Action-friendly GitLab helper providing convenient methods for CI/CD workflows.
 * Automatically detects GitLab CI environment and provides both high-level
 * convenience methods and access to underlying REST helper for advanced use cases.
 * 
 * This class is designed for use in fcli actions via the #ci.gitlab() SpEL function.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
@SpelFunctionPrefix("ci.gitlab()")
public class ActionGitLabSpelFunctions {
    private final ActionRunnerContext ctx;
    private final GitLabEnvironment env;
    private GitLabRestHelper restHelper;
    
    /**
     * Create helper with automatic environment detection.
     * Does not throw if not in CI - use getEnv() != null to check.
     */
    public ActionGitLabSpelFunctions(ActionRunnerContext ctx) {
        this.ctx = ctx;
        this.env = GitLabEnvironment.detect();
    }
    
    /**
     * Get environment data as ObjectNode for use in actions.
     * Returns null if not running in GitLab CI.
     * Can be accessed in action YAML as: ${#ci.gitlab().env}
     */
    public ObjectNode getEnv() {
        return env != null ? JsonHelper.getObjectMapper().valueToTree(env) : null;
    }
    
    // === Security Report Upload (Convenience Methods) ===
    
    /**
     * Upload security report using detected environment values.
     * Throws exception if not in GitLab CI or required env vars not set.
     * 
     * @param reportContent Report content (JSON format)
     * @param reportType Report type (sast, dast, dependency_scanning, etc.)
     * @return Response from GitLab API
     */
    public ObjectNode uploadSecurityReport(String reportContent, String reportType) {
        requireEnv("uploadSecurityReport");
        return getRestHelper().uploadSecurityReport(
            env.projectId(), env.pipelineId(), reportType, reportContent);
    }
    
    /**
     * Upload security report with explicit parameters.
     */
    public ObjectNode uploadSecurityReport(String reportContent, int projectId, 
                                            int pipelineId, String reportType) {
        return getRestHelper().uploadSecurityReport(projectId, pipelineId, reportType, reportContent);
    }
    
    // === Merge Request Comments (Auto-Detect Context) ===
    
    /**
     * Add a comment to the current merge request.
     * Throws exception if not in merge request context.
     * 
     * @param body Comment body (Markdown supported)
     * @return Created note object
     */
    public ObjectNode addMrComment(String body) {
        if (!env.isMergeRequest()) {
            throw new FcliSimpleException("Not running in merge request context. CI_MERGE_REQUEST_IID is not set.");
        }
        return getRestHelper().createMergeRequestNote(
            env.projectId(), env.mergeRequestId(), body);
    }
    
    /**
     * Add a merge request comment with explicit parameters.
     */
    public ObjectNode addMrComment(int projectId, int mrIid, String body) {
        return getRestHelper().createMergeRequestNote(projectId, mrIid, body);
    }
    
    // === REST Helper Access ===
    
    private GitLabRestHelper getRestHelper() {
        if (restHelper == null) {
            var supplier = GitLabUnirestInstanceSupplier.builder(ctx.getUnirestContext()).build();
            restHelper = new GitLabRestHelper(supplier);
        }
        return restHelper;
    }
    
    private void requireEnv(String operation) {
        if (env == null) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires GitLab CI environment. " +
                "Set GITLAB_CI=true and related environment variables, or check " +
                "${#ci.gitlab().env != null} before calling.");
        }
    }
}
