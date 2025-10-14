/*
 * Copyright 2021-2025 Open Text.
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

class RequestMetrics {
    private final long startTime;
    private volatile long endTime = 0;
    private volatile String status = "PENDING";

    public RequestMetrics() {
        this.startTime = System.currentTimeMillis();
    }

    public void complete(String status) {
        this.endTime = System.currentTimeMillis();
        this.status = status;
    }

    public long getDuration() {
        return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
    }
}