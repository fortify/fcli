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
package com.fortify.cli.ssc._common.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.SpelEvaluator;
import com.fortify.cli.common.spel.SpelHelper;

class SSCBulkAuditFailureReportingTest {
    @ParameterizedTest
    @CsvSource({
        "sast, AUDITED_NOT_UPLOADED, 1, true, true",
        "dast, AUDITED_NOT_UPLOADED, 1, true, true",
        "sast, AUDITED, 0, true, false",
        "dast, AUDITED, 0, true, false",
        "sast, SKIPPED, 0, true, false",
        "dast, SKIPPED, 0, true, false",
        "sast, QUOTA_EXCEEDED, 0, true, false",
        "sast, FAILED, 1, false, true",
        "dast, FAILED, 1, false, true",
        "dast, FAILED, 0, true, true",
        "dast, PARTIALLY_AUDITED, 0, true, true",
        "dast, PARTIALLY_AUDITED, 1, true, true"
    })
    void countsFailedCommandsOnceAndIncludesFailuresInSummaries(
            String engine, String status, int exitCode, boolean hasRecords, boolean expectedFailure) throws Exception {
        var loaded = ActionLoaderHelper.load(
            ActionSource.builtinActionSources("SSC"), "bulkaudit-" + engine, ActionValidationHandler.IGNORE);
        JsonNode action = new ObjectMapper(new YAMLFactory()).readTree(loaded.getActionText());
        String functionName = "sast".equals(engine) ? "runSastAudit" : "runDastAudit";
        JsonNode runAudit = action.path("functions").path(functionName)
            .path("steps").get(0).path("run.fcli").path("run_audit");
        assertFalse(runAudit.path("status.check").asBoolean(true));
        assertTrue(runAudit.path("records.collect").asBoolean(false));
        var nodes = new ArrayList<JsonNode>();
        collectObjects(action.path("steps"), nodes);
        var failureSteps = nodes.stream()
            .filter(node -> node.path("do").isArray() && node.path("do").findValues("log.warn").stream()
                .anyMatch(warning -> warning.asText().startsWith(
                    "sast".equals(engine) ? "Audit failed for " : "DAST audit failed for ")))
            .filter(node -> node.has("if") && node.path("if").asText().contains("run_audit.exitCode"))
            .toList();
        assertEquals(1, failureSteps.size());

        ObjectNode context = JsonHelper.getObjectMapper().createObjectNode();
        var command = context.putObject("run_audit").put("exitCode", exitCode);
        if (hasRecords) {
            command.putArray("records").addObject().put("__action__", status);
        } else {
            command.putNull("records");
        }
        var stats = context.putObject("stats");
        stats.put("audit_failures", 0).put("failures", 0).put("create_successes", 0)
            .put("create_attempts", 0).put("create_failures", 0).put("audit_attempts", 1)
            .put("selection_failures", 0).put("prepare_failures", 0).put("quota_skipped", 0)
            .put("create_skipped_after_failure", 0).put("missing_dast_skipped", 0).put("unchanged_skipped", 0);
        context.putObject("cli").put("aviator-app-mapping", "app");

        JsonNode failureStep = failureSteps.get(0);
        assertEquals(expectedFailure, evaluate(failureStep.path("if").asText(), context, Boolean.class).booleanValue());
        if (expectedFailure) {
            for (var entry : failureStep.path("do")) {
                var assignments = entry.path("var.set").fields();
                while (assignments.hasNext()) {
                    var assignment = assignments.next();
                    String counter = assignment.getKey().substring("stats.".length());
                    stats.put(counter, evaluate(assignment.getValue().asText(), context, Integer.class).intValue());
                }
            }
        }
        int failures = expectedFailure ? 1 : 0;
        assertEquals(failures, stats.path("audit_failures").asInt());
        if ("dast".equals(engine)) {
            assertEquals(failures, stats.path("failures").asInt());
        }

        var summaries = nodes.stream().map(node -> node.path("log.info").asText())
            .filter(message -> message.startsWith("Complete (mapping:")).toList();
        assertEquals("sast".equals(engine) ? 2 : 1, summaries.size());
        for (String summary : summaries) {
            String rendered = evaluate(summary, context, String.class);
            assertTrue(rendered.contains("sast".equals(engine)
                ? "Audit failures " + failures : "Failures " + failures + " (create 0, audit " + failures), rendered);
        }
    }

    private static <T> T evaluate(String expression, JsonNode context, Class<T> type) {
        return SpelEvaluator.JSON_GENERIC.evaluate(SpelHelper.parseTemplateExpression(expression), context, type);
    }

    private static void collectObjects(JsonNode node, List<JsonNode> objects) {
        if (node.isObject()) {
            objects.add(node);
        }
        if (node.isContainerNode()) {
            node.forEach(child -> collectObjects(child, objects));
        }
    }
}
