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
 * Complete local Git repository information returned by {@code localRepo()} SpEL function.
 * Combines repository, branch, and commit data from local Git working tree.
 * 
 * <p>Structure:
 * <pre>
 * {
 *   "repository": {
 *     "workDir": "/path/to/repo",
 *     "remoteUrl": "https://github.com/owner/repo.git",
 *     "name": {
 *       "short": "repo",
 *       "full": "owner/repo"
 *     }
 *   },
 *   "branch": {
 *     "full": "refs/heads/main",
 *     "short": "main"
 *   },
 *   "commit": {
 *     "id": {
 *       "full": "abc123...",
 *       "short": "abc123"
 *     },
 *     "message": {
 *       "short": "First line",
 *       "full": "Full message"
 *     },
 *     "author": {
 *       "name": "John Doe",
 *       "email": "john@example.com",
 *       "when": "2026-01-12T10:00:00Z"
 *     },
 *     "committer": {
 *       "name": "Jane Smith",
 *       "email": "jane@example.com",
 *       "when": "2026-01-12T11:00:00Z"
 *     }
 *   }
 * }
 * </pre>
 * 
 * @param repository Repository information
 * @param branch Branch information (may be null if HEAD is detached)
 * @param commit Commit information (may be null if no commits exist)
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record LocalRepoInfo(
    CiRepository repository,
    CiBranch branch,
    CiCommit commit
) {}
