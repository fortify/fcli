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
package com.fortify.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fortify.cli.app._main.cli.cmd.FCLIRootCommands;
import com.fortify.cli.common.output.writer.CommandSpecMessageResolver;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

@DisplayName("Aviator branding")
class AviatorBrandingTest {
    @Test
    @DisplayName("should expose renamed Aviator usage headers without changing command namespaces")
    void shouldExposeRenamedAviatorUsageHeadersWithoutChangingCommandNamespaces() {
        var root = new CommandLine(FCLIRootCommands.class);
        var aviator = requireSubcommand(root, "aviator");
        var app = requireSubcommand(aviator, "app");
        var entitlement = requireSubcommand(aviator, "entitlement");
        var ssc = requireSubcommand(aviator, "ssc");
        var fod = requireSubcommand(root, "fod");
        var fodAviator = requireSubcommand(fod, "aviator");

        assertEquals("aviator", aviator.getCommandSpec().name());
        assertEquals("aviator", fodAviator.getCommandSpec().name());
        assertEquals("Manage Fortify Aviator user sessions (start here).",
            message(requireSubcommand(aviator, "session").getCommandSpec()));
        assertEquals("Interact with Fortify Aviator.", message(aviator.getCommandSpec()));
        assertEquals("Add one entitlement to an existing Fortify Aviator application.",
            message(requireSubcommand(app, "add-entitlement").getCommandSpec()));
        assertEquals("Audit an SSC application version using Fortify Remediation Aviator.",
            message(requireSubcommand(ssc, "audit").getCommandSpec()));
        assertEquals("Correlate SAST and DAST findings for an SSC application version.",
            message(requireSubcommand(ssc, "correlate-sast-dast").getCommandSpec()));
        assertEquals("List Fortify Remediation Aviator entitlements for a tenant.",
            message(requireSubcommand(entitlement, "list-sast").getCommandSpec()));
        assertEquals("List Fortify DAST Aviator entitlements for a tenant.",
            message(requireSubcommand(entitlement, "list-dast").getCommandSpec()));
        assertEquals("Use Fortify Remediation Aviator with FoD.", message(fodAviator.getCommandSpec()));
    }

    private static CommandLine requireSubcommand(CommandLine commandLine, String name) {
        var subcommand = commandLine.getSubcommands().get(name);
        assertNotNull(subcommand, () -> "Missing subcommand: " + name);
        return subcommand;
    }

    private static String message(CommandSpec commandSpec) {
        return new CommandSpecMessageResolver(commandSpec).getMessageString("usage.header");
    }
}