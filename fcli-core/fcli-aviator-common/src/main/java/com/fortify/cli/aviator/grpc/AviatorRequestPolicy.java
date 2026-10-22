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

import java.util.Objects;

import io.grpc.stub.AbstractBlockingStub;

/** Unary request behavior; audit and diagnostic clients use {@link #WAIT_FOR_READY}. */
public enum AviatorRequestPolicy {
    FAIL_FAST,
    WAIT_FOR_READY;

    boolean isWaitForReady() {
        return this == WAIT_FOR_READY;
    }

    /** Requires a fresh stub: gRPC's default is fail-fast, and wait-for-ready cannot be unset through the stub API. */
    <S extends AbstractBlockingStub<S>> S configureNewStub(S stub, AviatorConnectionContext context) {
        if ( isWaitForReady() ) {
            return stub.withWaitForReady();
        }
        return stub.withOption(AviatorConnectionContext.CALL_OPTION, Objects.requireNonNull(context, "connectionContext"));
    }

}
