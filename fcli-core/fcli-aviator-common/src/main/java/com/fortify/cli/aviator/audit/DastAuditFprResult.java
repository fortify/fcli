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
package com.fortify.cli.aviator.audit;

import java.io.File;

import lombok.Builder;

/**
 * Summary of processing one DAST FPR.
 */
@Builder
public record DastAuditFprResult(
    File updatedFile,
    DastAuditFprStatus status,
    String message,
    int totalReported,
    int eligible,
    int submitted,
    int succeeded,
    int truePositives,
    int falsePositivesSuppressed,
    int likelyFalsePositives,
    int skipped,
    int failed,
    int reservedQuota,
    int exceededCount,
    boolean unlimitedQuota,
    String quotaLastUpdated,
    String nextQuotaUpdateMessage
) {}