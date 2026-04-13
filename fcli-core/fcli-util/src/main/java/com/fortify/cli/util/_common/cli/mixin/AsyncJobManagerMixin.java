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
package com.fortify.cli.util._common.cli.mixin;

import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.util._common.helper.AsyncJobManager;

import picocli.CommandLine.Option;

/**
 * Shared mixin for configuring the {@link AsyncJobManager} used by both the MCP and RPC
 * server commands. All options are optional; callers supply server-specific defaults via
 * {@link #buildAsyncJobManager(AsyncJobManager.Config)}.
 *
 * @author Ruud Senden
 */
public class AsyncJobManagerMixin {
    private static final DateTimePeriodHelper PERIOD_HELPER =
        DateTimePeriodHelper.byRange(Period.MILLISECONDS, Period.MINUTES);

    @Option(names = "--async-max-jobs") private Integer maxJobs;
    @Option(names = "--async-bg-threads") private Integer bgThreads;
    @Option(names = "--async-job-ttl") private String jobTtl;

    /**
     * Build an {@link AsyncJobManager} whose configuration merges explicit CLI values (when
     * supplied) with the provided server-specific defaults (for values not supplied).
     */
    public AsyncJobManager buildAsyncJobManager(AsyncJobManager.Config defaults) {
        var config = AsyncJobManager.Config.builder()
            .maxEntries(maxJobs != null ? maxJobs : defaults.getMaxEntries())
            .bgThreads(bgThreads != null ? bgThreads : defaults.getBgThreads())
            .ttlMillis(jobTtl != null && !jobTtl.isBlank()
                ? PERIOD_HELPER.parsePeriodToMillis(jobTtl)
                : defaults.getTtlMillis())
            .build();
        return new AsyncJobManager(config);
    }
}
