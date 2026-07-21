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

import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;

import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;

public interface AviatorDiagnosticProbe {
    InetAddress[] resolve(String host) throws IOException;
    void connect(String host, int port, int timeoutSeconds) throws IOException;
    AviatorTlsHandshakeResult handshake(AviatorConnectionPlan connectionPlan, int timeoutSeconds) throws IOException;
    AviatorGrpcReachabilityResult probeGrpc(String url, int timeoutSeconds) throws Exception;

    default Optional<String> getProxySource() {
        return Optional.empty();
    }
}