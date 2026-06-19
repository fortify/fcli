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
 * Provides Azure DevOps credentials for Git operations.
 * Retrieves Azure DevOps token from SYSTEM_ACCESSTOKEN environment variable (set by ADO Pipeline).
 * 
 * @author Sangamesh Vijayakumar
 */
@Reflectable
public class ActionAdoCredentialsProvider implements IActionCredentialsProvider {
    
    @Override
    public CredentialsProvider getCredentialsProvider() {
        String token = getAdoToken();
        if (token == null) {
            return null;
        }
        // Azure DevOps uses empty string as username with token as password
        return new UsernamePasswordCredentialsProvider("", token);
    }
    
    @Override
    public String getCiSystemType() {
        return "ado";
    }
    
    @Override
    public boolean isAvailable() {
        return getAdoToken() != null;
    }
    
    private String getAdoToken() {
        String token = EnvHelper.env("SYSTEM_ACCESSTOKEN");
        if (token != null && !token.isBlank()) {
            return token;
        }
        return null;
    }
}
