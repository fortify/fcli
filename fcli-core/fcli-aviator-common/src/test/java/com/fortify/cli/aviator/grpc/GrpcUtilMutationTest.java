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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.fortify.aviator.application.ApplicationServiceGrpc;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.util.Constants;
import com.google.protobuf.Any;
import com.google.rpc.ErrorInfo;
import com.google.rpc.RetryInfo;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.protobuf.StatusProto;

/** Retry and failure semantics of state-changing calls, without a server. */
class GrpcUtilMutationTest {
    private static final String OPERATION_ID = "0194a7c0-0000-7000-8000-000000000001";

    private final List<Long> sleeps = new ArrayList<>();
    private final AtomicInteger signings = new AtomicInteger();
    private final AtomicInteger calls = new AtomicInteger();

    private String execute(String operationId, GrpcUtil.GrpcCall<ApplicationServiceGrpc.ApplicationServiceBlockingStub, String, String> call) {
        return executeFor("test mutation", operationId, call);
    }

    private String executeFor(String operation, String operationId,
            GrpcUtil.GrpcCall<ApplicationServiceGrpc.ApplicationServiceBlockingStub, String, String> call) {
        var channel = NettyChannelBuilder.forAddress("127.0.0.1", 1).usePlaintext().build();
        try {
            var stub = ApplicationServiceGrpc.newBlockingStub(channel);
            return GrpcUtil.executeMutationCall(stub, (s, request) -> {
                calls.incrementAndGet();
                return call.call(s, request);
            }, () -> "signed-attempt-" + signings.incrementAndGet(), operation, operationId, sleeps::add);
        } finally {
            channel.shutdownNow();
        }
    }

    private static StatusRuntimeException operationFailure(Status.Code code, String reason, String message, RetryInfo retry) {
        var status = com.google.rpc.Status.newBuilder().setCode(code.value()).setMessage(message)
            .addDetails(Any.pack(ErrorInfo.newBuilder().setDomain("aviator.operation").setReason(reason).build()));
        if (retry != null) {
            status.addDetails(Any.pack(retry));
        }
        return StatusProto.toStatusRuntimeException(status.build());
    }

    @Test
    void unknownOutcomeIsRetriedWithTheSameOperationIdAndEveryAttemptIsReSigned() {
        var result = execute(OPERATION_ID, (stub, request) -> {
            if (calls.get() < 3) {
                throw Status.DEADLINE_EXCEEDED.asRuntimeException();
            }
            return "ok:" + request;
        });

        assertEquals("ok:signed-attempt-3", result);
        assertEquals(3, calls.get());
        assertEquals(3, signings.get(), "every attempt must be signed again with a fresh timestamp");
        assertEquals(2, sleeps.size());
    }

    @Test
    void abortedWithOperationInProgressIsRetriedHonoringTheServerDelay() {
        var retry = RetryInfo.newBuilder().setRetryDelay(com.google.protobuf.Duration.newBuilder().setSeconds(1)).build();

        var result = execute(OPERATION_ID, (stub, request) -> {
            if (calls.get() == 1) {
                throw operationFailure(Status.Code.ABORTED, "OPERATION_IN_PROGRESS", "still processing", retry);
            }
            return "done";
        });

        assertEquals("done", result);
        assertEquals(1, sleeps.size());
        assertTrue(sleeps.get(0) >= 1000 && sleeps.get(0) < 1200, "delay " + sleeps.get(0));
    }

    @Test
    void mutationsAreNeverRetriedWithoutAnOperationIdBecauseARetryCouldApplyThemTwice() {
        assertThrows(AviatorTechnicalException.class, () -> execute(null, (stub, request) -> {
            throw Status.UNAVAILABLE.asRuntimeException();
        }));

        assertEquals(1, calls.get());
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void exhaustedRetriesTellTheUserHowToRetrySafely() {
        var failure = assertThrows(AviatorTechnicalException.class, () -> execute(OPERATION_ID, (stub, request) -> {
            throw Status.UNAVAILABLE.asRuntimeException();
        }));

        assertEquals(GrpcUtil.MAX_MUTATION_ATTEMPTS, calls.get());
        assertTrue(failure.getMessage().contains("--operation-id " + OPERATION_ID), failure.getMessage());
        assertTrue(failure.getMessage().contains("outcome of this operation is unknown"), failure.getMessage());
        assertTrue(failure.getMessage().contains("Do not start a new operation"), failure.getMessage());
    }

    @Test
    void technicalFailuresAlsoCarryTheRetryGuidance() {
        var failure = assertThrows(AviatorTechnicalException.class, () -> execute(OPERATION_ID, (stub, request) -> {
            throw Status.INTERNAL.withDescription("boom").asRuntimeException();
        }));

        assertEquals(1, calls.get(), "INTERNAL is not retried automatically");
        assertTrue(failure.getMessage().contains("--operation-id " + OPERATION_ID), failure.getMessage());
    }

    @Test
    void definitiveFailuresAreNotRetriedAndCarryNoUnknownOutcomeHint() {
        for (Status status : new Status[] {Status.NOT_FOUND, Status.FAILED_PRECONDITION, Status.PERMISSION_DENIED,
                Status.INVALID_ARGUMENT, Status.ALREADY_EXISTS}) {
            calls.set(0);
            var failure = assertThrows(AviatorSimpleException.class, () -> execute(OPERATION_ID, (stub, request) -> {
                throw status.withDescription("denied").asRuntimeException();
            }));

            assertEquals(1, calls.get(), status.getCode().name());
            assertFalse(failure.getMessage().contains("unknown"), failure.getMessage());
        }
    }

    @Test
    void operationReasonsAreTranslatedForTheUser() {
        var expired = assertThrows(AviatorSimpleException.class, () -> execute(OPERATION_ID, (stub, request) -> {
            throw operationFailure(Status.Code.FAILED_PRECONDITION, "OPERATION_ID_EXPIRED", "too old", null);
        }));
        assertTrue(expired.getMessage().contains("older than 24 hours"), expired.getMessage());
        assertTrue(expired.getMessage().contains("Check the current state"), expired.getMessage());

        var reused = assertThrows(AviatorSimpleException.class, () -> execute(OPERATION_ID, (stub, request) -> {
            throw operationFailure(Status.Code.INVALID_ARGUMENT, "OPERATION_ID_REUSED", "reused", null);
        }));
        assertTrue(reused.getMessage().contains("different request"), reused.getMessage());

        var completed = assertThrows(AviatorSimpleException.class, () -> execute(OPERATION_ID, (stub, request) -> {
            throw operationFailure(Status.Code.ALREADY_EXISTS, "OPERATION_ALREADY_COMPLETED",
                "token already created and cannot be shown again", null);
        }));
        assertEquals("token already created and cannot be shown again", completed.getMessage());

        var mismatch = assertThrows(AviatorSimpleException.class, () -> execute(OPERATION_ID, (stub, request) -> {
            throw operationFailure(Status.Code.PERMISSION_DENIED, "OPERATION_BINDING_MISMATCH", "binding", null);
        }));
        assertTrue(mismatch.getMessage().contains("compatible versions"), mismatch.getMessage());
    }

    @Test
    void tokenCreationNeverPromisesThatTheOriginalTokenIsReturned() {
        var unknown = assertThrows(RuntimeException.class, () -> executeFor(Constants.OP_GENERATE_TOKEN, OPERATION_ID,
            (stub, request) -> { throw Status.DEADLINE_EXCEEDED.asRuntimeException(); }));
        assertTrue(unknown.getMessage().contains("cannot be shown again"), unknown.getMessage());
        assertTrue(unknown.getMessage().contains("--operation-id " + OPERATION_ID), unknown.getMessage());
        assertFalse(unknown.getMessage().contains("returns the original result"), unknown.getMessage());

        var completed = assertThrows(AviatorSimpleException.class, () -> executeFor(Constants.OP_GENERATE_TOKEN, OPERATION_ID,
            (stub, request) -> { throw operationFailure(Status.Code.ALREADY_EXISTS, "OPERATION_ALREADY_COMPLETED", "", null); }));
        assertTrue(completed.getMessage().contains("fcli aviator token list"), completed.getMessage());
        assertTrue(completed.getMessage().contains("revoke"), completed.getMessage());
    }

    @Test
    void alreadyCompletedFallsBackToAFixedMessageWhenTheServerSendsNoDescription() {
        var completed = assertThrows(AviatorSimpleException.class, () -> execute(OPERATION_ID,
            (stub, request) -> { throw operationFailure(Status.Code.ALREADY_EXISTS, "OPERATION_ALREADY_COMPLETED", "", null); }));

        assertTrue(completed.getMessage().contains("already completed"), completed.getMessage());
        assertTrue(completed.getMessage().contains("test mutation"), completed.getMessage());
    }

    @Test
    void stillInProgressAfterAllAttemptsAsksTheUserToWaitAndReuseTheId() {
        var failure = assertThrows(AviatorSimpleException.class, () -> execute(OPERATION_ID, (stub, request) -> {
            throw operationFailure(Status.Code.ABORTED, "OPERATION_IN_PROGRESS", "busy", null);
        }));

        assertEquals(GrpcUtil.MAX_MUTATION_ATTEMPTS, calls.get());
        assertTrue(failure.getMessage().contains("still being processed"), failure.getMessage());
        assertTrue(failure.getMessage().contains("--operation-id " + OPERATION_ID), failure.getMessage());
    }
}
