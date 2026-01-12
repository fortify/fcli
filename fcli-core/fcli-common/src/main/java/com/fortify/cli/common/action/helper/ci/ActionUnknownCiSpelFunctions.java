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


/**
 * Unknown/unsupported CI system implementation.
 * Used when no known CI system is detected.
 * 
 * @author rsenden
 */
@Reflectable
@SpelFunctionPrefix("ci.unknown().")
public class ActionUnknownCiSpelFunctions implements IActionSpelFunctions {
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
