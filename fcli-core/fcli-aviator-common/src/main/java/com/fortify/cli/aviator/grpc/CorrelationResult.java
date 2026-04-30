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

/**
 * Holds the outcome of a full correlation stream run — both confirmed
 * and rejected SAST–DAST pairs.
 *
 * @param confirmedPairs pairs where Phase 2 validation returned confirmed=true
 * @param rejectedPairs  pairs where Phase 2 validation returned confirmed=false
 */
public record CorrelationResult(
    List<CorrelatedPair> confirmedPairs,
    List<CorrelatedPair> rejectedPairs
) {}
