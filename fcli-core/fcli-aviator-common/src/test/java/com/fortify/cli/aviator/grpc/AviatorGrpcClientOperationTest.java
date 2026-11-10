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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fortify.aviator.application.ApplicationById;
import com.fortify.aviator.application.ApplicationMultiplierOperationResponse;
import com.fortify.aviator.application.ApplicationServiceGrpc;
import com.fortify.aviator.application.ServerCapabilities;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.google.protobuf.Empty;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;

/**
 * End-to-end client behavior against a server that implements the replay protocol contract (capabilities,
 * method-bound signed messages, at-most-once execution per operation id).
 */
class AviatorGrpcClientOperationTest {
    private static final String REDUCE = ApplicationServiceGrpc.getReduceEntitlementMethod().getFullMethodName();

    private Server server;
    private ManagedChannel channel;

    @AfterEach
    void tearDown() throws Exception {
        if (channel != null) { channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS); }
        if (server != null) { server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS); }
    }

    private AviatorGrpcClient client(ApplicationServiceGrpc.ApplicationServiceImplBase service) throws Exception {
        server = NettyServerBuilder.forAddress(new InetSocketAddress("127.0.0.1", 0)).addService(service).build().start();
        channel = NettyChannelBuilder.forAddress("127.0.0.1", server.getPort()).proxyDetector(address -> null).usePlaintext().build();
        return new AviatorGrpcClient(channel, 30, null, 30);
    }

    /** Records every call and executes a reduction at most once per operation id, like the real server. */
    private static final class DedupingServer extends ApplicationServiceGrpc.ApplicationServiceImplBase {
        final AtomicInteger multiplier = new AtomicInteger(3);
        final AtomicInteger failuresAfterCommit;
        final Map<String, ApplicationMultiplierOperationResponse> committed = new ConcurrentHashMap<>();
        final List<ApplicationById> requests = new ArrayList<>();
        final boolean supportsCapabilities;
        final AtomicInteger capabilityCalls = new AtomicInteger();

        DedupingServer(boolean supportsCapabilities, int failuresAfterCommit) {
            this.supportsCapabilities = supportsCapabilities;
            this.failuresAfterCommit = new AtomicInteger(failuresAfterCommit);
        }

        @Override
        public void getServerCapabilities(Empty request, StreamObserver<ServerCapabilities> observer) {
            capabilityCalls.incrementAndGet();
            if (!supportsCapabilities) {
                observer.onError(Status.UNIMPLEMENTED.asRuntimeException());
                return;
            }
            observer.onNext(ServerCapabilities.newBuilder().setReplayProtocolVersion(1).setOperationIdMaxAgeSeconds(24 * 3600)
                .setMessageMaxAgeSeconds(300).build());
            observer.onCompleted();
        }

        @Override
        public void reduceEntitlement(ApplicationById request, StreamObserver<ApplicationMultiplierOperationResponse> observer) {
            synchronized (requests) {
                requests.add(request);
            }
            ApplicationMultiplierOperationResponse response;
            if (request.getOperationId().isEmpty()) {
                int previous = multiplier.getAndDecrement();
                response = ApplicationMultiplierOperationResponse.newBuilder().setPreviousMultiplier(previous).setNewMultiplier(previous - 1).build();
            } else {
                var existing = committed.get(request.getOperationId());
                if (existing != null) {
                    response = existing.toBuilder().setMessage("replayed").build();
                } else {
                    int previous = multiplier.getAndDecrement();
                    response = ApplicationMultiplierOperationResponse.newBuilder().setPreviousMultiplier(previous).setNewMultiplier(previous - 1).build();
                    committed.put(request.getOperationId(), response);
                }
            }
            if (failuresAfterCommit.getAndDecrement() > 0) {
                // the change committed, but the client never sees the response
                observer.onError(Status.UNAVAILABLE.asRuntimeException());
                return;
            }
            observer.onNext(response);
            observer.onCompleted();
        }
    }

    private static AviatorRequestSigner signer(List<String> messages) {
        return (method, operationId) -> {
            String timestamp = Instant.now().toString();
            String message = operationId == null ? "tenant;101;reduce-entitlement;" + timestamp
                : "tenant;101;reduce-entitlement;" + method + ";" + operationId + ";" + timestamp;
            messages.add(message);
            return new AviatorRequestSigner.SignedRequest(message, "signature-" + messages.size());
        };
    }

    @Test
    void retryAfterUnknownOutcomeReusesTheOperationIdAndTheServerAppliesItOnce() throws Exception {
        var service = new DedupingServer(true, 2);
        var messages = new ArrayList<String>();
        try (var client = client(service)) {
            var response = client.reduceEntitlement("101", "tenant", null, signer(messages));

            assertEquals(3, response.getPreviousMultiplier());
            assertEquals(2, response.getNewMultiplier());
        }

        assertEquals(2, service.multiplier.get(), "the reduction must be applied exactly once");
        assertEquals(3, service.requests.size());
        String operationId = service.requests.get(0).getOperationId();
        assertTrue(AviatorOperationId.isValid(operationId));
        service.requests.forEach(request -> assertEquals(operationId, request.getOperationId()));
        // each attempt is signed again and the signed message binds method and operation id
        assertEquals(3, messages.size());
        messages.forEach(message -> assertTrue(message.contains(";" + REDUCE + ";" + operationId + ";"), message));
        assertEquals(3, service.requests.stream().map(ApplicationById::getSignature).distinct().count());
    }

    @Test
    void userSuppliedOperationIdFromAnEarlierRunIsHonored() throws Exception {
        var service = new DedupingServer(true, 0);
        String earlierRun = AviatorOperationId.generate();
        try (var client = client(service)) {
            client.reduceEntitlement("101", "tenant", earlierRun, signer(new ArrayList<>()));
            var retry = client.reduceEntitlement("101", "tenant", earlierRun, signer(new ArrayList<>()));

            assertEquals("replayed", retry.getMessage());
        }
        assertEquals(2, service.multiplier.get());
        assertEquals(earlierRun, service.requests.get(1).getOperationId());
    }

    @Test
    void capabilitiesAreProbedOncePerClient() throws Exception {
        var service = new DedupingServer(true, 0);
        try (var client = client(service)) {
            client.reduceEntitlement("101", "tenant", null, signer(new ArrayList<>()));
            client.reduceEntitlement("101", "tenant", null, signer(new ArrayList<>()));
        }
        assertEquals(1, service.capabilityCalls.get());
    }

    @Test
    void legacyServerGetsTheLegacyMessageAndIsNeverRetried() throws Exception {
        var service = new DedupingServer(false, 1);
        var messages = new ArrayList<String>();
        try (var client = client(service)) {
            assertThrows(AviatorTechnicalException.class, () -> client.reduceEntitlement("101", "tenant", null, signer(messages)));
        }

        assertEquals(1, service.requests.size(), "without server-side deduplication a retry could apply the change twice");
        assertEquals("", service.requests.get(0).getOperationId());
        assertFalse(messages.get(0).contains(REDUCE), messages.get(0));
    }

    @Test
    void explicitOperationIdIsRefusedWhenTheServerCannotHonorIt() throws Exception {
        var service = new DedupingServer(false, 0);
        try (var client = client(service)) {
            var failure = assertThrows(AviatorSimpleException.class,
                () -> client.reduceEntitlement("101", "tenant", AviatorOperationId.generate(), signer(new ArrayList<>())));

            assertTrue(failure.getMessage().contains("does not support operation IDs"), failure.getMessage());
        }
        assertTrue(service.requests.isEmpty());
    }

    @Test
    void malformedOrExpiredOperationIdsNeverReachTheServer() throws Exception {
        var service = new DedupingServer(true, 0);
        try (var client = client(service)) {
            var malformed = assertThrows(AviatorSimpleException.class,
                () -> client.reduceEntitlement("101", "tenant", "not-an-id", signer(new ArrayList<>())));
            assertTrue(malformed.getMessage().contains("Invalid --operation-id"), malformed.getMessage());

            String old = AviatorOperationId.generate(java.time.Clock.offset(java.time.Clock.systemUTC(), java.time.Duration.ofHours(-30)));
            var expired = assertThrows(AviatorSimpleException.class,
                () -> client.reduceEntitlement("101", "tenant", old, signer(new ArrayList<>())));
            assertTrue(expired.getMessage().contains("can no longer be deduplicated"), expired.getMessage());
        }
        assertTrue(service.requests.isEmpty());
        assertNotEquals(0, service.capabilityCalls.get());
    }
}
