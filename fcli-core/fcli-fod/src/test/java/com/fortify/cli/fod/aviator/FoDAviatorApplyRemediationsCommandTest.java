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
package com.fortify.cli.fod.aviator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.fod.aviator.cli.mixin.FoDAviatorApplyRemediationsSourceMixin;
import com.fortify.cli.fod.aviator.cmd.FoDAviatorApplyRemediationsCommand;

import picocli.CommandLine;

/**
 * CLI wiring and product rules for FoD apply-remediations (not default-field or util tests).
 */
class FoDAviatorApplyRemediationsCommandTest {

    @Test
    void fromCacheParsesPath() throws Exception {
        FoDAviatorApplyRemediationsCommand command = parse("--from-cache", "remediations.zip");
        Field sourceSelectorField = FoDAviatorApplyRemediationsCommand.class.getDeclaredField("sourceSelector");
        sourceSelectorField.setAccessible(true);
        FoDAviatorApplyRemediationsSourceMixin sourceSelector =
                (FoDAviatorApplyRemediationsSourceMixin) sourceSelectorField.get(command);
        assertEquals(Path.of("remediations.zip"), sourceSelector.getFromCache());
        assertTrue(sourceSelector.isFromCacheSelected());
    }

    @Test
    void releaseAndFromCacheAreExclusive() {
        assertThrows(CommandLine.ParameterException.class,
                () -> parse("--release", "1", "--from-cache", "local.zip"));
    }

    @Test
    void issueIdsRequireFromCache() {
        FoDAviatorApplyRemediationsCommand command = parse("--release", "1", "--issue-ids", "ISSUE-1");
        assertThrows(FcliSimpleException.class, command::getJsonNode);
    }

    @Test
    void blankSourceDirIsRejected() throws Exception {
        FoDAviatorApplyRemediationsCommand command = parse("--from-cache", "cache.zip");
        Field field = FoDAviatorApplyRemediationsCommand.class.getDeclaredField("sourceCodeDirectory");
        field.setAccessible(true);
        field.set(command, "");
        assertThrows(FcliSimpleException.class, command::getJsonNode);
    }

    private static FoDAviatorApplyRemediationsCommand parse(String... args) {
        FoDAviatorApplyRemediationsCommand command = new FoDAviatorApplyRemediationsCommand();
        new CommandLine(command).parseArgs(args);
        return command;
    }
}
