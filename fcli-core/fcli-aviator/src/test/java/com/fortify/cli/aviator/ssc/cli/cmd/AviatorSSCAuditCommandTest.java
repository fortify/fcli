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

import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

class AviatorSSCAuditCommandTest {
    @Test
    void registersCanonicalAndDeprecatedCommandNames() {
        assertEquals("audit-sast", new CommandLine(new AviatorSSCSastAuditCommand()).getCommandName());
        assertEquals("audit", new CommandLine(new AviatorSSCAuditCommand()).getCommandName());
    }

    @Test
    void deprecatedCommandHelpPointsToCanonicalCommand() {
        var messages = ResourceBundle.getBundle("com.fortify.cli.aviator.i18n.AviatorMessages");
        String header = messages.getString("fcli.aviator.ssc.audit.usage.header");
        String description = messages.getString("fcli.aviator.ssc.audit.usage.description");

        assertTrue(header.contains("(DEPRECATED)"));
        assertTrue(description.contains("fcli aviator ssc audit-sast"));
    }

    @Test
    void canonicalCommandAllowsFilterOptions() {
        var cmd = parseCanonical("--filterset", "Security Auditor View", "--no-filterset");
        assertEquals("Security Auditor View", cmd.getFilterSetTitleOrId());
        assertTrue(cmd.isNoFilterSet());
    }

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

    private static AviatorSSCAuditCommand parse(String... args) {
        var cmd = new AviatorSSCAuditCommand();
        parse(cmd, args);
        return cmd;
    }

    private static AviatorSSCSastAuditCommand parseCanonical(String... args) {
        var cmd = new AviatorSSCSastAuditCommand();
        parse(cmd, args);
        return cmd;
    }

    private static void parse(AviatorSSCSastAuditCommand cmd, String... args) {
        var fullArgs = new ArrayList<String>();
        Collections.addAll(fullArgs, "--av", "test:1.0");
        Collections.addAll(fullArgs, args);
        new CommandLine(cmd).parseArgs(fullArgs.toArray(String[]::new));
    }
}