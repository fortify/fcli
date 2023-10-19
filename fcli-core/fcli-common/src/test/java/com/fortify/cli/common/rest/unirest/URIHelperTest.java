/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.rest.unirest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 *
 * @author Ruud Senden
 */
public class URIHelperTest {
    private static final String BASE_URI = "https://x.y/z";
    @ParameterizedTest
    @CsvSource({
        ",?o=10", 
        "?a=b,?a=b&o=10", 
        "?oo=5,?oo=5&o=10",
        "?o=5,?o=10",
        "?oo=5&o=5,?oo=5&o=10",
        "?o=5&oo=5,?oo=5&o=10",
        "?oo=5&o=5&oo=7,?oo=5&oo=7&o=10",
        "?oo=5&o=5&o=6&oo=7,?oo=5&oo=7&o=10",
    })
    public void testAddOrReplaceParam(String input, String expected) throws Exception {
        if ( input==null ) { input = ""; }
        if ( expected==null ) { expected = ""; }
        var actual = URIHelper.addOrReplaceParam(BASE_URI+input, "o", "10");
        assertEquals(BASE_URI+expected, actual);
    }
}
