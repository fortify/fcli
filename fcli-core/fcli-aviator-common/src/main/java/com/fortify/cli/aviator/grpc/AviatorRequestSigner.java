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

/**
 * Signs one attempt of an Aviator admin request. Implementations must produce a fresh timestamp on every call:
 * a retry keeps its operation id and payload but is always re-signed, because the server enforces signature
 * and freshness on every attempt.
 */
@FunctionalInterface
public interface AviatorRequestSigner {
    /**
     * @param fullMethodName fully qualified gRPC method (bound into the signature)
     * @param operationId    operation id to bind into the signature, or {@code null} when the server only
     *                       understands the legacy message format
     */
    SignedRequest sign(String fullMethodName, String operationId);

    record SignedRequest(String message, String signature) {}
}
