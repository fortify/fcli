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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fortify.aviator.application.ApplicationServiceGrpc;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;

class GrpcUtilExecuteGrpcCallTest {
    private static final String HINT = "Use the numeric id instead.";

    @Test
    void unimplementedReportsUnsupportedOperationWithUpgradeAdviceAndHint() {
        String message = executeUnimplemented(HINT).getMessage();
        assertTrue(message.contains("test-op"), message);
        assertTrue(message.contains("upgrade the Aviator server"), message);
        assertTrue(message.endsWith(HINT), message);
    }

    @Test
    void unimplementedWithoutHintOmitsHint() {
        String message = executeUnimplemented(null).getMessage();
        assertTrue(message.contains("upgrade the Aviator server"), message);
        assertTrue(!message.contains("null"), message);
    }

    private static AviatorSimpleException executeUnimplemented(String hint) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 1).usePlaintext().build();
        try {
            var stub = ApplicationServiceGrpc.newBlockingStub(channel);
            return assertThrows(AviatorSimpleException.class, () -> GrpcUtil.executeGrpcCall(stub,
                    (s, r) -> { throw Status.UNIMPLEMENTED.withDescription("unknown method").asRuntimeException(); },
                    "request", "test-op", hint));
        } finally {
            channel.shutdownNow();
        }
    }
}
