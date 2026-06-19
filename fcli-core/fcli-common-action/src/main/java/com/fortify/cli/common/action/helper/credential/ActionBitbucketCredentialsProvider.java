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
 * Provides Bitbucket credentials for Git operations.
 * Retrieves Bitbucket token from BITBUCKET_TOKEN environment variable.
 * 
 * @author Sangamesh Vijayakumar
 */
@Reflectable
public class ActionBitbucketCredentialsProvider implements IActionCredentialsProvider {
    
    @Override
    public CredentialsProvider getCredentialsProvider() {
        String token = getBitbucketToken();
        if (token == null) {
            return null;
        }
        // Bitbucket uses "x-token-auth" as username with token as password
        return new UsernamePasswordCredentialsProvider("x-token-auth", token);
    }
    
    @Override
    public String getCiSystemType() {
        return "bitbucket";
    }
    
    @Override
    public boolean isAvailable() {
        return getBitbucketToken() != null;
    }
    
    private String getBitbucketToken() {
        String token = EnvHelper.env("BITBUCKET_TOKEN");
        if (token != null && !token.isBlank()) {
            return token;
        }
        return null;
    }
}
