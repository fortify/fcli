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
import com.fortify.cli.common.ci.ado.AdoEnvironment;
import com.fortify.cli.common.ci.ado.AdoProject;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctions;

import lombok.RequiredArgsConstructor;

/**
 * Action wrapper for AdoProject providing environment-aware Azure DevOps project operations.
 * Used by ActionAdoSpelFunctions to provide SpEL functions that automatically 
 * use Azure DevOps environment defaults.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
@SpelFunctions
public class ActionAdoProject {
    private final AdoProject project;
    private final AdoEnvironment env;
    
    @SpelFunction(cat=ci, desc="(PREVIEW) Publishes test results (free tier); auto-detects project and build ID from the current pipeline run. This function is not yet used by any built-in fcli actions; signature and implementation may change in future fcli versions based on new insights as to how to best integrate this functionality into fcli built-in actions.",
            returns="Response from Azure DevOps API")
    public ObjectNode publishTestResults(
            @SpelFunctionParam(name="testRunner", desc="test runner type (JUnit, NUnit, XUnit, VSTest, CTest)") String testRunner,
            @SpelFunctionParam(name="testResults", desc="test results in XML format (JUnit, NUnit, XUnit, etc.)") String testResults) {
        return project.publishTestResults(requireBuildId("publishTestResults"), testResults, testRunner);
    }
    
    @SpelFunction(cat=ci, desc="(PREVIEW) Adds a comment thread to the current pull request; auto-detects project, repository, and PR context. This function is not yet used by any built-in fcli actions; signature and implementation may change in future fcli versions based on new insights as to how to best integrate this functionality into fcli built-in actions.",
            returns="Created thread object")
    public ObjectNode addPrThread(
            @SpelFunctionParam(name="comment", desc="comment text") String comment) {
        if (!env.pullRequest().active()) {
            throw new FcliSimpleException("Not running in pull request context. Build.SourceBranch does not indicate a PR.");
        }
        return project.createPullRequestThread(requireRepositoryId("addPrThread"), 
                                               env.pullRequest().id(), comment);
    }
    
    private String requireBuildId(String operation) {
        var buildId = env.buildId();
        if (StringUtils.isBlank(buildId)) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires Build.BuildId to be available in the environment.");
        }
        return buildId;
    }
    
    private String requireRepositoryId(String operation) {
        var repositoryId = env.repositoryId();
        if (StringUtils.isBlank(repositoryId)) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires Build.Repository.ID to be available in the environment.");
        }
        return repositoryId;
    }
}
