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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fortify.cli.app._main.cli.cmd.FCLIRootCommands;

import picocli.CommandLine;

public class AviatorSSCAuditCommandTest {
    @Test
    void parsesFolderPriorityOrderWithoutSkipIfExceedingQuota() {
        Assertions.assertDoesNotThrow(() -> new CommandLine(FCLIRootCommands.class).parseArgs(
            "aviator", "ssc", "audit",
            "--av", "BULK_AUDIT:1.6",
            "--app", "qoflow2",
            "--folder-priority-order", "High,Medium"));
    }

    @Test
    void rejectsSkipIfExceedingQuotaWithFolderPriorityOrder() {
        var exception = Assertions.assertThrows(CommandLine.MutuallyExclusiveArgsException.class,
            () -> new CommandLine(FCLIRootCommands.class).parseArgs(
                "aviator", "ssc", "audit",
                "--av", "BULK_AUDIT:1.6",
                "--app", "qoflow2",
                "--skip-if-exceeding-quota",
                "--folder-priority-order", "High"));
        Assertions.assertAll(
            () -> Assertions.assertTrue(exception.getMessage().contains("--skip-if-exceeding-quota")),
            () -> Assertions.assertTrue(exception.getMessage().contains("--folder-priority-order")));
    }
}