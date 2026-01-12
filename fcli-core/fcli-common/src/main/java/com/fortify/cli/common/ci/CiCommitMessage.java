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
 * Commit message information providing both short (first line) and full message.
 * Primarily used by local Git repositories, typically not available in CI systems
 * as this metadata is not exposed in standard CI environment variables.
 * 
 * <p>Structure matches the output of {@code localRepo()} SpEL function:
 * <pre>
 * {
 *   "short": "Fix bug in parser",
 *   "full": "Fix bug in parser\n\nThis commit fixes issue #123..."
 * }
 * </pre>
 * 
 * @param short_ First line of commit message
 * @param full Full commit message including body
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record CiCommitMessage(
    @JsonProperty("short") String short_,
    String full
) {}
