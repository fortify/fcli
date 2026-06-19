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
package com.fortify.cli.common.action.helper.credential;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.EnvHelper;

/**
 * Provides GitLab credentials for Git operations.
 * Retrieves GitLab token from CI_JOB_TOKEN environment variable (set by GitLab CI/CD).
 * 
 * @author Sangamesh Vijayakumar
 */
@Reflectable
public class ActionGitLabCredentialsProvider implements IActionCredentialsProvider {
    
    @Override
    public CredentialsProvider getCredentialsProvider() {
        String token = getGitLabToken();
        if (token == null) {
            return null;
        }
        // GitLab uses "gitlab-ci-token" as username with token as password
        return new UsernamePasswordCredentialsProvider("gitlab-ci-token", token);
    }
    
    @Override
    public String getCiSystemType() {
        return "gitlab";
    }
    
    @Override
    public boolean isAvailable() {
        return getGitLabToken() != null;
    }
    
    private String getGitLabToken() {
        String token = EnvHelper.env("CI_JOB_TOKEN");
        if (token != null && !token.isBlank()) {
            return token;
        }
        return null;
    }
}
