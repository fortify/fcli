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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.ci.ado.AdoEnvironment;
import com.fortify.cli.common.ci.ado.AdoRepository;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctions;

import lombok.RequiredArgsConstructor;

/**
 * Action wrapper for AdoRepository providing environment-aware Azure DevOps repository operations.
 * Used by ActionAdoSpelFunctions to provide SpEL functions that automatically 
 * use Azure DevOps environment defaults.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
@SpelFunctions
public class ActionAdoRepository {
    private final AdoRepository repository;
    private final AdoEnvironment env;
    
    @SpelFunction(cat=ci, desc="(PREVIEW) Uploads SARIF to ADO Advanced Security (paid tier); auto-detects organization/project/repository/commit from the current run. This function is not yet used by any built-in fcli actions; signature and implementation may change in future fcli versions based on new insights as to how to best integrate this functionality into fcli built-in actions.",
            returns="Response from Azure DevOps API")
    public ObjectNode uploadSarif(
            @SpelFunctionParam(name="sarifContent", desc="SARIF report content") String sarifContent) {
        var ref = env.ciBranch().short_();
        var sha = env.ciCommit().headId().full();
        return repository.uploadSarif(ref, sha, sarifContent);
    }
}
