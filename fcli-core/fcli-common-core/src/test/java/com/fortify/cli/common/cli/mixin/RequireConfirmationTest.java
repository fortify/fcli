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
package com.fortify.cli.common.cli.mixin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

class RequireConfirmationTest {
    @Command(name = "test")
    static class TestCommand {
        @Mixin CommonOptionMixins.RequireConfirmation confirmation;
    }

    static class OuterMixin {
        @Mixin CommonOptionMixins.RequireConfirmation confirmation;
    }

    @Command(name = "nested")
    static class NestedCommand {
        @Mixin OuterMixin outer;
    }

    @Test
    void shortOptionWorksWhenTheMixinIsNestedInAnotherMixin() {
        NestedCommand command = new NestedCommand();
        new CommandLine(command).parseArgs("-y");

        assertTrue(command.outer.confirmation.isConfirmed("anything"));
    }

    @Test
    void isConfirmedIsTrueWhenConfirmOptionIsGiven() {
        TestCommand command = new TestCommand();
        new CommandLine(command).parseArgs("--confirm");

        assertTrue(command.confirmation.isConfirmed("anything"));
    }

    @Test
    void isConfirmedIsFalseWithoutConfirmOptionAndWithoutConsole() {
        assumeTrue(System.console() == null, "only meaningful when no interactive console is attached");
        TestCommand command = new TestCommand();
        new CommandLine(command).parseArgs();

        assertFalse(command.confirmation.isConfirmed("anything"));
    }
}
