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

import org.apache.commons.lang3.StringUtils;

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
@SpelFunctionPrefix("ado.")
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
    @SpelFunction(cat=ci, desc="Returns Azure DevOps environment data as ObjectNode (auto-detected for the current pipeline run)",
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
        return AdoEnvironment.TYPE;
    }
    // === SARIF Upload (Advanced Security - Paid Tier) ===
    
    /**
     * Upload SARIF report to Azure DevOps Advanced Security.
     * Requires GitHub Advanced Security for Azure DevOps license.
     * 
     * SARIF format: https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html
     * 
     * @return Response from Azure DevOps API
     */
    @SpelFunction(cat=ci, desc="Uploads SARIF to ADO Advanced Security (paid tier); auto-detects organization/project/repository/commit from the current run",
            returns="Response from Azure DevOps API")
    public ObjectNode uploadSarif(
            @SpelFunctionParam(name="sarifContent", desc="SARIF report content") String sarifContent) {
        requireEnv("uploadSarif");
        var repositoryId = requireRepositoryId("uploadSarif");
        return getRestHelper().uploadSarif(
            requireOrganizationSlug("uploadSarif"),
            requireProject("uploadSarif"),
            repositoryId,
            env.ciBranch().short_(),
            env.ciCommit().id().full(),
            sarifContent);
    }
    
    // === Test Results (Free Tier - Can be adapted for security findings) ===
    
    /**
     * Publish test results using detected environment values.
     * While primarily for test results, this can be adapted to display security findings on free tier.
     * 
     * Supported formats: JUnit, NUnit, XUnit, VSTest, CTest
     * For security findings, format as test failures where test name = vulnerability title.
     * 
         * @param testRunner Test runner type (JUnit, NUnit, XUnit, VSTest, CTest)
         * @param testResults Test results content (JUnit XML, NUnit XML, etc.)
     * @return Response from Azure DevOps API
     */
    @SpelFunction(cat=ci, desc="Publishes test results (free tier); auto-detects project and build ID from the current pipeline run",
            returns="Response from Azure DevOps API")
    public ObjectNode publishTestResults(
            @SpelFunctionParam(name="testRunner", desc="test runner type (JUnit, NUnit, XUnit, VSTest, CTest)") String testRunner,
            @SpelFunctionParam(name="testResults", desc="test results in XML format (JUnit, NUnit, XUnit, etc.)") String testResults) {
        requireEnv("publishTestResults");
        return getRestHelper().publishTestResults(
            requireProject("publishTestResults"),
            requireBuildId("publishTestResults"),
            testResults,
            testRunner);
    }
    
    // === Pull Request Comments (Auto-Detect Context) ===
    
    /**
     * Add a comment thread to the current pull request.
     * Throws exception if not in pull request context.
     * Note: Repository ID is automatically detected from the current pipeline.
     * 
     * @param comment Comment text
     * @return Created thread object
     */
    @SpelFunction(cat=ci, desc="Adds a comment thread to the current pull request; auto-detects project, repository, and PR context",
            returns="Created thread object")
    public ObjectNode addPrThread(
            @SpelFunctionParam(name="comment", desc="comment text") String comment) {
        requireEnv("addPrThread");
        if (!env.pullRequest().active()) {
            throw new FcliSimpleException("Not running in pull request context. Build.SourceBranch does not indicate a PR.");
        }
        return getRestHelper().createPullRequestThread(
            requireProject("addPrThread"),
            requireRepositoryId("addPrThread"),
            env.pullRequest().id(),
            comment);
    }
    
    // === REST Helper Access ===
    
    private AdoRestHelper getRestHelper() {
        if (restHelper == null) {
            var supplier = AdoUnirestInstanceSupplier.fromEnv(ctx.getUnirestContext());
            restHelper = new AdoRestHelper(supplier);
        }
        return restHelper;
    }
    
    private void requireEnv(String operation) {
        if (env == null) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires Azure DevOps environment. " +
                "Set Build.Repository.Name and related environment variables (including Build.Repository.ID and Build.BuildId), or check " +
                "${#ci.ado().env != null} before calling.");
        }
    }

    private String requireRepositoryId(String operation) {
        var repositoryId = env.repositoryId();
        if (StringUtils.isBlank(repositoryId)) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires Build.Repository.ID to be available in the environment or passed explicitly.");
        }
        return repositoryId;
    }

    private String requireBuildId(String operation) {
        var buildId = env.buildId();
        if (StringUtils.isBlank(buildId)) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires Build.BuildId to be available in the environment or passed explicitly.");
        }
        return buildId;
    }

    private String requireProject(String operation) {
        var project = env.project();
        if (StringUtils.isBlank(project)) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires System.TeamProject to be available in the environment or passed explicitly.");
        }
        return project;
    }

    private String requireOrganizationSlug(String operation) {
        var orgUrl = env.organization();
        var organization = orgUrl != null ? orgUrl.replaceAll(".*/", "") : null;
        if (StringUtils.isBlank(organization)) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires System.TeamFoundationCollectionUri to derive the organization name.");
        }
        return organization;
    }
}
