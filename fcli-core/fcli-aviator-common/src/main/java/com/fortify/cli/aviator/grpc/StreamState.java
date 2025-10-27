/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.aviator.grpc;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class StreamState {
    String streamId;
    final String projectName;
    final String FPRBuildId;
    final String SSCApplicationName;
    final String SSCApplicationVersion;
    final String token;
    final int totalRequests;
    final Set<String> processedIssueIds = ConcurrentHashMap.newKeySet();
    final Set<String> pendingIssueIds = ConcurrentHashMap.newKeySet();
    volatile int streamRetryCount = 0;
    volatile boolean isStreamInitialized = false;

    StreamState(String streamId, String projectName, String FPRBuildId,
                String SSCApplicationName, String SSCApplicationVersion,
                String token, int totalRequests) {
        this.streamId = streamId;
        this.projectName = projectName;
        this.FPRBuildId = FPRBuildId;
        this.SSCApplicationName = SSCApplicationName;
        this.SSCApplicationVersion = SSCApplicationVersion;
        this.token = token;
        this.totalRequests = totalRequests;
    }
}