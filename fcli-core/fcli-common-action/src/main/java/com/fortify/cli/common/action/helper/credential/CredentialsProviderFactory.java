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

import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.transport.CredentialsProvider;

import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Factory for creating and detecting CI system credentials providers.
 * Manages credential provider instantiation and selection based on CI system or availability.
 * 
 * @author Sangamesh Vijayakumar
 */
@Reflectable
public class CredentialsProviderFactory {
    
    // Ordered list of providers - GitHub checked first to maintain backward compatibility
    private static final List<IActionCredentialsProvider> PROVIDERS = Arrays.asList(
        new ActionGitHubCredentialsProvider(),
        new ActionGitLabCredentialsProvider(),
        new ActionAdoCredentialsProvider(),
        new ActionBitbucketCredentialsProvider()
    );
    
    /**
     * Get the credentials provider for a specific CI system.
     * 
     * @param ciSystemType "github", "gitlab", "ado", or "bitbucket"
     * @return IActionCredentialsProvider for the specified system, or null if not found
     */
    public static IActionCredentialsProvider getProvider(String ciSystemType) {
        if (ciSystemType == null) {
            return null;
        }
        
        return PROVIDERS.stream()
            .filter(p -> p.getCiSystemType().equalsIgnoreCase(ciSystemType))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Auto-detect and return the first available credentials provider.
     * Checks providers in order: GitHub, GitLab, ADO, Bitbucket.
     * This maintains backward compatibility where GitHub token takes priority.
     * 
     * @return IActionCredentialsProvider for the first detected CI system,
     *         or null if no credentials are available
     */
    public static IActionCredentialsProvider detectAndGetProvider() {
        return PROVIDERS.stream()
            .filter(IActionCredentialsProvider::isAvailable)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get JGit CredentialsProvider from auto-detected CI system.
     * Convenience method combining detection and credential provider retrieval.
     * 
     * @return JGit CredentialsProvider for the detected CI system,
     *         or null if no credentials are available
     */
    public static CredentialsProvider detectAndGetJGitProvider() {
        IActionCredentialsProvider provider = detectAndGetProvider();
        return provider != null ? provider.getCredentialsProvider() : null;
    }
    
    /**
     * Check if any credentials are available.
     * 
     * @return true if at least one CI system has credentials configured
     */
    public static boolean hasCredentials() {
        return PROVIDERS.stream().anyMatch(IActionCredentialsProvider::isAvailable);
    }
    
    /**
     * Get all registered credential providers.
     * 
     * @return List of all IActionCredentialsProvider implementations
     */
    public static List<IActionCredentialsProvider> getAllProviders() {
        return PROVIDERS;
    }
}
