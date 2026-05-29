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
 * Repository information combining name, location, and remote URL.
 * Provides a consistent structure across local Git repositories and CI systems.
 * 
 * <p>Structure matches the output of {@code localRepo()} SpEL function:
 * <pre>
 * {
 *   "workspaceDir": "/path/to/repo",
 *   "remoteUrl": "https://github.com/owner/repo.git",
 *   "name": {
 *     "short": "repo",
 *     "full": "owner/repo"
 *   }
 * }
 * </pre>
 * 
 * @param workspaceDir Workspace directory path (repository root) containing source code
 * @param remoteUrl Git remote URL (may be null if not available)
 * @param name Repository name information (short and full)
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record CiRepository(
    String workspaceDir,
    String remoteUrl,
    CiRepositoryName name
) {}
