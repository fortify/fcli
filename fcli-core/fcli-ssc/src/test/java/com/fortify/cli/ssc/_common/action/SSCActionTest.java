/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc._common.action;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fortify.cli.common.action.helper.ActionHelper;

public class SSCActionTest {
    private static final String TYPE = "SSC";
    @ParameterizedTest
    @MethodSource("getActions")
    public void testLoadAction(String name) {
        try {
            ActionHelper.load(TYPE, name);
        } catch ( Exception e ) {
            System.err.println(String.format("Error loading %s action %s:\n%s", TYPE, name, e));
            Assertions.fail(String.format("Error loading %s action %s", TYPE, name), e);
        }
    }
    
    public static final String[] getActions() {
        return ActionHelper.list(TYPE)
                .map(a->a.get("name").asText())
                .toArray(String[]::new);
    }
}
