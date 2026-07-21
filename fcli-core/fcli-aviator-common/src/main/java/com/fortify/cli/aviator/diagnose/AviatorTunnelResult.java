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
 * Outcome of a single TCP(+CONNECT)+TLS probe session. CONNECT and TLS share one socket
 * so diagnostics do not re-open the tunnel between the PROXY and TLS stages.
 */
public sealed interface AviatorTunnelResult {

    boolean proxyConfigured();

    record ProxyConnectFailed(Exception error) implements AviatorTunnelResult {
        @Override
        public boolean proxyConfigured() {
            return true;
        }
    }

    record TlsFailed(
            boolean proxyConfigured,
            String proxyConnectStatus,
            AviatorTlsPhase phase,
            Exception error) implements AviatorTunnelResult {}

    record TlsSucceeded(
            boolean proxyConfigured,
            String proxyConnectStatus,
            String protocol,
            String cipherSuite,
            String peerSubject,
            String applicationProtocol) implements AviatorTunnelResult {}
}
