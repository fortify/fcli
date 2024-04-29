/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.ssc._common.action;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionInvalidSignatureHandlers;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.crypto.SignatureHelper.InvalidSignatureHandler;

// TODO Move this class to a common test utility module; currently
//      exact copies of this class are available in every module 
//      that performs action tests.
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractActionTest {
    @ParameterizedTest
    @MethodSource("getActions")
    public void testLoadAction(String name) {
        try {
            ActionLoaderHelper
            .load(ActionSource.builtinActionSources(getType()), name, invalidSignatureHandler())
            .asAction();
        } catch ( Exception e ) {
            System.err.println(String.format("Error loading %s action %s:\n%s", getType(), name, e));
            Assertions.fail(String.format("Error loading %s action %s", getType(), name), e);
        }
    }

    public final String[] getActions() {
        return ActionLoaderHelper.streamAsJson(ActionSource.builtinActionSources(getType()), ActionInvalidSignatureHandlers.IGNORE)
                .map(a->a.get("name").asText())
                .toArray(String[]::new);
    }

    private static final InvalidSignatureHandler invalidSignatureHandler() {
        return "true".equalsIgnoreCase((System.getProperty("test.action.requireValidSignature")))
                ? ActionInvalidSignatureHandlers.FAIL
                : ActionInvalidSignatureHandlers.WARN;
    }
    
    protected abstract String getType();
}