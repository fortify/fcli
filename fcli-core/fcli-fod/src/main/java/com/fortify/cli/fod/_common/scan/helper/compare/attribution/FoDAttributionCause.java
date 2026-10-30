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

import static com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionBasis.DIRECT;
import static com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionBasis.INFERRED;
import static com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionBasis.POSSIBLE;
import static com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionEffects.all;
import static com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionEffects.none;

import com.fortify.cli.fod._common.scan.helper.compare.attribution.FoDAttributionEffects.Table;

/**
 * Catalog of attribution causes. Each cause declares which issue status transitions it can explain,
 * and with which basis, depending on the direction of the change. Issue-level evidence is always
 * non-directional ({@link FoDAttributionDirection#ANY}); scan-level conditions are capped at
 * {@link FoDAttributionBasis#INFERRED} by the attribution engine.
 */
public enum FoDAttributionCause {
    // Issue-level, SAST
    FILE_ADDED(Table.any(none().newIssues(DIRECT).reopened(DIRECT))),
    FILE_REMOVED(Table.any(none().fixed(DIRECT))),
    FILE_CHANGED(Table.any(all(DIRECT))),
    FILE_MOVED(Table.any(all(DIRECT))),
    FILE_MATCH_AMBIGUOUS(Table.any(all(POSSIBLE))),
    // Issue-level, SAST dataflow (based on issue traces, for issues at an unchanged sink location)
    SOURCE_FILE_ADDED(Table.any(none().newIssues(DIRECT).reopened(DIRECT))),
    SOURCE_FILE_REMOVED(Table.any(none().fixed(DIRECT))),
    SOURCE_FILE_CHANGED(Table.any(all(INFERRED))),
    SOURCE_FILE_MOVED(Table.any(all(INFERRED))),
    DATAFLOW_FILE_CHANGED(Table.any(all(INFERRED))),
    INSTANCE_ID_CORRELATION_ERROR(Table.any(all(INFERRED))),
    // Issue-level, DAST
    URL_ADDED(Table.any(none().newIssues(DIRECT).reopened(DIRECT))),
    URL_REMOVED(Table.any(none().fixed(DIRECT))),
    URL_METHOD_CHANGED(Table.any(all(DIRECT))),
    URL_STATUS_CHANGED(Table.any(all(DIRECT))),
    URL_CHANGED(Table.any(all(POSSIBLE))),
    RELATED_URL_CHANGED(Table.any(all(POSSIBLE))),
    // Scan-level, SAST
    PACKAGING_CHANGE(Table.directional()),
    MASS_RELOCATION(Table.any(none().newIssues(INFERRED).fixed(INFERRED))),
    ANALYZER_CHANGE(Table.any(none().newIssues(INFERRED).fixed(INFERRED))),
    // Scan-level, SAST & DAST
    SCAN_POLICY_CHANGE(Table.directional()),
    // Scan-level (and issue-level for matching hosts), DAST
    TARGET_CHANGE(Table.any(none().newIssues(DIRECT).reopened(INFERRED).fixed(DIRECT))),
    SCOPE_CHANGE(Table.directional()),
    AUTH_CONFIG_CHANGE(Table.directional()),
    COVERAGE_CHANGE(Table.directional()),
    AVAILABILITY_CHANGE(Table.directional()),
    AUTHENTICATION_CHANGE(Table.directional()),
    TIMEBOX_CHANGE(Table.weakDirectional());

    private final Table effects;

    FoDAttributionCause(Table effects) {
        this.effects = effects;
    }

    public FoDAttributionEffects getEffects(FoDAttributionDirection direction) {
        return effects.get(direction);
    }
}
