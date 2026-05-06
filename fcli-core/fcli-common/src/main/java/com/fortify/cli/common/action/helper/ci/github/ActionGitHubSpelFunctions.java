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
package com.fortify.cli.common.action.helper.ci.github;

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.ci;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.helper.ci.IActionSpelFunctions;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.ci.github.GitHubEnvironment;
import com.fortify.cli.common.ci.github.GitHubRestHelper;
import com.fortify.cli.common.ci.github.GitHubUnirestInstanceSupplier;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.fn.descriptor.annotation.RenderSubFunctionsMode;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;

import lombok.RequiredArgsConstructor;

/**
 * Action-friendly GitHub helper providing convenient methods for CI/CD workflows.
 * Automatically detects GitHub Actions environment and provides both high-level
 * convenience methods and access to underlying REST helper for advanced use cases.
 * 
 * This class is designed for use in fcli actions via the #ci.github() SpEL function.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
@SpelFunctionPrefix("github.")
public class ActionGitHubSpelFunctions implements IActionSpelFunctions {
    private final ActionRunnerContext ctx;
    private final GitHubEnvironment env;
    private GitHubRestHelper restHelper;
    
    /**
     * Create helper with automatic environment detection.
     * Does not throw if not in CI - use getEnv() != null to check.
     */
    public ActionGitHubSpelFunctions(ActionRunnerContext ctx) {
        this.ctx = ctx;
        this.env = GitHubEnvironment.detect();
    }
    
    /**
     * Get environment data as ObjectNode for use in actions.
     * Returns null if not running in GitHub Actions.
     * Can be accessed in action YAML as: ${#ci.github().env}
     */
    @SpelFunction(cat=ci, desc="Returns GitHub Actions environment data as ObjectNode (auto-detected for the current workflow run)",
            returns="Environment data or `null` if not running in GitHub Actions",
            returnType=GitHubEnvironment.class)
    @Override
    public ObjectNode getEnv() {
        return env != null ? JsonHelper.getObjectMapper().valueToTree(env) : null;
    }
    
    /**
     * Returns "github" as the CI system type.
     */
    @SpelFunction(cat=ci, desc="Returns CI system type identifier",
            returns="\"github\"")
    @Override
    public String getType() {
        return GitHubEnvironment.TYPE;
    }
    
    /**
     * Create a repository-scoped client using environment defaults.
     * Automatically extracts owner and repository name from GitHub Actions environment.
     * 
     * @return Repository-scoped action client
     */
    @SpelFunction(cat=ci, desc="Returns a repository-scoped GitHub client using repository and owner detected from the current workflow run",
            returns="GitHub repository client for CI operations",
            renderSubFunctions=RenderSubFunctionsMode.INLINE)
    public ActionGitHubRepo repo() {
        requireEnv("repo");
        var repoName = env.ciRepository().name();
        var owner = repoName.full().contains("/") ? repoName.full().split("/")[0] : "";
        var repo = repoName.short_();
        return new ActionGitHubRepo(getRestHelper().repo(owner, repo), env);
    }
    
    /**
     * Get the underlying RestHelper for advanced use cases.
     * Private since this is an internal implementation detail.
     * 
     * @return GitHubRestHelper instance
     */
    private GitHubRestHelper getRestHelper() {
        if (restHelper == null) {
            var supplier = GitHubUnirestInstanceSupplier.fromEnv(ctx.getUnirestContext());
            restHelper = new GitHubRestHelper(supplier);
        }
        return restHelper;
    }
    
    private void requireEnv(String operation) {
        if (env == null) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires GitHub Actions environment. " +
                "Set GITHUB_REPOSITORY and related environment variables, or check " +
                "${#ci.github().env != null} before calling.");
        }
    }
}
