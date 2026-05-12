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

/**
 * Represents a confirmed SAST–DAST correlation pair.
 * Used as the output of the gRPC correlation stream and as input
 * to the ExternalFindings injection step.
 *
 * @param sastInstanceId SAST finding instance ID (from FVDL InstanceID)
 * @param dastIssueId    DAST issue ID (from webinspect.xml Issue@id)
 * @param scanGuid       SAST scan UUID (FVDL document UUID, used as OriginID)
 * @param confidence     Confidence level of the correlation (e.g. HIGH, MEDIUM, LOW)
 * @param rationale      Server-provided explanation for the correlation decision
 */
public record CorrelatedPair(
    String sastInstanceId,
    String dastIssueId,
    String scanGuid,
    String confidence,
    String rationale
) {}
