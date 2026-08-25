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
package com.fortify.cli.aviator._common.cli.mixin;

import java.util.List;

import com.fortify.cli.aviator._common.remediations_cache.IApplyRemediationsOptions;

import lombok.Getter;
import picocli.CommandLine.Option;

/** Shared apply-remediations options; used as a @Mixin in AbstractAviatorApplyRemediationsCommand. */
@Getter
public final class ApplyRemediationsOptionsMixin implements IApplyRemediationsOptions {
    @Option(names = {"--source-dir"})
    private String sourceCodeDirectory = System.getProperty("user.dir");
    @Option(names = {"--issue-ids"}, split = ",")
    private List<String> issueIds;
    @Option(names = {"--preview"})
    private boolean previewMode = false;
}
