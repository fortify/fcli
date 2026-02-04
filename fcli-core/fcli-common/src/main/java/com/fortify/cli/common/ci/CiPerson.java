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

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Builder;

/**
 * Person information (author or committer) from Git commits.
 * Primarily used by local Git repositories, may be null in CI systems
 * as this metadata is typically not available in CI environment variables.
 * 
 * <p>Structure matches the output of {@code localRepo()} SpEL function:
 * <pre>
 * {
 *   "name": "John Doe",
 *   "email": "john@example.com",
 *   "when": "2026-01-12T10:00:00Z"
 * }
 * </pre>
 * 
 * @param name Person's name
 * @param email Person's email address
 * @param when Timestamp in ISO-8601 format
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record CiPerson(
    String name,
    String email,
    String when
) {}
