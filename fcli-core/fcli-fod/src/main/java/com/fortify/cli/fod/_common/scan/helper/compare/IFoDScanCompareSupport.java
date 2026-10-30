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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;

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

    void addScanTypeDeltas(UnirestInstance unirest, FoDScanComparePair pair, ObjectNode result);
}
