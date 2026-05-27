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
package com.fortify.cli.common.ci.bitbucket;

import com.formkiq.graalvm.annotations.Reflectable;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Bitbucket REST API helper factory providing fluent API for repository operations.
 * This class can be used from commands, actions, and other modules.
 * 
 * <p>Usage examples:
 * <pre>{@code
 * // Repository-scoped operations
 * var repo = restHelper.repository("workspace", "repo-slug");
 * repo.upsertCommitReport(commitSha, reportId, reportContent);
 * repo.addReportAnnotations(commitSha, reportId, annotationsContent);
 * }</pre>
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class BitbucketRestHelper {
    private final BitbucketUnirestInstanceSupplier unirestInstanceSupplier;

    /**
     * Create a repository-scoped API client for the specified repository.
     * 
     * @param workspace Workspace name
     * @param repoSlug Repository slug
     * @return Repository-scoped client
     */
    public BitbucketRepository repository(String workspace, String repoSlug) {
        return new BitbucketRepository(getUnirest(), workspace, repoSlug);
    }

    /**
     * Get the UnirestInstance from the supplier.
     */
    private UnirestInstance getUnirest() {
        return unirestInstanceSupplier.getUnirestInstance();
    }
}
