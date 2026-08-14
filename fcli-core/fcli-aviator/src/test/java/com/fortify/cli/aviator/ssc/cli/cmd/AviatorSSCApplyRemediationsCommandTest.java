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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCApplyRemediationsSourceMixin;
import com.fortify.cli.common.exception.FcliSimpleException;

import picocli.CommandLine;

/**
 * CLI wiring and product rules for apply-remediations (not metric/util logic).
 */
class AviatorSSCApplyRemediationsCommandTest {

    @Test
    void fromCacheParsesPath() throws Exception {
        AviatorSSCApplyRemediationsCommand command = parse("--from-cache", "remediations.zip");
        assertEquals(Path.of("remediations.zip"), getSourceMixin(command).getFromCache());
        assertTrue(getSourceMixin(command).isFromCacheSelected());
    }

    @Test
    void fromCacheCannotBeCombinedWithArtifactId() {
        assertThrows(CommandLine.ParameterException.class,
                () -> parse("--artifact-id", "1", "--from-cache", "cache.zip"));
    }

    @Test
    void issueIdsRequireFromCache() {
        AviatorSSCApplyRemediationsCommand command = parse("--artifact-id", "1", "--issue-ids", "ISSUE-1");
        assertThrows(FcliSimpleException.class, command::getJsonNode);
    }

    private static AviatorSSCApplyRemediationsCommand parse(String... args) {
        AviatorSSCApplyRemediationsCommand command = new AviatorSSCApplyRemediationsCommand();
        new CommandLine(command).parseArgs(args);
        return command;
    }

    private static AviatorSSCApplyRemediationsSourceMixin getSourceMixin(AviatorSSCApplyRemediationsCommand command)
            throws Exception {
        Field field = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("sourceSelector");
        field.setAccessible(true);
        return (AviatorSSCApplyRemediationsSourceMixin) field.get(command);
    }
}
