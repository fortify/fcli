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
package com.fortify.cli.aviator.diagnose.support;

import java.io.IOException;
import java.net.InetAddress;

import com.fortify.cli.aviator.diagnose.AviatorGrpcReachabilityResult;
import com.fortify.cli.aviator.diagnose.AviatorTunnelResult;
import com.fortify.cli.aviator.diagnose.IAviatorDiagnosticProbe;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;

/**
 * Shared offline probe for diagnose unit tests. Defaults to all-transport-pass
 * (DNS, TCP, TLS/h2, gRPC response). Mutate fields to force failures.
 */
public final class ConfigurableDiagnosticProbe implements IAviatorDiagnosticProbe {
    public IOException resolveException;
    public IOException connectException;
    public AviatorTunnelResult tunnelResult = new AviatorTunnelResult.TlsSucceeded(false, "not-used",
        "TLSv1.3", "TLS_AES_128_GCM_SHA256", "CN=aviator.invalid", "h2");
    public AviatorGrpcReachabilityResult grpcResult =
        AviatorGrpcReachabilityResult.responseReceived("OK", "response received");
    public boolean tunnelCalled;
    public int tunnelCallCount;
    public boolean grpcCalled;

    public ConfigurableDiagnosticProbe() {}

    public ConfigurableDiagnosticProbe(AviatorGrpcReachabilityResult grpcResult) {
        this.grpcResult = grpcResult;
    }

    @Override
    public InetAddress[] resolve(String host) throws IOException {
        if (resolveException != null) {
            throw resolveException;
        }
        return new InetAddress[] {InetAddress.getByName("127.0.0.1")};
    }

    @Override
    public void connect(String host, int port, int timeoutSeconds) throws IOException {
        if (connectException != null) {
            throw connectException;
        }
    }

    @Override
    public AviatorTunnelResult probeTunnel(AviatorConnectionPlan connectionPlan, int timeoutSeconds) {
        tunnelCalled = true;
        tunnelCallCount++;
        return tunnelResult;
    }

    @Override
    public AviatorGrpcReachabilityResult probeGrpc(String url, int timeoutSeconds) {
        grpcCalled = true;
        return grpcResult;
    }
}
