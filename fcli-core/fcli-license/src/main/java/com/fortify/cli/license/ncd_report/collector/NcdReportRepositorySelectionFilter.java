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
package com.fortify.cli.license.ncd_report.collector;

import com.fortify.cli.license.ncd_report.writer.NcdReportRepositoriesWriter.NcdReportRepositoryReportingStatus;

public enum NcdReportRepositorySelectionFilter {
    included,
    excluded,
    all;

    public boolean matches(NcdReportRepositoryReportingStatus status) {
        return switch ( this ) {
        case included -> status == NcdReportRepositoryReportingStatus.included;
        case excluded -> status == NcdReportRepositoryReportingStatus.excluded;
        case all -> true;
        };
    }
}
