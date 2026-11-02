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
package com.fortify.cli.aviator.ssc.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;

class AviatorSSCFprTransferHelperTest {
    @Test
    void returnsUploadedArtifactId() throws Exception {
        var response = JsonHelper.getObjectMapper().readTree("{\"data\":{\"id\":2786}}");

        assertEquals("2786", AviatorSSCFprTransferHelper.getUploadedArtifactId(response));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "{}", "{\"data\":{}}", "{\"data\":{\"id\":null}}",
        "{\"data\":{\"id\":\"\"}}", "{\"data\":{\"id\":\"   \"}}"
    })
    void detectsMissingArtifactIdAsUploadFailure(String responseJson) throws Exception {
        var response = responseJson == null ? null : JsonHelper.getObjectMapper().readTree(responseJson);

        var failure = assertThrows(FcliTechnicalException.class,
            () -> AviatorSSCFprTransferHelper.getUploadedArtifactId(response));

        assertEquals("SSC FPR upload response did not contain an artifact ID", failure.getMessage());
    }
}
