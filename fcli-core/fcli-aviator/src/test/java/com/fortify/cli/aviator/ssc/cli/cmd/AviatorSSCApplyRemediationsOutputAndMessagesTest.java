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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.util.AviatorRemediationMetricsHelper;
import com.fortify.cli.aviator.fpr.remediation.RemediationExecutionMode;
import com.fortify.cli.aviator.fpr.remediation.model.RemediationMetric;
import com.fortify.cli.common.json.JsonHelper;

import picocli.CommandLine;
import picocli.CommandLine.Model.OptionSpec;

class AviatorSSCApplyRemediationsOutputAndMessagesTest {
    private static final String MESSAGES = "com/fortify/cli/aviator/i18n/AviatorMessages.properties";
    private static final String PREFIX = "fcli.aviator.ssc.apply-remediations.";
    private static final Set<String> REMEDIATION_COLUMNS = Set.of(
        "totalRemediation", "remediatedRemediation", "appliedRemediation", "availableRemediation", "identicalRemediation",
        "supersededRemediation", "supersedesPriorFixRemediation", "possiblyRemediatedRemediation",
        "coveredByPriorFixRemediation", "skippedRemediation", "skippedReasons");

    private static Properties messages() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = AviatorSSCApplyRemediationsOutputAndMessagesTest.class.getClassLoader().getResourceAsStream(MESSAGES)) {
            assertNotNull(in, "message bundle not found: " + MESSAGES);
            properties.load(in);
        }
        return properties;
    }

    private static ObjectNode resultFor(RemediationExecutionMode mode) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        AviatorRemediationMetricsHelper.putRemediationMetricFields(result, RemediationMetric.builder()
            .executionMode(mode)
            .totalRemediations(1)
            .appliedRemediations(1)
            .build());
        return result;
    }

    @Test
    void tableShowsEveryRemediationCountAndEachOneIsInTheResult() throws IOException {
        List<String> columns = Arrays.asList(messages().getProperty(PREFIX + "output.table.args").split(","));
        ObjectNode apply = resultFor(RemediationExecutionMode.APPLY);
        ObjectNode preview = resultFor(RemediationExecutionMode.PREVIEW);

        assertTrue(columns.containsAll(REMEDIATION_COLUMNS), "table columns: " + columns);
        for (String column : REMEDIATION_COLUMNS) {
            assertTrue(apply.has(column) || preview.has(column), "neither result has the table column " + column);
        }
    }

    @Test
    void everyCountFieldOfTheResultIsAColumnOfTheTable() throws IOException {
        List<String> columns = Arrays.asList(messages().getProperty(PREFIX + "output.table.args").split(","));

        for (RemediationExecutionMode mode : RemediationExecutionMode.values()) {
            ObjectNode result = resultFor(mode);
            result.fieldNames().forEachRemaining(field -> {
                if (field.endsWith("Remediation") && !columns.contains(field)) {
                    throw new AssertionError(mode + " result field " + field + " is not a table column: " + columns);
                }
            });
        }
    }

    @Test
    void previewReportsTheCountAsAvailableAndApplyAsApplied() {
        ObjectNode preview = resultFor(RemediationExecutionMode.PREVIEW);
        ObjectNode apply = resultFor(RemediationExecutionMode.APPLY);

        assertEquals(1, preview.get("availableRemediation").asInt());
        assertFalse(preview.has("appliedRemediation"));
        assertEquals(1, apply.get("appliedRemediation").asInt());
        assertFalse(apply.has("availableRemediation"));
    }

    @Test
    void confirmPromptTakesTheRequestedIssuesTheFixAndEverythingItResolves() throws IOException {
        String prompt = messages().getProperty(PREFIX + "confirmPrompt");
        assertNotNull(prompt);

        String formatted = String.format(prompt, "D1, D2", "fix-A", "D1, D2, X");

        assertTrue(formatted.contains("D1, D2"));
        assertTrue(formatted.contains("D1, D2, X"));
        assertEquals(2, formatted.split("fix-A", -1).length - 1, "the fix id is shown in the explanation and in the question");
    }

    @Test
    void applyCommandHasExactlyOneConfirmOptionWithAShortForm() {
        var spec = new CommandLine(new AviatorSSCApplyRemediationsCommand()).getCommandSpec();

        OptionSpec confirm = spec.findOption("--confirm");
        assertNotNull(confirm);
        assertEquals(List.of("-y", "--confirm"), Arrays.asList(confirm.names()));
        assertEquals(1, spec.options().stream().filter(o -> Arrays.asList(o.names()).contains("-y")).count());
    }
}
