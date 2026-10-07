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

import java.util.List;

import lombok.Builder;

/**
 * Holds the outcome of a full correlation stream run — both confirmed
 * and rejected SAST–DAST pairs, plus request statistics for the correlation phase.
 *
 * @param confirmedPairs                pairs where Phase 2 validation returned confirmed=true
 * @param rejectedPairs                 pairs where Phase 2 validation returned confirmed=false
 * @param submittedCorrelationRequests  number of Phase 1 correlation requests sent to the server
 * @param successfulCorrelationResponses number of Phase 1 requests with a successful server response
 * @param skippedCorrelationResponses   number of Phase 1 requests skipped by the server
 * @param failedCorrelationResponses    number of Phase 1 requests that failed
 */
@Builder
public record CorrelationResult(
    List<CorrelatedPair> confirmedPairs,
    List<CorrelatedPair> rejectedPairs,
    int submittedCorrelationRequests,
    int successfulCorrelationResponses,
    int skippedCorrelationResponses,
    int failedCorrelationResponses
) {
    public CorrelationResult {
        confirmedPairs = confirmedPairs == null ? List.of() : List.copyOf(confirmedPairs);
        rejectedPairs = rejectedPairs == null ? List.of() : List.copyOf(rejectedPairs);
    }

    public static CorrelationResult empty() {
        return CorrelationResult.builder()
            .build();
    }
}
