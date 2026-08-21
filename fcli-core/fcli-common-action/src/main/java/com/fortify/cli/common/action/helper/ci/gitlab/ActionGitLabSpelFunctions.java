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

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.runner.ActionRunnerContextLocal;
import com.fortify.cli.common.ci.gitlab.GitLabRestHelper;
import com.fortify.cli.common.ci.gitlab.GitLabUnirestInstanceSupplier;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.spel.fn.descriptor.annotation.RenderSubFunctionsMode;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;

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
@SpelFunctionPrefix("gitlab.")
public class ActionGitLabSpelFunctions extends ActionGitLabCiInfoSpelFunctions {
    private final ActionRunnerContextLocal ctx;
    private GitLabRestHelper restHelper;
    
    /**
     * Create helper with automatic environment detection.
     * Does not throw if not in CI - use getEnv() != null to check.
     */
    public ActionGitLabSpelFunctions(ActionRunnerContextLocal ctx) {
        super();
        this.ctx = ctx;
    }
    
    /**
     * Create a project-scoped client using environment defaults.
     * Automatically extracts project ID from GitLab CI environment.
     * 
     * @return Project-scoped action client
     */
    @SpelFunction(cat=ci, desc="Returns a project-scoped GitLab client using project ID detected from the current pipeline run",
            returns="GitLab project client for CI operations",
            renderSubFunctions=RenderSubFunctionsMode.INLINE)
    public ActionGitLabProject project() {
        requireEnv("project");
        return new ActionGitLabProject(getRestHelper().project(requireProjectId("project")), env);
    }
    
    /**
     * Get the underlying RestHelper for advanced use cases.
     * Private since this is an internal implementation detail.
     * 
     * @return GitLabRestHelper instance
     */
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
    
    private String requireProjectId(String operation) {
        var projectId = env.projectId();
        if (StringUtils.isBlank(projectId)) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires CI_PROJECT_ID to be available in the environment.");
        }
        return projectId;
    }
}
