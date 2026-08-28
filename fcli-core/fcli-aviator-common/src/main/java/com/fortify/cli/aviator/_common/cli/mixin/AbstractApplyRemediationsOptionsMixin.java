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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.aviator._common.remediations_cache.IApplyRemediationsOptions;
import com.fortify.cli.common.exception.FcliSimpleException;

import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Abstract base for apply-remediations options. Provides shared CLI options and validation template method.
 * Product-specific subclasses declare source-selection mixins and implement validation hooks.
 */
@Getter
public abstract class AbstractApplyRemediationsOptionsMixin implements IApplyRemediationsOptions {
    @Option(names = {"--source-dir"})
    private String sourceCodeDirectory = System.getProperty("user.dir");

    @Option(names = {"--issue-ids"}, split = ",")
    private List<String> issueIds;

    @Option(names = {"--preview"})
    private boolean previewMode = false;

    /**
     * Validates all options by calling validation hooks in order.
     * Template method: ensures consistent validation sequence across SSC and FoD.
     */
    @Override
    public final void validate() {
        validateSourceSelection();
        validateSourceDir();
        validateIssueIdsConstraints();
    }

    /** Hook for product-specific source selection validation (--from-cache vs online selection). */
    protected abstract void validateSourceSelection();

    /** Hook to determine if --from-cache is selected (needed for --issue-ids constraint validation). */
    protected abstract boolean isCacheMode();

    private void validateSourceDir() {
        FcliSimpleException.throwIf(
                StringUtils.isBlank(sourceCodeDirectory),
                "--source-dir must specify a valid directory path");
        Path path = Path.of(sourceCodeDirectory);
        FcliSimpleException.throwIf(
                !Files.exists(path) || !Files.isDirectory(path),
                "--source-dir path does not exist or is not a directory: %s", sourceCodeDirectory);
        FcliSimpleException.throwIf(
                !Files.isReadable(path),
                "--source-dir path is not accessible: %s", sourceCodeDirectory);
    }

    private void validateIssueIdsConstraints() {
        FcliSimpleException.throwIf(
                issueIds != null && !issueIds.isEmpty() && !isCacheMode(),
                "--issue-ids can only be used with --from-cache; "
                        + "create a cache with download-remediations-cache and rerun with --from-cache");
    }
}
