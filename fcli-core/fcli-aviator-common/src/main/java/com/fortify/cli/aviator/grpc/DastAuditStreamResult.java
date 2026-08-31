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

import java.util.List;

import lombok.Builder;

/**
 * Results and quota metadata returned by one DAST audit stream.
 */
@Builder
public record DastAuditStreamResult(
    List<DastAuditResult> results,
    int reservedQuota,
    int exceededCount,
    boolean unlimitedQuota,
    String quotaLastUpdated,
    String nextQuotaUpdateMessage
) {
    public DastAuditStreamResult {
        results = results == null ? List.of() : List.copyOf(results);
    }
}