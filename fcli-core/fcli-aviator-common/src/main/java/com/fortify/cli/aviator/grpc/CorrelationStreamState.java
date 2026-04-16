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
package com.fortify.cli.aviator.grpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the state of a correlation gRPC bidi stream across its
 * two phases: initial correlation and validation.
 */
class CorrelationStreamState {

    enum Phase { INIT, CORRELATING, VALIDATING, COMPLETE }

    volatile String streamId;
    final String token;
    final String applicationName;
    final String sscApplicationName;
    final String sscApplicationVersion;
    final String fprBuildId;

    volatile Phase currentPhase = Phase.INIT;

    // Correlation phase counters
    int totalCorrelationRequests;
    final AtomicInteger sentCorrelations = new AtomicInteger(0);
    final AtomicInteger receivedCorrelations = new AtomicInteger(0);

    // Validation phase counters
    int totalValidationRequests;
    final AtomicInteger sentValidations = new AtomicInteger(0);
    final AtomicInteger receivedValidations = new AtomicInteger(0);

    // Quota from init response
    volatile long quota = -1;

    // Results
    final List<CandidateMatch> candidateMatches = Collections.synchronizedList(new ArrayList<>());
    final List<CorrelatedPair> confirmedPairs = Collections.synchronizedList(new ArrayList<>());

    // Retry
    volatile int streamRetryCount = 0;
    volatile boolean isStreamInitialized = false;

    CorrelationStreamState(String streamId, CorrelationStreamConfig config) {
        this.streamId = streamId;
        this.token = config.token();
        this.applicationName = config.applicationName();
        this.sscApplicationName = config.sscApplicationName();
        this.sscApplicationVersion = config.sscApplicationVersion();
        this.fprBuildId = config.fprBuildId();
    }

    /**
     * Intermediate match from the correlation phase, pending validation.
     */
    record CandidateMatch(
        String sastInstanceId,
        String url,
        String confidence,
        String rationale
    ) {}
}
