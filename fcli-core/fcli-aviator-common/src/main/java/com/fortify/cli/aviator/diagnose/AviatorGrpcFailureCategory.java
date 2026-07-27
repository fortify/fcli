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
package com.fortify.cli.aviator.diagnose;

/**
 * Probe-internal gRPC failure taxonomy used by classification.
 * Not emitted on stage evidence JSON; public automation should use
 * {@link AviatorGrpcPattern} / evidence {@code pattern} only.
 */
public enum AviatorGrpcFailureCategory {
    NONE,
    NON_GRPC_HTTP,
    TLS,
    NO_RESPONSE;
}
