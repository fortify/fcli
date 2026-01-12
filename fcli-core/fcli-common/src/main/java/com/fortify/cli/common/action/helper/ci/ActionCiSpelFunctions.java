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
package com.fortify.cli.common.action.helper.ci;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;

import lombok.RequiredArgsConstructor;

/**
 * Factory class for CI system helpers, registered as the #ci SpEL variable in actions.
 * Provides factory methods to obtain platform-specific helpers:
 * 
 * <ul>
 *   <li>{@code #ci.github()} - GitHub Actions helper</li>
 *   <li>{@code #ci.gitlab()} - GitLab CI helper</li>
 *   <li>{@code #ci.ado()} - Azure DevOps helper</li>
 * </ul>
 * 
 * Each helper automatically detects the CI environment and provides convenient
 * access to platform-specific operations like uploading reports, adding PR comments,
 * and streaming repository data.
 * 
 * <p><b>Example usage in actions:</b></p>
 * <pre>
 * # Upload SARIF report to GitHub
 * - var.set:
 *     sarifReport: {fmt: sarif}
 *     result: ${#ci.github().uploadSarif(sarifReport)}
 * 
 * # Add PR comment
 * - var.set:
 *     comment: ${#ci.github().addPrComment("Scan completed!")}
 * 
 * # Access environment properties
 * - if: ${#ci.github().env != null}
 *   log.info: "Running in ${#ci.github().env.owner}/${#ci.github().env.repository}"
 * </pre>
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
@SpelFunctionPrefix("ci.ado()")
public class ActionCiSpelFunctions {
    private final ActionRunnerContext ctx;
    
    /**
     * Get GitHub Actions helper.
     * Automatically detects GitHub Actions environment on first property access.
     * 
     * @return GitHub Actions helper
     */
    public ActionGitHubSpelFunctions github() {
        return new ActionGitHubSpelFunctions(ctx);
    }
    
    /**
     * Get GitLab CI helper.
     * Automatically detects GitLab CI environment on first property access.
     * 
     * @return GitLab CI helper
     */
    public ActionGitLabSpelFunctions gitlab() {
        return new ActionGitLabSpelFunctions(ctx);
    }
    
    /**
     * Get Azure DevOps helper.
     * Automatically detects Azure DevOps environment on first property access.
     * 
     * @return Azure DevOps helper
     */
    public ActionAdoSpelFunctions ado() {
        return new ActionAdoSpelFunctions(ctx);
    }
}
