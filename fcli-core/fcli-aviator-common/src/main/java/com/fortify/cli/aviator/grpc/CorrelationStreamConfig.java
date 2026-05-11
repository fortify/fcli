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
 * Configuration record for initializing a correlation gRPC stream.
 *
 * @param token              Aviator user session token
 * @param applicationName    Aviator application name
 * @param sscApplicationName SSC application name (for metadata)
 * @param sscApplicationVersion SSC application version name
 * @param fprBuildId         SAST FPR build ID
 */
public record CorrelationStreamConfig(
    String token,
    String applicationName,
    String sscApplicationName,
    String sscApplicationVersion,
    String fprBuildId
) {}
