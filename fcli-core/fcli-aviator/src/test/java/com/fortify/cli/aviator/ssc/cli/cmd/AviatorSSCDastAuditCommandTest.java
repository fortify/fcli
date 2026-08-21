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
package com.fortify.cli.aviator.ssc.cli.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.audit.DastAuditFprResult;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCAuditHelper;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;

import picocli.CommandLine;

class AviatorSSCDastAuditCommandTest {
    @Test
    void rejectsClientSideToneImprovementOptions() {
        assertThrows(CommandLine.ParameterException.class, () -> parse("--improve-tone"));
        assertThrows(CommandLine.ParameterException.class, () -> parse("--no-improve-tone"));
    }

    @Test
    void acceptsCustomTagMapping() {
        var commandLine = new CommandLine(new AviatorSSCDastAuditCommand());

        var parseResult = commandLine.parseArgs("--av", "test:1.0", "--tag-mapping", "dast-tags.yaml");

        assertEquals("dast-tags.yaml", parseResult.matchedOptionValue("--tag-mapping", null));
    }

    @Test
    void dastAuditStatsUseSastAuditOutputEnvelope() {
        var appVersion = new SSCAppVersionDescriptor();
        appVersion.setVersionId("42");
        appVersion.setApplicationName("WebGoat");
        appVersion.setVersionName("1.0");
        var auditResult = new DastAuditFprResult(
            null, "PARTIALLY_AUDITED", null, 8, 6, 6, 4,
            2, 1, 1, 2, 0, 6, 2, false, null, null);

        var result = AviatorSSCAuditHelper.buildResultNode(appVersion, "2786", auditResult.status());
        AviatorSSCAuditHelper.setDastAuditStats(result, auditResult);

        assertEquals("42", result.path("id").asText());
        assertEquals("WebGoat", result.path("applicationName").asText());
        assertEquals("2786", result.path("artifactId").asText());
        assertEquals("PARTIALLY_AUDITED", result.path("__action__").asText());
        assertEquals(6, result.path("operation").path("audit").path("submitted").asInt());
        assertEquals(4, result.path("operation").path("audit").path("succeeded").asInt());
        assertEquals(2, result.path("operation").path("audit").path("skipped").asInt());
        assertEquals(0, result.path("operation").path("audit").path("failed").asInt());
        assertFalse(result.path("operation").path("audit").has("truePositives"));
        assertFalse(result.path("operation").path("audit").has("falsePositivesSuppressed"));
        assertFalse(result.path("operation").path("audit").has("likelyFalsePositives"));
        assertFalse(result.has("state"));
        assertFalse(result.has("submitted"));
    }

    private static AviatorSSCDastAuditCommand parse(String... args) {
        var command = new AviatorSSCDastAuditCommand();
        var fullArgs = new ArrayList<String>();
        Collections.addAll(fullArgs, "--av", "test:1.0");
        Collections.addAll(fullArgs, args);
        new CommandLine(command).parseArgs(fullArgs.toArray(String[]::new));
        return command;
    }
}