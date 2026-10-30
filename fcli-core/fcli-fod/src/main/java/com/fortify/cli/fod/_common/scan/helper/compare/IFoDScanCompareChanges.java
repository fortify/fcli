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

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionMetric;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.IFoDAttributionChangeIndex;

/** Scan type-specific changes between two scans, like scan manifest file changes or site tree URL changes */
public interface IFoDScanCompareChanges {
    /**
     * Add scan type-specific change data to the compare result
     *
     * @param includeChange Predicate on file path or URL, determining whether individual changes are listed;
     *        counts always reflect all changes
     */
    void addTo(ObjectNode result, Predicate<String> includeChange);

    /** @return Index of scan type-specific changes for issue attribution, or null if changes couldn't be determined */
    IFoDAttributionChangeIndex getChangeIndex();

    /** @return Reason why scan type-specific changes couldn't be determined, or null if changes were determined */
    String getSkipReason();

    /** @return Scan type-specific changes, like added, changed and removed files; empty if changes couldn't be determined */
    List<FoDScanCompareChangeSet> getChangeSets();

    /** @return Additional metrics for issue attribution, like site tree statistics */
    Map<String, FoDAttributionMetric> getMetrics();
}
