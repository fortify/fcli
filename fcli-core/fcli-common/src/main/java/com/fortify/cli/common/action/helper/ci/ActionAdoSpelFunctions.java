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
import com.fortify.cli.common.rest.ci.ado.AdoEnvironment;
import com.fortify.cli.common.rest.ci.ado.AdoRestHelper;
import com.fortify.cli.common.rest.ci.ado.AdoUnirestInstanceSupplier;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;

import lombok.RequiredArgsConstructor;

/**
 * Action-friendly Azure DevOps helper providing convenient methods for CI/CD workflows.
 * Automatically detects Azure DevOps environment and provides both high-level
 * convenience methods and access to underlying REST helper for advanced use cases.
 * 
 * This class is designed for use in fcli actions via the #ci.ado() SpEL function.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
@SpelFunctionPrefix("ci.ado()")
public class ActionAdoSpelFunctions {
    private final ActionRunnerContext ctx;
    private final AdoEnvironment env;
    private AdoRestHelper restHelper;
    
    /**
     * Create helper with automatic environment detection.
     * Does not throw if not in CI - use getEnv() != null to check.
     */
    public ActionAdoSpelFunctions(ActionRunnerContext ctx) {
        this.ctx = ctx;
        this.env = AdoEnvironment.detect();
    }
    
    /**
     * Get environment data as ObjectNode for use in actions.
     * Returns null if not running in Azure DevOps.
     * Can be accessed in action YAML as: ${#ci.ado().env}
     */
    public ObjectNode getEnv() {
        return env != null ? JsonHelper.getObjectMapper().valueToTree(env) : null;
    }
    

    // === Code Analysis Results Upload (Convenience Methods) ===
    
    /**
     * Upload code analysis results using detected environment values.
     * Note: Requires additional build context that may need explicit parameters.
     * 
     * @param results Analysis results (JSON format)
     * @param buildId Build ID
     * @return Response from Azure DevOps API
     */
    public ObjectNode uploadCodeAnalysisResults(String results, int buildId) {
        return getRestHelper().uploadCodeAnalysisResults(
            env.project(), buildId, results);
    }
    
    /**
     * Upload code analysis results with explicit parameters.
     */
    public ObjectNode uploadCodeAnalysisResults(String results, String project, int buildId) {
        return getRestHelper().uploadCodeAnalysisResults(project, buildId, results);
    }
    
    // === Pull Request Comments (Auto-Detect Context) ===
    
    /**
     * Add a comment thread to the current pull request.
     * Throws exception if not in pull request context.
     * Note: Requires repository ID which may need to be provided explicitly.
     * 
     * @param repositoryId Repository ID (GUID)
     * @param comment Comment text
     * @return Created thread object
     */
    public ObjectNode addPrThread(String repositoryId, String comment) {
        requireEnv("addPrThread");
        if (!env.isPullRequest()) {
            throw new FcliSimpleException("Not running in pull request context. Build.SourceBranch does not indicate a PR.");
        }
        return getRestHelper().createPullRequestThread(
            env.project(), repositoryId, env.pullRequestId(), comment);
    }
    
    // === REST Helper Access ===
    
    private AdoRestHelper getRestHelper() {
        if (restHelper == null) {
            var supplier = AdoUnirestInstanceSupplier.builder(ctx.getUnirestContext()).build();
            restHelper = new AdoRestHelper(supplier);
        }
        return restHelper;
    }
    
    private void requireEnv(String operation) {
        if (env == null) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires Azure DevOps environment. " +
                "Set Build.Repository.Name and related environment variables, or check " +
                "${#ci.ado().env != null} before calling.");
        }
    }
}
