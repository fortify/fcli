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
package com.fortify.cli.aviator.diagnose;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.json.JsonHelper;

class AviatorDiagnosticStageResultTest {
    @Test
    void shouldKeepEvidenceNestedOnly() {
        var evidence = JsonHelper.getObjectMapper().createObjectNode();
        evidence.put("tenant", "demo");

        var result = AviatorDiagnosticStageResult.of(1, AviatorDiagnosticStage.TOKEN,
            AviatorDiagnosticStatus.PASS, false, "ok", "none", evidence);

        var node = result.asObjectNode();
        assertTrue(node.path("evidence").has("tenant"));
        assertFalse(node.has("tenant"));
    }
}