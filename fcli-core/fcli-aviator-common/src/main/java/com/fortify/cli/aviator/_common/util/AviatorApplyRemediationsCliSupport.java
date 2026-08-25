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
package com.fortify.cli.aviator._common.util;

import java.util.List;

import com.fortify.cli.common.exception.FcliSimpleException;

/**
 * Shared option validation for SSC/FoD apply-remediations commands (CLI surface only).
 */
public final class AviatorApplyRemediationsCliSupport {
    private AviatorApplyRemediationsCliSupport() {}

    public static void requireSourceDir(String sourceCodeDirectory) {
        FcliSimpleException.throwIf(sourceCodeDirectory == null || sourceCodeDirectory.isBlank(),
                "--source-dir must specify a valid directory path");
    }

    /** Enforces that --issue-ids is only used with --from-cache. */
    public static void requireIssueIdsCacheOnly(List<String> issueIds, boolean fromCacheSelected) {
        FcliSimpleException.throwIf(
                issueIds != null && !issueIds.isEmpty() && !fromCacheSelected,
                "--issue-ids can only be used with --from-cache; "
                        + "create a cache with download-remediations-cache and rerun with --from-cache");
    }
}
