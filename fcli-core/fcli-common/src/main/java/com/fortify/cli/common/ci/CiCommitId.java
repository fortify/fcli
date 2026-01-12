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
package com.fortify.cli.common.ci;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Builder;

/**
 * Commit ID information providing both full and abbreviated SHA.
 * Used by both local Git repositories and CI systems.
 * 
 * <p>Structure matches the output of {@code localRepo()} SpEL function:
 * <pre>
 * {
 *   "full": "abc123def456...",
 *   "short": "abc123de"
 * }
 * </pre>
 * 
 * @param full Full commit SHA (40 characters for SHA-1, 64 for SHA-256)
 * @param short_ Abbreviated commit SHA (typically 7-8 characters)
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record CiCommitId(
    String full,
    @JsonProperty("short") String short_
) {}
