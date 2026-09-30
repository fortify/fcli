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

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;

import lombok.Builder;

/**
 * Configuration for one DAST audit gRPC stream.
 */
@Builder
public record DastAuditStreamConfig(
    String token,
    String applicationName,
    String sscApplicationName,
    String sscApplicationVersion,
    String fprBuildId
) {
    public DastAuditStreamConfig {
        if (token == null || token.isBlank()) {
            throw new AviatorSimpleException("Aviator token must be specified for DAST audit");
        }
        if (applicationName == null || applicationName.isBlank()) {
            throw new AviatorSimpleException("Aviator application name must be specified for DAST audit");
        }
    }
}