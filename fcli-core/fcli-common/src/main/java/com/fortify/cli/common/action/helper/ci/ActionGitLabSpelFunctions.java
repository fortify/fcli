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

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.ci;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.ci.gitlab.GitLabEnvironment;
import com.fortify.cli.common.ci.gitlab.GitLabRestHelper;
import com.fortify.cli.common.ci.gitlab.GitLabUnirestInstanceSupplier;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
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
@SpelFunctionPrefix("gitlab.")
public class ActionGitLabSpelFunctions implements IActionSpelFunctions {
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
    @SpelFunction(cat=ci, desc="Returns GitLab CI environment data as ObjectNode (auto-detected for the current pipeline run)",
            returns="Environment data or `null` if not running in GitLab CI",
            returnType=GitLabEnvironment.class)
    @Override
    public ObjectNode getEnv() {
        return env != null ? JsonHelper.getObjectMapper().valueToTree(env) : null;
    }
    
    /**
     * Returns "gitlab" as the CI system type.
     */
    @SpelFunction(cat=ci, desc="Returns CI system type identifier",
            returns="\"gitlab\"")
    @Override
    public String getType() {
        return GitLabEnvironment.TYPE;
    }
    
    // === Security Report Upload (Ultimate/Premium Tier - Paid) ===
    
    /**
     * Upload security report using detected environment values.
     * Requires GitLab Ultimate or Premium tier.
     * 
     * Report schemas: https://docs.gitlab.com/ee/development/integrations/secure.html
     * 
         * @param reportType Report type (sast, dast, dependency_scanning, container_scanning, etc.)
         * @param reportContent Report content (JSON matching GitLab security report schema)
     * @return Response from GitLab API
     */
    @SpelFunction(cat=ci, desc="Uploads security report to GitLab (paid tier, requires Ultimate/Premium) using project/pipeline detected from the current run",
            returns="Response from GitLab API")
    public ObjectNode uploadSecurityReport(
            @SpelFunctionParam(name="reportType", desc="report type: sast, dast, dependency_scanning, container_scanning, etc.") String reportType,
            @SpelFunctionParam(name="reportContent", desc="security report in GitLab schema format") String reportContent) {
        requireEnv("uploadSecurityReport");
        return getRestHelper().uploadSecurityReport(
            env.projectId(), env.pipelineId(), reportType, reportContent);
    }
    
    // === Code Quality Report (Free Tier) ===
    
    /**
     * Upload code quality report to current merge request (available on all tiers).
     * Shows code quality degradation in merge request UI.
     * 
     * Code quality format: https://docs.gitlab.com/ee/ci/testing/code_quality.html#implement-a-custom-tool
     * 
     * @param reportContent Code quality report (JSON array with description, severity, location)
     * @return Response from GitLab API
     */
    @SpelFunction(cat=ci, desc="Uploads code quality report to the detected merge request (free tier, all GitLab tiers)",
            returns="Response from GitLab API")
    public ObjectNode uploadCodeQualityReport(
            @SpelFunctionParam(name="reportContent", desc="code quality report as JSON array") String reportContent) {
        requireEnv("uploadCodeQualityReport");
        if (!env.pullRequest().active()) {
            throw new FcliSimpleException("Not running in merge request context. CI_MERGE_REQUEST_IID is not set.");
        }
        return getRestHelper().uploadCodeQualityReport(env.projectId(), env.pullRequest().id(), reportContent);
    }
    
    // === Merge Request Comments (Auto-Detect Context) ===
    
    /**
     * Add a comment to the current merge request.
     * Throws exception if not in merge request context.
     * 
     * @param body Comment body (Markdown supported)
     * @return Created note object
     */
    @SpelFunction(cat=ci, desc="Adds a comment to the merge request detected from the current pipeline run",
            returns="Created note object")
    public ObjectNode addMrComment(
            @SpelFunctionParam(name="body", desc="comment body (Markdown supported)") String body) {
        if (!env.pullRequest().active()) {
            throw new FcliSimpleException("Not running in merge request context. CI_MERGE_REQUEST_IID is not set.");
        }
        return getRestHelper().createMergeRequestNote(
            env.projectId(), env.pullRequest().id(), body);
    }
    
    // === REST Helper Access ===
    
    private GitLabRestHelper getRestHelper() {
        if (restHelper == null) {
            var supplier = GitLabUnirestInstanceSupplier.fromEnv(ctx.getUnirestContext());
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
