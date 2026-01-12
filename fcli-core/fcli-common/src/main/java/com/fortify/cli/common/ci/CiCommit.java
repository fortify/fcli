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
 * Commit information including ID, message, author, and committer.
 * Provides a consistent structure across local Git repositories and CI systems.
 * 
 * <p>Structure matches the output of {@code localRepo()} SpEL function:
 * <pre>
 * {
 *   "id": {
 *     "full": "abc123...",
 *     "short": "abc123"
 *   },
 *   "message": {
 *     "short": "First line",
 *     "full": "Full message"
 *   },
 *   "author": {
 *     "name": "John Doe",
 *     "email": "john@example.com",
 *     "when": "2026-01-12T10:00:00Z"
 *   },
 *   "committer": {
 *     "name": "Jane Smith",
 *     "email": "jane@example.com",
 *     "when": "2026-01-12T11:00:00Z"
 *   }
 * }
 * </pre>
 * 
 * <p><b>Note:</b> CI systems typically only provide commit ID ({@code id.full} and {@code id.short}).
 * Message, author, and committer are usually only available from local Git repositories.
 * 
 * @param id Commit ID (full and short SHA)
 * @param message Commit message (may be null in CI systems)
 * @param author Commit author (may be null in CI systems)
 * @param committer Commit committer (may be null in CI systems)
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record CiCommit(
    CiCommitId id,
    CiCommitMessage message,
    CiPerson author,
    CiPerson committer
) {}
