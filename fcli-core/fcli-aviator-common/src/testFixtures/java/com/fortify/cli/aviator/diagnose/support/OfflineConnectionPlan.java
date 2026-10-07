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

import java.util.Optional;

import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.ParsedTarget;
import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor;

/**
 * Fixed connection plans that avoid ambient proxy/DNS policy in unit tests.
 */
public final class OfflineConnectionPlan {
    private OfflineConnectionPlan() {}

    public static AviatorConnectionPlan noProxy() {
        return noProxy("aviator.invalid", 443);
    }

    public static AviatorConnectionPlan noProxy(String host, int port) {
        Integer targetPort = port == 443 ? null : port;
        return new AviatorConnectionPlan(host, "https://" + host,
            new ParsedTarget(host, targetPort), port, Optional.empty());
    }

    /** Plan whose URL fields match the diagnose input string (helper offline path). */
    public static AviatorConnectionPlan fixedUrl(String url) {
        return new AviatorConnectionPlan(url, url,
            new ParsedTarget("aviator.invalid", null), 443, Optional.empty());
    }

    public static AviatorConnectionPlan withProxy() {
        var proxy = ProxyDescriptor.builder().proxyHost("proxy.invalid").proxyPort(8080).build();
        return new AviatorConnectionPlan("aviator.invalid", "https://aviator.invalid",
            new ParsedTarget("aviator.invalid", null), 443, Optional.of(proxy));
    }
}
