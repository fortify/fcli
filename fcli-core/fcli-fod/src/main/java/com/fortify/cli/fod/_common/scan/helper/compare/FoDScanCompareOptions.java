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
package com.fortify.cli.fod._common.scan.helper.compare;

import lombok.Builder;

/**
 * Scan type-independent scan comparison options
 *
 * @param ignoreCanceled Whether to ignore canceled scans rather than failing
 * @param maxIssues Maximum number of issues to list for each of new, reopened and fixed issues
 * @param attribution Whether to attribute issue status changes to likely causes
 * @param limitChangeDetails Whether to only list changes referenced by issue attributions
 * @param changeThresholdPercent Minimum relative change of scan metrics to be considered significant
 */
@Builder
public record FoDScanCompareOptions(boolean ignoreCanceled, int maxIssues, boolean attribution, boolean limitChangeDetails,
        int changeThresholdPercent) {
    public FoDScanCompareOptions {
        FoDScanCompareHelper.requireMinimum("--max-issues", maxIssues, 0);
        FoDScanCompareHelper.requireRange("--change-threshold", changeThresholdPercent, 1, 100);
    }
}
