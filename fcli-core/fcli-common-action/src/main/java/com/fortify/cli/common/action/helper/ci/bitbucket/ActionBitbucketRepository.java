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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.ci.bitbucket.BitbucketEnvironment;
import com.fortify.cli.common.ci.bitbucket.BitbucketRepository;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctions;

import lombok.RequiredArgsConstructor;

/**
 * Action wrapper for BitbucketRepository providing environment-aware Bitbucket repository operations.
 * Used by ActionBitbucketSpelFunctions to provide SpEL functions that automatically 
 * use Bitbucket Pipelines environment defaults.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
@SpelFunctions
public class ActionBitbucketRepository {
    private final BitbucketRepository repository;
    private final BitbucketEnvironment env;
    
    @SpelFunction(cat=ci, desc="Creates or updates a Bitbucket Code Insights report for the current commit using detected workspace/repository data",
            returns="Response from Bitbucket API")
    public ObjectNode uploadReport(
            @SpelFunctionParam(name="reportId", desc="Code Insights report key (for example test-001)") String reportId,
            @SpelFunctionParam(name="reportContent", desc="JSON payload that follows Bitbucket's Code Insights report schema") String reportContent) {
        var commit = requireCommitSha("uploadReport");
        var id = requireValue("reportId", reportId);
        return repository.upsertCommitReport(commit, id, reportContent);
    }
    
    @SpelFunction(cat=ci, desc="Appends annotations to an existing Bitbucket Code Insights report for the detected commit",
            returns="Response from Bitbucket API")
    public ObjectNode addReportAnnotations(
            @SpelFunctionParam(name="reportId", desc="Code Insights report key to associate the annotations with") String reportId,
            @SpelFunctionParam(name="annotationsContent", desc="JSON array of annotation objects that match Bitbucket's schema") String annotationsContent) {
        var commit = requireCommitSha("addReportAnnotations");
        var id = requireValue("reportId", reportId);
        return repository.addReportAnnotations(commit, id, annotationsContent);
    }
    
    private String requireCommitSha(String operation) {
        var commit = env.ciCommit() != null && env.ciCommit().headId() != null
            ? env.ciCommit().headId().full()
            : null;
        if (StringUtils.isBlank(commit)) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires BITBUCKET_COMMIT to be available in the environment.");
        }
        return commit;
    }
    
    private static String requireValue(String name, String value) {
        if (StringUtils.isBlank(value)) {
            throw new FcliSimpleException(name + " must be provided");
        }
        return value;
    }
}
