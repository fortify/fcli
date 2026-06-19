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

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Provides CI system-specific credentials for Git operations.
 * Each implementation handles token retrieval and formatting for its respective CI system.
 * 
 * @author Sangamesh Vijayakumar
 */
@Reflectable
public interface IActionCredentialsProvider {
    /**
     * Get the JGit CredentialsProvider for this CI system.
     * 
     * @return JGit CredentialsProvider configured with appropriate token and username format,
     *         or null if credentials are not available
     */
    CredentialsProvider getCredentialsProvider();
    
    /**
     * Get the CI system type identifier.
     * 
     * @return "github", "gitlab", "ado", or "bitbucket"
     */
    String getCiSystemType();
    
    /**
     * Check if credentials are available for this CI system.
     * 
     * @return true if required environment variable is set, false otherwise
     */
    boolean isAvailable();
}
