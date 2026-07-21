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

public record AviatorGrpcReachabilityResult(
        boolean responseReceived,
        boolean httpResponseReceived,
        String statusCode,
        String failureCategory,
        String httpStatusCode,
        String httpContentType,
        String description) {
    public AviatorGrpcReachabilityResult(boolean responseReceived, String statusCode, String description) {
        this(responseReceived, false, statusCode, null, null, null, description);
    }
}