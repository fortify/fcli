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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;

import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * SpEL functions for detecting the current CI system and accessing 
 * the CI-specific helper methods.
 *
 * @author Ruud Senden
 */
@Reflectable
@Accessors(fluent=true)
@SpelFunctionPrefix("_ci.")
@Slf4j
public class ActionCiSpelFunctions {
    private final IActionSpelFunctions[] ciSpelFunctions;
    
    public ActionCiSpelFunctions(IActionSpelFunctions... ciSpelFunctions) {
        this.ciSpelFunctions = ciSpelFunctions;
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
    @SpelFunction(cat=ci, desc="""
            Auto-detects current CI system; returns an object that provides CI-specific SpEL functions.
            The returned object is guaranteed to have non-null `type` and `env` properties, with `type`
            corresponding to one of the documented known CI systems (github/gitlab/ado/bitbucket), allowing the
            SpEL functions as documented for that CI system to be called on the returned object. For 
            example, if `type` equals `github`, the documented `github.*` SpEL functions may be called
            on the returned object. If no known CI system is detected, `type` will be "unknown", `env`
            will be an empty JSON object, and no other SpEL functions will be available. 
            """,
            returns="CI-specific object providing SpEL functions for the detected CI system")
    public IActionSpelFunctions detect() {
        for (var ciSpelFunctions : ciSpelFunctions) {
            if (ciSpelFunctions.getEnv() != null) {
                log.debug("Detected CI environment: {}", ciSpelFunctions.getEnv());
                return ciSpelFunctions;
            }
        }
        log.debug("No CI environment detected");
        return ActionUnknownCiSpelFunctions.INSTANCE;
    }
    
    /**
     * Unknown/unsupported CI system implementation.
     * Used when no known CI system is detected.
     * 
     * @author rsenden
     */
    @Reflectable
    private static final class ActionUnknownCiSpelFunctions implements IActionSpelFunctions {
        private static final ActionUnknownCiSpelFunctions INSTANCE = new ActionUnknownCiSpelFunctions();
        /**
         * Returns an empty ObjectNode since no CI environment was detected.
         */
        @SpelFunction(cat=ci, desc="Returns empty ObjectNode (no CI environment detected)",
                returns="Empty ObjectNode")
        @Override
        public ObjectNode getEnv() {
            return JsonHelper.getObjectMapper().createObjectNode();
        }
        
        /**
         * Returns "unknown" as the CI system type.
         */
        @SpelFunction(cat=ci, desc="Returns CI system type identifier",
                returns="\"unknown\"")
        @Override
        public String getType() {
            return "unknown";
        }
    }
}
