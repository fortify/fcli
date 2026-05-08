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
package com.fortify.cli.common.concurrent.job;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.cli.util.FcliActionState;
import com.fortify.cli.common.cli.util.FcliExecutionContext;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.cli.util.FcliIsolationScope;
import com.fortify.cli.common.util.OutputHelper.Result;

class AsyncJobManagerIsolationScopeTest {
    @Test
    void jobsAreTrackedWithinTheCurrentIsolationScope() {
        var manager = new AsyncJobManager();
        var scopeOne = new FcliIsolationScope();
        var scopeTwo = new FcliIsolationScope();

        try {
            FcliExecutionContextHolder.push(new FcliExecutionContext(scopeOne, new FcliActionState()));
            var jobId = manager.startBackground(AsyncJobManager.TaskDescriptor.builder()
                    .task(recordConsumer -> new Result(0, "", ""))
                    .description("scope-one-job")
                    .build());
            assertNotNull(manager.getJobInfo(jobId));
            FcliExecutionContextHolder.pop();

            FcliExecutionContextHolder.push(new FcliExecutionContext(scopeTwo, new FcliActionState()));
            try {
                assertNull(manager.getJobInfo(jobId));
            } finally {
                FcliExecutionContextHolder.pop();
            }

            FcliExecutionContextHolder.push(new FcliExecutionContext(scopeOne, new FcliActionState()));
            try {
                assertNotNull(manager.getJobInfo(jobId));
            } finally {
                FcliExecutionContextHolder.pop();
            }
        } finally {
            manager.shutdown();
        }
    }
}
