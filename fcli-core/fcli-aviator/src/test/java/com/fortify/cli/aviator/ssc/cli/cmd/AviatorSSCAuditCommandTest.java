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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.audit.model.FPRAuditResult;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCAuditHelper;
import com.fortify.cli.common.json.JsonHelper;

import picocli.CommandLine;

class AviatorSSCAuditCommandTest {
    @Test
    void testAllowsCombinedFilterOptions() {
        var cmd = parse("--filterset", "Security Auditor View", "--no-filterset");
        assertEquals("Security Auditor View", cmd.getFilterSetTitleOrId());
        assertTrue(cmd.isNoFilterSet());
    }

    @Test
    void testAllowsFilterSetOption() {
        var cmd = parse("--filterset", "Security Auditor View");
        assertEquals("Security Auditor View", cmd.getFilterSetTitleOrId());
        assertFalse(cmd.isNoFilterSet());
    }

    @Test
    void testAllowsNoFilterSetOption() {
        var cmd = parse("--no-filterset");
        assertTrue(cmd.isNoFilterSet());
        assertNull(cmd.getFilterSetTitleOrId());
    }

    @Test
    void testAllowsForceReauditOption() throws Exception {
        var cmd = parse("--force-reaudit");
        Field field = AviatorSSCAuditCommand.class.getDeclaredField("forceReaudit");
        field.setAccessible(true);
        assertTrue((boolean) field.get(cmd));
    }

    @Test
    void reportsDecodeSkippedIssuesAsNotSubmitted() {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.set("operation", JsonHelper.getObjectMapper().createObjectNode());
        var auditResult = new FPRAuditResult(null, "SKIPPED", "All 6 issues were skipped", 0, 6, 0, 6,
                Map.of("Source file decode failed", 6), 0, Map.of());

        AviatorSSCAuditHelper.setAuditStats(result, auditResult);

        var audit = result.path("operation").path("audit");
        assertEquals(0, audit.get("submitted").asInt());
        assertEquals(0, audit.get("succeeded").asInt());
        assertEquals(6, audit.get("skipped").asInt());
    }

    @Test
    void reportsServerSkippedIssuesAsSubmitted() {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.set("operation", JsonHelper.getObjectMapper().createObjectNode());
        var auditResult = new FPRAuditResult(null, "SKIPPED", "Skipped by Aviator", 0, 1, 1, 1,
                Map.of("Skipped by Aviator", 1), 0, Map.of());

        AviatorSSCAuditHelper.setAuditStats(result, auditResult);

        assertEquals(1, result.path("operation").path("audit").get("submitted").asInt());
    }

    @Test
    void excludesOnlyDecodeSkippedIssuesFromSubmittedCount() {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.set("operation", JsonHelper.getObjectMapper().createObjectNode());
        var auditResult = new FPRAuditResult(null, "PARTIALLY_AUDITED", null, 2, 4, 3, 2,
                Map.of("Source file decode failed", 1, "Skipped by Aviator", 1), 0, Map.of());

        AviatorSSCAuditHelper.setAuditStats(result, auditResult);

        var audit = result.path("operation").path("audit");
        assertEquals(3, audit.get("submitted").asInt());
        assertEquals(2, audit.get("succeeded").asInt());
        assertEquals(2, audit.get("skipped").asInt());
    }

    private static AviatorSSCAuditCommand parse(String... args) {
        var cmd = new AviatorSSCAuditCommand();
        var fullArgs = new ArrayList<String>();
        Collections.addAll(fullArgs, "--av", "test:1.0");
        Collections.addAll(fullArgs, args);
        new CommandLine(cmd).parseArgs(fullArgs.toArray(String[]::new));
        return cmd;
    }
}