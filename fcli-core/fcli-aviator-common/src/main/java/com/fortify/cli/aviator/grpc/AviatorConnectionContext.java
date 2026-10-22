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

import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;

import io.grpc.CallOptions;

/**
 * Endpoints retained for error messages, without proxy credentials.
 * @param proxy null for a direct connection
 */
record AviatorConnectionContext(AviatorEndpoint target, AviatorEndpoint proxy) {
    // Present only on fail-fast unary stubs; absence preserves the existing error mapping.
    static final CallOptions.Key<AviatorConnectionContext> CALL_OPTION = CallOptions.Key.create("aviator-connectivity");

    AviatorConnectionContext {
        Objects.requireNonNull(target, "target");
    }

    static AviatorConnectionContext of(AviatorConnectionPlan plan) {
        var target = new AviatorEndpoint(plan.target().host(), plan.effectivePort());
        var proxy = plan.proxyDescriptor()
            .map(d -> new AviatorEndpoint(d.getProxyHost(), d.getProxyPort()))
            .orElse(null);
        return new AviatorConnectionContext(target, proxy);
    }

    boolean isProxied() {
        return proxy!=null;
    }

    AviatorEndpoint firstHop() {
        return isProxied() ? proxy : target;
    }
}
