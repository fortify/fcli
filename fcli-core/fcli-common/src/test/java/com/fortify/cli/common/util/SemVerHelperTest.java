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
package com.fortify.cli.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 *
 * @author Ruud Senden
 */
public class SemVerHelperTest {
    @ParameterizedTest
    @CsvSource({
        ",,0",
        "a,a,0",
        "1.2.3,1.2.3,0",        
        "a,b,-1",
        "b,a,1",
        "a,1.2.3,-1",
        "1.2.3,a,1",
        "a,1.2.3-alpha1,-1",
        "1.2.3-alpha1,a,1",
        "1.2.0,1.2.1,-1",
        "1.2.1,1.2.0,1",
        "1.2.0,1.3.0,-1",
        "1.3.0,1.2.0,1",
        "1.0.0,2.0.0,-1",
        "2.0.0,1.0.0,1",
        "1.2.0,1.2.0-alpha1,1",
        "1.2.0-alpha1,1.2.0,-1"
    })
    public void testSemVerCompare(String semver1, String semver2, int expectedResult) throws Exception {
        assertEquals(expectedResult, SemVerHelper.compare(semver1, semver2));
    }
}
