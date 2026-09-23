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
package com.fortify.cli.aviator.ssc.helper;

import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.system_state.helper.SSCJobHelper;

import kong.unirest.UnirestInstance;

/**
 * Refreshes stale SSC application-version metrics before workflows read current state.
 */
public final class AviatorSSCRefreshHelper {
    private AviatorSSCRefreshHelper() {}

    public static void refreshMetricsIfNeeded(
            UnirestInstance unirest,
            SSCAppVersionDescriptor appVersion,
            boolean refreshEnabled,
            String refreshTimeout,
            IAviatorLogger logger) {
        if (!refreshEnabled || !appVersion.isRefreshRequired()) {
            return;
        }

        logger.progress("Status: Metrics for application version %s:%s are out of date, starting refresh...",
            appVersion.getApplicationName(), appVersion.getVersionName());
        var refreshJob = SSCAppVersionHelper.refreshMetrics(unirest, appVersion);
        if (refreshJob != null) {
            SSCJobHelper.waitForJob(unirest, refreshJob, refreshTimeout);
            logger.progress("Status: Metrics refreshed successfully.");
        }
    }
}