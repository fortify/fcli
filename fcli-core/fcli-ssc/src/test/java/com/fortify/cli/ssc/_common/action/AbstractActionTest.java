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

import java.util.function.BiConsumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler.ActionInvalidSchemaVersionHandler;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler.ActionInvalidSignatureHandler;
import com.fortify.cli.common.action.model.Action.ActionMetadata;
import com.fortify.cli.common.crypto.helper.SignatureHelper.SignedTextDescriptor;

// TODO Move this class to a common test utility module; currently
//      exact copies of this class are available in every module 
//      that performs action tests.
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractActionTest {
    @ParameterizedTest
    @MethodSource("getActions")
    public void testLoadAction(String name) {
        try {
            var actionValidationHandler = ActionValidationHandler.builder()
                    .onSignatureStatusDefault(invalidSignatureHandler())
                    .onUnsupportedSchemaVersion(ActionInvalidSchemaVersionHandler.fail)
                    .build();
            ActionLoaderHelper
                .load(ActionSource.builtinActionSources(getType()), name, actionValidationHandler)
                .getAction();
        } catch ( Exception e ) {
            System.err.println(String.format("Error loading %s action %s:\n%s", getType(), name, e));
            Assertions.fail(String.format("Error loading %s action %s", getType(), name), e);
        }
    }

    public final String[] getActions() {
        return ActionLoaderHelper.streamAsJson(ActionSource.builtinActionSources(getType()), ActionValidationHandler.IGNORE)
                .map(a->a.get("name").asText())
                .toArray(String[]::new);
    }

    private static final BiConsumer<ActionMetadata, SignedTextDescriptor> invalidSignatureHandler() {
        return "true".equalsIgnoreCase((System.getProperty("test.action.requireValidSignature")))
                ? ActionInvalidSignatureHandler.fail
                : ActionInvalidSignatureHandler.warn;
    }
    
    protected abstract String getType();
}