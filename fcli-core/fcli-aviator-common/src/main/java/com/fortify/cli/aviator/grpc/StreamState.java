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