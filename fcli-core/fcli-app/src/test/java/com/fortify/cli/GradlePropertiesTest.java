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
package com.fortify.cli;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.StringUtils;

public class GradlePropertiesTest {
    private static final String PRP_EXPECTED_CLASS_NAMES = "gradle.expectedClassNames";
    
    // This test checks whether class names passed by gradle in the gradle.expectedClassNames
    // property (like main class and other classes referenced during the build) can be resolved
    @ParameterizedTest @EnabledIfSystemProperty(named = PRP_EXPECTED_CLASS_NAMES, matches = ".+") 
    @MethodSource("getExpectedClassNames")
    public void testExpectedClassName(String name) throws Exception {
        try {
            Class.forName(name);
        } catch (ClassNotFoundException e) {
            var msg = "Class name define in build.gradle doesn't exist: "+name;
            System.err.println("ERROR: "+msg);
            Assertions.fail(msg);
        }
    }
    
    private static Stream<Arguments> getExpectedClassNames() {
        var prp = System.getProperty(PRP_EXPECTED_CLASS_NAMES);
        return StringUtils.isBlank(prp)
                ? Stream.empty()
                : Stream.of(prp.split(",")).map(Arguments::of);
    }
}
