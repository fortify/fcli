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

/** Looks up the dataflow traces associated with a SAST issue */
public interface IFoDIssueTraceLookup {
    /** @return Issue traces, or null if not available, for example because a maximum number of lookups was reached */
    List<FoDIssueTrace> lookup(FoDAttributionIssue issue);

    /** @return Number of performed lookups */
    int getLookupCount();

    /** @return Whether any lookups were skipped because the maximum number of lookups was reached */
    boolean isLimitReached();
}
