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
package com.fortify.cli.fod._common.scan.helper.compare.attribution;

import java.util.List;

/**
 * Detects large-scale file relocation, like renaming or reorganizing source directories, which
 * typically causes many issues to be marked as fixed, together with corresponding new issues.
 */
public final class FoDAttributionMassRelocationDetector implements IFoDScanConditionDetector {
    @Override
    public List<FoDScanCondition> detect(FoDAttributionContext context) {
        var index = context.getChangeIndex(FoDAttributionFileChangeIndex.class);
        if ( index == null || index.getPreviousFileCount() == 0 ) { return List.of(); }
        var moved = index.getMovedCount();
        var total = index.getPreviousFileCount();
        if ( moved < FoDAttributionMetricDetector.MIN_COUNTER_CHANGE
                || moved * 100d / total < context.getChangeThresholdPercent() ) {
            return List.of();
        }
        return List.of(new FoDScanCondition(FoDAttributionCause.MASS_RELOCATION, FoDAttributionDirection.ANY,
                List.of(String.format("%d of %d files were moved to a different location", moved, total))));
    }
}
