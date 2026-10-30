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

import java.util.Collection;

/**
 * Overall attribution status of an issue, derived from its attributions; INDETERMINATE if there are no attributions
 * or only POSSIBLE attributions
 */
public enum FoDAttributionStatus {
    DIRECT, INFERRED, INDETERMINATE;

    static FoDAttributionStatus of(Collection<FoDAttribution> attributions) {
        if ( attributions.isEmpty() ) { return INDETERMINATE; }
        var strongest = attributions.stream().map(FoDAttribution::basis).min(Enum::compareTo).orElseThrow();
        return switch ( strongest ) {
            case DIRECT -> DIRECT;
            case INFERRED -> INFERRED;
            case POSSIBLE -> INDETERMINATE;
        };
    }
}
