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
package com.fortify.cli.common.action.helper.ci.bitbucket;

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.ci;

import org.apache.commons.lang3.StringUtils;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.runner.ActionRunnerContextLocal;
import com.fortify.cli.common.ci.bitbucket.BitbucketRestHelper;
import com.fortify.cli.common.ci.bitbucket.BitbucketUnirestInstanceSupplier;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.spel.fn.descriptor.annotation.RenderSubFunctionsMode;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;

/**
 * Bitbucket-specific helper exposed to actions through {@code #ci.bitbucket()}.
 * Provides shortcuts for Bitbucket Code Insights report workflows including
 * uploading reports and annotations without requiring YAML authors to deal
 * with REST plumbing.
 */
@Reflectable
@SpelFunctionPrefix("bitbucket.")
public class ActionBitbucketSpelFunctions extends ActionBitbucketCiInfoSpelFunctions {
    private final ActionRunnerContextLocal ctx;
    private BitbucketRestHelper restHelper;

    public ActionBitbucketSpelFunctions(ActionRunnerContextLocal ctx) {
        super();
        this.ctx = ctx;
    }
    
    /**
     * Create a repository-scoped client using environment defaults.
     * Automatically extracts workspace and repository slug from Bitbucket Pipelines environment.
     * 
     * @return Repository-scoped action client
     */
    @SpelFunction(cat=ci, desc="Returns a repository-scoped Bitbucket client using workspace and repository detected from the current pipeline run",
            returns="Bitbucket repository client for CI operations",
            renderSubFunctions=RenderSubFunctionsMode.INLINE)
    public ActionBitbucketRepository repo() {
        requireEnv("repo");
        return new ActionBitbucketRepository(
            getRestHelper().repository(
                requireWorkspace("repo"),
                requireRepositorySlug("repo")
            ), 
            env
        );
    }
    
    /**
     * Get the underlying RestHelper for advanced use cases.
     * Private since this is an internal implementation detail.
     * 
     * @return BitbucketRestHelper instance
     */
    private BitbucketRestHelper getRestHelper() {
        if (restHelper == null) {
            var supplier = BitbucketUnirestInstanceSupplier.fromEnv(ctx.getUnirestContext());
            restHelper = new BitbucketRestHelper(supplier);
        }
        return restHelper;
    }

    private void requireEnv(String operation) {
        if (env == null) {
            throw new FcliSimpleException("Operation '" + operation + "' requires Bitbucket Pipelines environment. " +
                "Ensure BITBUCKET_REPO_SLUG and related variables are set, or check ${#ci.bitbucket().env != null} before calling.");
        }
    }

    private String requireWorkspace(String operation) {
        var workspace = env.workspace();
        if (StringUtils.isBlank(workspace)) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires BITBUCKET_WORKSPACE or BITBUCKET_REPO_OWNER to be available in the environment.");
        }
        return workspace;
    }

    private String requireRepositorySlug(String operation) {
        var slug = env.repositorySlug();
        if (StringUtils.isBlank(slug)) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires BITBUCKET_REPO_SLUG to be available in the environment.");
        }
        return slug;
    }
}
