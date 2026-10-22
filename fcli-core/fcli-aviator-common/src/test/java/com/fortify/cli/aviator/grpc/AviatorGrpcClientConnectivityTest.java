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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.fortify.aviator.application.ApplicationByTenantName;
import com.fortify.aviator.application.ApplicationList;
import com.fortify.aviator.application.ApplicationServiceGrpc;
import com.fortify.aviator.dastentitlement.DastEntitlementServiceGrpc;
import com.fortify.aviator.dastentitlement.ListDastEntitlementsByTenantRequest;
import com.fortify.aviator.dastentitlement.ListDastEntitlementsByTenantResponse;
import com.fortify.aviator.entitlement.EntitlementServiceGrpc;
import com.fortify.aviator.entitlement.ListEntitlementsByTenantRequest;
import com.fortify.aviator.entitlement.ListEntitlementsByTenantResponse;
import com.fortify.grpc.token.ListTokensRequest;
import com.fortify.grpc.token.ListTokensResponse;
import com.fortify.grpc.token.TokenServiceGrpc;
import com.fortify.cli.aviator._common.exception.AviatorConnectivityException;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.ParsedTarget;
import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Checks connectivity behavior against real Netty transports on the loopback interface.
 */
class AviatorGrpcClientConnectivityTest {
    private static final Duration FAIL_FAST_LIMIT = Duration.ofSeconds(5);
    private static final String PROXY_SECRET = "sentinel-proxy-secret";
    private static final String TARGET_HOST = "aviator.example.invalid";

    private ServerSocket proxyServer;
    private Server grpcServer;
    private ManagedChannel plaintextChannel;

    @AfterEach
    void tearDown() throws Exception {
        if ( proxyServer!=null ) { proxyServer.close(); }
        if ( plaintextChannel!=null ) { plaintextChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS); }
        if ( grpcServer!=null ) { grpcServer.shutdownNow().awaitTermination(5, TimeUnit.SECONDS); }
    }

    @Test
    void directRefusalFailsFastWithDirectRefusedMessage() throws Exception {
        int port = unusedPort();
        var plan = new AviatorConnectionPlan("127.0.0.1:" + port, "https://127.0.0.1:" + port,
            new ParsedTarget("127.0.0.1", port), port, Optional.empty());

        var exception = callAndExpectConnectivityFailure(plan);

        assertEquals("DIRECT_REFUSED", exception.getKind(),
            () -> ExceptionUtils.getRootCause(exception).toString());
        assertTrue(exception.getMessage().startsWith("Connection to Aviator server 127.0.0.1:" + port + " was refused"));
    }

    @Test
    void proxyRefusalFailsFastWithProxyRefusedMessage() throws Exception {
        int proxyPort = unusedPort();

        var exception = callAndExpectConnectivityFailure(proxiedPlan(proxyPort));

        assertEquals("PROXY_REFUSED", exception.getKind());
        assertEquals("Connection to proxy 127.0.0.1:" + proxyPort + " was refused\n"
                + "Check that the proxy is running and that its host and port are correct", exception.getMessage());
        assertFalse(exception.getSafeDiagnosticString().contains(PROXY_SECRET));
    }

    @Test
    void proxyAuthenticationRejectionIsReportedAsProxyRejection() throws Exception {
        proxyServer = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        startRejectingProxy(proxyServer);

        var exception = callAndExpectConnectivityFailure(proxiedPlan(proxyServer.getLocalPort()));

        assertEquals("PROXY_REJECTED", exception.getKind());
        assertFalse(exception.getMessage().contains("sentinel-realm"));
        assertFalse(exception.getSafeDiagnosticString().contains("sentinel-realm"));
        assertFalse(exception.getSafeDiagnosticString().contains(PROXY_SECRET));
    }

    @Test
    @Timeout(15)
    void pendingTcpConnectUsesNettyTimeoutBeforeRequestDeadline() throws Exception {
        var eventLoop = new NioEventLoopGroup(1);
        var builder = NettyChannelBuilder.forAddress("127.0.0.1", 443).usePlaintext()
            .eventLoopGroup(eventLoop).channelFactory(PendingConnectChannel::new);
        plaintextChannel = AviatorRequestPolicy.FAIL_FAST.configureChannel(builder).build();
        var context = new AviatorConnectionContext(new AviatorEndpoint("127.0.0.1", 443), null);
        long started = System.nanoTime();
        try ( var client = new AviatorGrpcClient(plaintextChannel, 30, null, 30, AviatorRequestPolicy.FAIL_FAST, context) ) {
            var exception = assertThrows(AviatorConnectivityException.class,
                () -> client.listApplication("tenant", "signature", "message"));
            assertEquals("CONNECT_TIMEOUT", exception.getKind());
            assertTrue(exception.getMessage().contains("timed out"));
            assertFalse(exception.getMessage().contains("refused"));
        } finally {
            eventLoop.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }
        var elapsed = Duration.ofNanos(System.nanoTime() - started);
        assertTrue(elapsed.compareTo(Duration.ofSeconds(4)) >= 0, "Connect timer fired too early: " + elapsed);
        assertTrue(elapsed.compareTo(Duration.ofSeconds(10)) < 0, "Connect timer fired too late: " + elapsed);
    }

    /** Simulates a pending TCP connection; Netty itself schedules and raises the timeout. */
    private static final class PendingConnectChannel extends NioSocketChannel {
        @Override
        protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) {
            return false;
        }
    }

    @Test
    @Timeout(20)
    void stalledProxyNegotiationEndsBeforeRequestDeadline() throws Exception {
        proxyServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        // Leave the TCP connection in the listen backlog without answering CONNECT. Netty's real
        // proxy handshake timer must end the call, rather than the RPC deadline.
        var started = System.nanoTime();
        try ( var client = AviatorGrpcClientHelper.createClient(proxiedPlan(proxyServer.getLocalPort()),
                null, 30, AviatorRequestPolicy.FAIL_FAST) ) {
            var exception = assertThrows(AviatorConnectivityException.class,
                () -> client.listApplication("tenant", "signature", "message"));
            assertEquals("PROXY_NEGOTIATION_FAILED", exception.getKind());
            assertFalse(exception.getMessage().contains("refused"));
            assertFalse(exception.getSafeDiagnosticString().contains(PROXY_SECRET));
        }
        assertTrue(Duration.ofNanos(System.nanoTime() - started).compareTo(Duration.ofSeconds(15)) < 0);
    }

    @ParameterizedTest
    @EnumSource(AviatorRequestPolicy.class)
    void healthyUnaryServicesUseSelectedPolicyAndStreamsKeepWaiting(AviatorRequestPolicy policy) throws Exception {
        var captured = new AtomicReference<CallOptions>();
        try ( var client = createPlaintextClient(policy, captured) ) {
            assertTrue(client.listApplication("tenant", "signature", "message").isEmpty());
            assertCallPolicy(policy, captured.get());
            assertEquals(ListTokensResponse.getDefaultInstance(), client.listTokens("email", "tenant", "signature", "message"));
            assertCallPolicy(policy, captured.get());
            assertTrue(client.listEntitlements("tenant", "signature", "message").isEmpty());
            assertCallPolicy(policy, captured.get());
            assertTrue(client.listDastEntitlements("tenant", "signature", "message").isEmpty());
            assertCallPolicy(policy, captured.get());
            assertTrue(client.getCorrelationAsyncStub().getCallOptions().isWaitForReady());
            assertTrue(client.getDastAuditAsyncStub().getCallOptions().isWaitForReady());
            assertNull(client.getCorrelationAsyncStub().getCallOptions().getOption(AviatorConnectionContext.CALL_OPTION));
            assertNull(client.getDastAuditAsyncStub().getCallOptions().getOption(AviatorConnectionContext.CALL_OPTION));
        }
    }

    private static void assertCallPolicy(AviatorRequestPolicy policy, CallOptions options) {
        assertEquals(policy.isWaitForReady(), options.isWaitForReady());
        assertEquals(!policy.isWaitForReady(), options.getOption(AviatorConnectionContext.CALL_OPTION) != null);
        assertDeadlineAtMostDefault(options);
    }

    private static AviatorConnectivityException callAndExpectConnectivityFailure(AviatorConnectionPlan plan) {
        try ( var client = AviatorGrpcClientHelper.createClient(plan, null, 30, AviatorRequestPolicy.FAIL_FAST) ) {
            long start = System.nanoTime();
            var exception = assertThrows(AviatorConnectivityException.class,
                () -> client.listApplication("tenant", "signature", "message"));
            var elapsed = Duration.ofNanos(System.nanoTime() - start);
            assertTrue(elapsed.compareTo(FAIL_FAST_LIMIT) < 0, "Call took " + elapsed);
            return exception;
        }
    }

    private static AviatorConnectionPlan proxiedPlan(int proxyPort) {
        var proxy = ProxyDescriptor.builder()
            .proxyHost("127.0.0.1").proxyPort(proxyPort)
            .proxyUser("sentinel-user").proxyPassword(PROXY_SECRET.toCharArray())
            .build();
        return new AviatorConnectionPlan(TARGET_HOST, "https://" + TARGET_HOST,
            new ParsedTarget(TARGET_HOST, null), 443, Optional.of(proxy));
    }

    private AviatorGrpcClient createPlaintextClient(AviatorRequestPolicy policy, AtomicReference<CallOptions> captured) throws IOException {
        grpcServer = NettyServerBuilder.forAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0))
            .addService(new EmptyApplicationService())
            .addService(new EmptyTokenService())
            .addService(new EmptyEntitlementService())
            .addService(new EmptyDastEntitlementService())
            .build()
            .start();
        plaintextChannel = NettyChannelBuilder.forAddress(InetAddress.getLoopbackAddress().getHostAddress(), grpcServer.getPort())
            .usePlaintext()
            .intercept(new CallOptionsCapture(captured))
            .build();
        var context = new AviatorConnectionContext(new AviatorEndpoint("127.0.0.1", grpcServer.getPort()), null);
        return new AviatorGrpcClient(plaintextChannel, 30, null, 30, policy, context);
    }

    private static void assertDeadlineAtMostDefault(CallOptions callOptions) {
        assertNotNull(callOptions.getDeadline());
        long remainingSeconds = callOptions.getDeadline().timeRemaining(TimeUnit.SECONDS);
        assertTrue(remainingSeconds <= 30 && remainingSeconds > 20, "Deadline remaining: " + remainingSeconds);
    }

    private static int unusedPort() throws IOException {
        try ( var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress()) ) {
            return socket.getLocalPort();
        }
    }

    private static void startRejectingProxy(ServerSocket server) {
        var thread = new Thread(() -> {
            while ( !server.isClosed() ) {
                try ( Socket socket = server.accept() ) {
                    readRequestHeaders(socket.getInputStream());
                    OutputStream out = socket.getOutputStream();
                    out.write(("HTTP/1.1 407 Proxy Authentication Required\r\n"
                            + "Proxy-Authenticate: Basic realm=\"sentinel-realm\"\r\n"
                            + "Content-Length: 0\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                } catch ( IOException e ) {
                    // Server socket closed at test end
                }
            }
        }, "test-rejecting-proxy");
        thread.setDaemon(true);
        thread.start();
    }

    private static void readRequestHeaders(InputStream in) throws IOException {
        int matched = 0;
        byte[] terminator = {'\r', '\n', '\r', '\n'};
        int b;
        while ( matched<terminator.length && (b = in.read())!=-1 ) {
            matched = b==terminator[matched] ? matched+1 : (b=='\r' ? 1 : 0);
        }
    }

    private static final class EmptyApplicationService extends ApplicationServiceGrpc.ApplicationServiceImplBase {
        @Override
        public void listApplications(ApplicationByTenantName request, StreamObserver<ApplicationList> responseObserver) {
            responseObserver.onNext(ApplicationList.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

    private static final class EmptyTokenService extends TokenServiceGrpc.TokenServiceImplBase {
        @Override
        public void listTokens(ListTokensRequest request, StreamObserver<ListTokensResponse> observer) {
            observer.onNext(ListTokensResponse.getDefaultInstance());
            observer.onCompleted();
        }
    }

    private static final class EmptyEntitlementService extends EntitlementServiceGrpc.EntitlementServiceImplBase {
        @Override
        public void listEntitlementsByTenant(ListEntitlementsByTenantRequest request,
                StreamObserver<ListEntitlementsByTenantResponse> observer) {
            observer.onNext(ListEntitlementsByTenantResponse.getDefaultInstance());
            observer.onCompleted();
        }
    }

    private static final class EmptyDastEntitlementService extends DastEntitlementServiceGrpc.DastEntitlementServiceImplBase {
        @Override
        public void listDastEntitlementsByTenant(ListDastEntitlementsByTenantRequest request,
                StreamObserver<ListDastEntitlementsByTenantResponse> observer) {
            observer.onNext(ListDastEntitlementsByTenantResponse.getDefaultInstance());
            observer.onCompleted();
        }
    }

    private static final class CallOptionsCapture implements ClientInterceptor {
        private final AtomicReference<CallOptions> captured;

        private CallOptionsCapture(AtomicReference<CallOptions> captured) {
            this.captured = captured;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions, Channel next) {
            captured.set(callOptions);
            return next.newCall(method, callOptions);
        }
    }
}
