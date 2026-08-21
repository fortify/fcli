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

import org.apache.commons.lang3.StringUtils;

/**
 * Basic authentication credentials for pushing to a git remote.
 *
 * @param username basic-auth username
 * @param token basic-auth token or password
 */
public record CiGitCredentials(String username, String token) {
    public boolean isPresent() {
        return StringUtils.isNotBlank(username) && StringUtils.isNotBlank(token);
    }
}