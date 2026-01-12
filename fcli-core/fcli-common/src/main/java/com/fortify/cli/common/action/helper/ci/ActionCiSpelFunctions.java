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

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.ci;

import java.util.function.Function;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Factory class for CI system helpers, registered as the #ci SpEL variable in actions.
 * Provides factory methods to obtain platform-specific helpers:
 * 
 * <ul>
 *   <li>{@code #ci.github()} - GitHub Actions helper</li>
 *   <li>{@code #ci.gitlab()} - GitLab CI helper</li>
 *   <li>{@code #ci.ado()} - Azure DevOps helper</li>
 *   <li>{@code #ci.detect()} - Auto-detect current CI system</li>
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
 * 
 * # Auto-detect CI system
 * - var.set:
 *     detected: ${#ci.detect()}
 * - log.info: "Detected CI system: ${detected.type}"
 * </pre>
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
@Accessors(fluent=true)
@SpelFunctionPrefix("ci.")
public class ActionCiSpelFunctions {
    private final ActionRunnerContext ctx;
    
    /**
     * Get GitHub Actions helper.
     * Automatically detects GitHub Actions environment on first property access.
     * 
     * @return GitHub Actions helper
     */
    @Getter(lazy=true, onMethod_=@SpelFunction(cat=ci, returns="GitHub Actions helper instance"))
    private final ActionGitHubSpelFunctions github = new ActionGitHubSpelFunctions(ctx);
    
    /**
     * Get GitLab CI helper.
     * Automatically detects GitLab CI environment on first property access.
     * 
     * @return GitLab CI helper
     */
    @Getter(lazy=true, onMethod_=@SpelFunction(cat=ci, returns="GitLab CI helper instance"))
    private final ActionGitLabSpelFunctions gitlab = new ActionGitLabSpelFunctions(ctx);
    
    /**
     * Get Azure DevOps helper.
     * Automatically detects Azure DevOps environment on first property access.
     * 
     * @return Azure DevOps helper
     */
    @Getter(lazy=true, onMethod_=@SpelFunction(cat=ci, returns="Azure DevOps helper instance"))
    private final ActionAdoSpelFunctions ado = new ActionAdoSpelFunctions(ctx);
    
    /**
     * Enum defining all available CI system implementations.
     * Provides a centralized registry for easy iteration and future extensibility.
     */
    @RequiredArgsConstructor
    public enum CiSystemType {
        GITHUB(ActionCiSpelFunctions::github),
        GITLAB(ActionCiSpelFunctions::gitlab),
        ADO(ActionCiSpelFunctions::ado),
        UNKNOWN(x->new ActionUnknownCiSpelFunctions());
        
        private final Function<ActionCiSpelFunctions, IActionSpelFunctions> instanceGetter;
        
        public Function<ActionCiSpelFunctions, IActionSpelFunctions> getInstanceGetter() {
            return instanceGetter;
        }
    }
    
    /**
     * Auto-detect the current CI system.
     * Iterates over all known CI system implementations and returns the first one
     * that successfully detects its environment (getEnv() returns non-null).
     * If no CI system is detected, returns an ActionUnknownCiSpelFunctions instance.
     * 
     * Use the returned object's getType() method to determine which CI system was detected,
     * then refer to the corresponding #ci.<type>() documentation for available methods.
     * For example, if getType() returns "github", refer to #ci.github().* documentation.
     * 
     * @return Detected CI system helper, or ActionUnknownCiSpelFunctions if none detected
     */
    @SpelFunction(cat=ci, desc="Auto-detects current CI system; returns helper for detected system or unknown. "
            + "Use `type` property to check detected system (github/gitlab/ado/unknown), "
            + "then refer to `#ci.<type>().*` documentation for available methods",
            returns="CI helper instance for detected system")
    public IActionSpelFunctions detect() {
        for (CiSystemType ciType : CiSystemType.values()) {
            IActionSpelFunctions helper = ciType.getInstanceGetter().apply(this);
            if (helper.getEnv() != null) {
                return helper;
            }
        }
        throw new IllegalStateException("detect() should always return a value; UNKNOWN should match");
    }
}
