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
 * Commit information including IDs, message, author, and committer.
 * Provides a consistent structure across local Git repositories and CI systems.
 * 
 * <p>Distinguishes between head commit (the actual commit on the branch) and merge commit
 * (a synthetic commit created when merging PRs). For most scenarios, both IDs are the same.
 * GitHub PRs are unique: {@code headId} is the actual PR commit, {@code mergeId} is the
 * temporary merge commit created by GitHub for testing the PR merged into the base branch.
 * 
 * <p>Structure matches the output of {@code localRepo()} SpEL function:
 * <pre>
 * {
 *   "headId": {
 *     "full": "abc123...",
 *     "short": "abc123"
 *   },
 *   "mergeId": {
 *     "full": "def456...",
 *     "short": "def456"
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
 * <p><b>Note:</b> CI systems typically only provide commit IDs ({@code headId} and {@code mergeId}).
 * Message, author, and committer are usually only available from local Git repositories. When present,
 * these metadata fields correspond to the head commit.
 * 
 * @param headId Head commit ID - the actual commit on the branch (full and short SHA)
 * @param mergeId Merge commit ID - for GitHub PRs, the merge commit; otherwise same as headId
 * @param message Commit message for the head commit (may be null in CI systems)
 * @param author Commit author for the head commit (may be null in CI systems)
 * @param committer Commit committer for the head commit (may be null in CI systems)
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record CiCommit(
    CiCommitId headId,
    CiCommitId mergeId,
    CiCommitMessage message,
    CiPerson author,
    CiPerson committer
) {}
