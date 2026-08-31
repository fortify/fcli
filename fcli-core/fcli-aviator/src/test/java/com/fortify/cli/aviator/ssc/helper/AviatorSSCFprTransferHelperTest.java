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

import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;

class AviatorSSCFprTransferHelperTest {
    @Test
    void returnsUploadedArtifactId() throws Exception {
        var response = JsonHelper.getObjectMapper().readTree("{\"data\":{\"id\":2786}}");

        assertEquals("2786", AviatorSSCFprTransferHelper.getUploadedArtifactId(response));
    }

    @Test
    void rejectsMissingArtifactId() throws Exception {
        var response = JsonHelper.getObjectMapper().readTree("{\"data\":{}}");

        assertThrows(FcliTechnicalException.class,
            () -> AviatorSSCFprTransferHelper.getUploadedArtifactId(response));
        assertThrows(FcliTechnicalException.class,
            () -> AviatorSSCFprTransferHelper.getUploadedArtifactId(null));
    }
}
