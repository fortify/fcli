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
 * Provides GitHub credentials for Git operations.
 * Retrieves GitHub token from GITHUB_TOKEN or GH_TOKEN environment variables.
 * 
 * @author Sangamesh Vijayakumar
 */
@Reflectable
public class ActionGitHubCredentialsProvider implements IActionCredentialsProvider {
    
    @Override
    public CredentialsProvider getCredentialsProvider() {
        String token = getGitHubToken();
        if (token == null) {
            return null;
        }
        // GitHub uses "x-access-token" as username with token as password
        return new UsernamePasswordCredentialsProvider("x-access-token", token);
    }
    
    @Override
    public String getCiSystemType() {
        return "github";
    }
    
    @Override
    public boolean isAvailable() {
        return getGitHubToken() != null;
    }
    
    private String getGitHubToken() {
        // Check GITHUB_TOKEN first (standard GitHub Actions variable)
        String token = EnvHelper.env("GITHUB_TOKEN");
        if (token != null && !token.isBlank()) {
            return token;
        }
        // Fall back to GH_TOKEN (GitHub CLI standard variable)
        token = EnvHelper.env("GH_TOKEN");
        if (token != null && !token.isBlank()) {
            return token;
        }
        return null;
    }
}
