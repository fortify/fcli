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
import com.fortify.cli.common.ci.ado.AdoEnvironment;
import com.fortify.cli.common.ci.ado.AdoRestHelper;
import com.fortify.cli.common.ci.ado.AdoUnirestInstanceSupplier;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
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
@SpelFunctionPrefix("ci.ado().")
public class ActionAdoSpelFunctions implements IActionSpelFunctions {
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
    @SpelFunction(cat=ci, desc="Returns Azure DevOps environment data as ObjectNode",
            returns="Environment data or `null` if not running in Azure DevOps",
            returnType=AdoEnvironment.class)
    @Override
    public ObjectNode getEnv() {
        return env != null ? JsonHelper.getObjectMapper().valueToTree(env) : null;
    }
    
    /**
     * Returns "ado" as the CI system type.
     */
    @SpelFunction(cat=ci, desc="Returns CI system type identifier",
            returns="\"ado\"")
    @Override
    public String getType() {
        return "ado";
    }
    // === SARIF Upload (Advanced Security - Paid Tier) ===
    
    /**
     * Upload SARIF report to Azure DevOps Advanced Security.
     * Requires GitHub Advanced Security for Azure DevOps license.
     * 
     * SARIF format: https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html
     * 
     * @param sarifContent SARIF report content
     * @param repositoryId Repository ID (GUID)
     * @return Response from Azure DevOps API
     */
    @SpelFunction(cat=ci, desc="Uploads SARIF to ADO Advanced Security (paid tier, requires GHAS license)",
            returns="Response from Azure DevOps API")
    public ObjectNode uploadSarif(
            @SpelFunctionParam(name="sarifContent", desc="SARIF report content") String sarifContent,
            @SpelFunctionParam(name="repositoryId", desc="repository ID (GUID)") String repositoryId) {
        requireEnv("uploadSarif");
        // Extract organization from organization URL
        var orgUrl = env.organization();
        var organization = orgUrl != null ? orgUrl.replaceAll(".*/", "") : null;
        return getRestHelper().uploadSarif(
            organization, env.project(), repositoryId, env.ciBranch().short_(), env.ciCommit().id().full(), sarifContent);
    }
    
    /**
     * Upload SARIF report with explicit parameters.
     */
    @SpelFunction(cat=ci, desc="Uploads SARIF to ADO Advanced Security with explicit parameters (paid tier)",
            returns="Response from Azure DevOps API")
    public ObjectNode uploadSarif(
            @SpelFunctionParam(name="sarifContent", desc="SARIF report content") String sarifContent,
            @SpelFunctionParam(name="organization", desc="organization name") String organization,
            @SpelFunctionParam(name="project", desc="project name") String project,
            @SpelFunctionParam(name="repositoryId", desc="repository ID (GUID)") String repositoryId,
            @SpelFunctionParam(name="ref", desc="git ref (branch/tag)") String ref,
            @SpelFunctionParam(name="commitSha", desc="commit SHA") String commitSha) {
        return getRestHelper().uploadSarif(organization, project, repositoryId, ref, commitSha, sarifContent);
    }
    
    // === Test Results (Free Tier - Can be adapted for security findings) ===
    
    /**
     * Publish test results using detected environment values.
     * While primarily for test results, this can be adapted to display security findings on free tier.
     * 
     * Supported formats: JUnit, NUnit, XUnit, VSTest, CTest
     * For security findings, format as test failures where test name = vulnerability title.
     * 
     * @param testResults Test results content (JUnit XML, NUnit XML, etc.)
     * @param testRunner Test runner type (JUnit, NUnit, XUnit, VSTest, CTest)
     * @param buildId Build ID
     * @return Response from Azure DevOps API
     */
    @SpelFunction(cat=ci, desc="Publishes test results (free tier, can show security findings as test failures)",
            returns="Response from Azure DevOps API")
    public ObjectNode publishTestResults(
            @SpelFunctionParam(name="testResults", desc="test results in XML format (JUnit, NUnit, XUnit, etc.)") String testResults,
            @SpelFunctionParam(name="testRunner", desc="test runner type (JUnit, NUnit, XUnit, VSTest, CTest)") String testRunner,
            @SpelFunctionParam(name="buildId", desc="build ID") int buildId) {
        requireEnv("publishTestResults");
        return getRestHelper().publishTestResults(env.project(), buildId, testResults, testRunner);
    }
    
    /**
     * Publish test results with explicit parameters.
     */
    @SpelFunction(cat=ci, desc="Publishes test results with explicit parameters (free tier)",
            returns="Response from Azure DevOps API")
    public ObjectNode publishTestResults(
            @SpelFunctionParam(name="testResults", desc="test results in XML format") String testResults,
            @SpelFunctionParam(name="project", desc="project name") String project,
            @SpelFunctionParam(name="buildId", desc="build ID") int buildId,
            @SpelFunctionParam(name="testRunner", desc="test runner type (JUnit, NUnit, XUnit, VSTest, CTest)") String testRunner) {
        return getRestHelper().publishTestResults(project, buildId, testResults, testRunner);
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
    @SpelFunction(cat=ci, desc="Adds a comment thread to the current pull request",
            returns="Created thread object")
    public ObjectNode addPrThread(
            @SpelFunctionParam(name="repositoryId", desc="repository ID (GUID)") String repositoryId,
            @SpelFunctionParam(name="comment", desc="comment text") String comment) {
        requireEnv("addPrThread");
        if (!env.pullRequest().active()) {
            throw new FcliSimpleException("Not running in pull request context. Build.SourceBranch does not indicate a PR.");
        }
        return getRestHelper().createPullRequestThread(
            env.project(), repositoryId, env.pullRequest().id(), comment);
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
