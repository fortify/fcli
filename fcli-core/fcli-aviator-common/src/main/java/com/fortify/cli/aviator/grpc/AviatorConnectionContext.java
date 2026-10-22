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

/**
 * Safe summary of the route that an Aviator client uses. It holds only host names and
 * ports; it never holds URLs, proxy credentials, or proxy descriptors.
 *
 * @param target the Aviator server endpoint
 * @param proxy the HTTP CONNECT proxy endpoint, or null for a direct connection
 */
record AviatorConnectionContext(AviatorEndpoint target, AviatorEndpoint proxy) {
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

    /** Returns the first TCP peer of the client: the proxy if one is used, else the target. */
    AviatorEndpoint firstHop() {
        return isProxied() ? proxy : target;
    }

    String route() {
        return isProxied() ? "proxy" : "direct";
    }
}
