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
package com.fortify.cli.common.action.helper.ci.ado;

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.ci;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.helper.ci.IActionSpelFunctions;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.ci.ado.AdoEnvironment;
import com.fortify.cli.common.ci.ado.AdoRestHelper;
import com.fortify.cli.common.ci.ado.AdoUnirestInstanceSupplier;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.fn.descriptor.annotation.RenderSubFunctionsMode;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
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
    
    /**
     * Create a repository-scoped client using environment defaults.
     * Automatically extracts organization, project, and repository ID from Azure DevOps environment.
     * 
     * @return Repository-scoped action client
     */
    @SpelFunction(cat=ci, desc="Returns a repository-scoped Azure DevOps client using organization/project/repository detected from the current pipeline run",
            returns="ADO repository client for CI operations",
            renderSubFunctions=RenderSubFunctionsMode.INLINE)
    public ActionAdoRepository repo() {
        requireEnv("repo");
        return new ActionAdoRepository(
            getRestHelper().repository(
                requireOrganizationSlug("repo"),
                requireProject("repo"),
                requireRepositoryId("repo")
            ), 
            env
        );
    }
    
    /**
     * Create a project-scoped client using environment defaults.
     * Automatically extracts organization and project from Azure DevOps environment.
     * 
     * @return Project-scoped action client
     */
    @SpelFunction(cat=ci, desc="Returns a project-scoped Azure DevOps client using organization and project detected from the current pipeline run",
            returns="ADO project client for CI operations",
            renderSubFunctions=RenderSubFunctionsMode.INLINE)
    public ActionAdoProject project() {
        requireEnv("project");
        return new ActionAdoProject(
            getRestHelper().project(
                requireOrganizationSlug("project"),
                requireProject("project")
            ), 
            env
        );
    }
    
    /**
     * Get the underlying RestHelper for advanced use cases.
     * Private since this is an internal implementation detail.
     * 
     * @return AdoRestHelper instance
     */
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
                "Set Build.Repository.Name and related environment variables, or check " +
                "${#ci.ado().env != null} before calling.");
        }
    }
    
    private String requireRepositoryId(String operation) {
        var repositoryId = env.repositoryId();
        if (StringUtils.isBlank(repositoryId)) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires Build.Repository.ID to be available in the environment.");
        }
        return repositoryId;
    }
    
    private String requireProject(String operation) {
        var project = env.project();
        if (StringUtils.isBlank(project)) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires System.TeamProject to be available in the environment.");
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
