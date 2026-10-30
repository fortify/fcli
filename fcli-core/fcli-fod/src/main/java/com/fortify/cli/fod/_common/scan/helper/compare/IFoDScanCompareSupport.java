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
import java.util.Set;

import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.IFoDIssueEvidenceMatcher;
import com.fortify.cli.fod._common.scan.helper.compare.attribution.IFoDScanConditionDetector;

import kong.unirest.UnirestInstance;

/**
 * Scan type-specific behavior for comparing two scans; generic behavior is
 * implemented in {@link FoDScanCompareHelper}.
 */
public interface IFoDScanCompareSupport {
    FoDScanType getScanType();

    /**
     * Lower-case assessment type names of imported scans that don't affect Fortify issue states,
     * like imports from non-Fortify tools; these scans are ignored when locating scans to compare.
     * Any other imported scan ('... (Imported)' assessment type) is considered to affect issue states.
     */
    Set<String> getIgnoredAssessmentTypes();

    /** Scan settings, for which only changed values are reported */
    List<FoDScanCompareField> getSettingFields();

    /** Scan statistics, for which previous and current values are always reported */
    List<FoDScanCompareField> getStatisticFields();

    /** Load scan type-specific changes, like scan manifest or site tree changes */
    IFoDScanCompareChanges loadChanges(UnirestInstance unirest, FoDScanComparePair pair);

    /** Detectors for scan-level conditions that may explain issue status transitions */
    List<IFoDScanConditionDetector> getConditionDetectors();

    /** Matchers for issue-specific evidence that may explain issue status transitions */
    List<IFoDIssueEvidenceMatcher> getIssueMatchers();

    /** Whether issue request/response data should be looked up for attributing issues to request-level changes */
    default boolean isIssueRequestLookupSupported() {
        return false;
    }

    /** Whether issue traces should be looked up for attributing issues to dataflow file changes */
    default boolean isIssueTraceLookupSupported() {
        return false;
    }

    /** Scan type-specific issue fields to be loaded in addition to the generic issue fields */
    default List<String> getAdditionalIssueFields() {
        return List.of();
    }
}
