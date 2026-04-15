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
    @Option(names = "--async-bg-threads") private Integer bgThreads;

    /**
     * Build an {@link AsyncJobManager} whose configuration merges explicit CLI values (when
     * supplied) with the provided server-specific defaults (for values not supplied).
     */
    public AsyncJobManager buildAsyncJobManager(AsyncJobManager.Config defaults) {
        var config = AsyncJobManager.Config.builder()
            .bgThreads(bgThreads != null ? bgThreads : defaults.getBgThreads())
            .build();
        return new AsyncJobManager(config);
    }
}
