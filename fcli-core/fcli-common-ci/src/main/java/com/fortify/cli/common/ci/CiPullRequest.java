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
 * Immutable record holding pull request / merge request information.
 * Unified structure for GitHub Pull Requests, GitLab Merge Requests, and Azure DevOps Pull Requests.
 * 
 * <p>This record provides CI-agnostic representation of code review requests. All CI systems
 * provide similar information:
 * <ul>
 *   <li><b>active</b> - indicates if running in PR/MR context</li>
 *   <li><b>id</b> - numeric identifier (GitHub: pull request number, GitLab: merge request IID, ADO: pull request ID)</li>
 *   <li><b>target</b> - destination branch for the changes</li>
 * </ul>
 * 
 * @param active whether running in pull request/merge request context
 * @param id numeric identifier for the pull/merge request (null if not active)
 * @param target target branch name (null if not active)
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record CiPullRequest(
    boolean active,
    String id,
    String target
) {
    /**
     * Create an inactive pull request instance (not in PR/MR context).
     */
    public static CiPullRequest inactive() {
        return CiPullRequest.builder()
            .active(false)
            .id(null)
            .target(null)
            .build();
    }
    
    /**
     * Create an active pull request instance.
     */
    public static CiPullRequest active(String id, String target) {
        return CiPullRequest.builder()
            .active(true)
            .id(id)
            .target(target)
            .build();
    }
}
