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
 * Branch information providing both full ref and short name.
 * Used by both local Git repositories and CI systems.
 * 
 * <p>Structure matches the output of {@code localRepo()} SpEL function:
 * <pre>
 * {
 *   "full": "refs/heads/main",
 *   "short": "main"
 * }
 * </pre>
 * 
 * <p>For CI systems in pull request context:
 * <ul>
 *   <li>{@code short} typically contains the source branch name</li>
 *   <li>{@code full} may contain the full ref (e.g., "refs/pull/123/merge")</li>
 * </ul>
 * 
 * @param full Full git reference (e.g., "refs/heads/main", "refs/pull/123/merge")
 * @param short_ Short branch name (e.g., "main", "feature/my-feature")
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record CiBranch(
    String full,
    @JsonProperty("short") String short_
) {}
