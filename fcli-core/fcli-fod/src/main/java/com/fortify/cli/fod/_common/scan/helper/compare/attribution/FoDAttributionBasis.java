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

/**
 * Basis of an attribution, ordered from strongest to weakest:
 * <ul>
 *  <li>DIRECT: issue-specific evidence, like the issue location having been added, removed or changed</li>
 *  <li>INFERRED: scan-wide condition that is consistent with the issue status transition</li>
 *  <li>POSSIBLE: weak or uncertain link; never sufficient to consider an issue attributed</li>
 * </ul>
 */
public enum FoDAttributionBasis {
    DIRECT, INFERRED, POSSIBLE;

    public boolean isStrongerThan(FoDAttributionBasis other) {
        return other == null || ordinal() < other.ordinal();
    }

    /** Return the weaker of this basis and the given maximum basis */
    public FoDAttributionBasis cap(FoDAttributionBasis max) {
        return max.isStrongerThan(this) ? this : max;
    }
}
