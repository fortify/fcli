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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.fortify.cli.common.exception.FcliSimpleException;

import picocli.CommandLine;

class AviatorSSCDownloadRemediationsCacheCommandTest {
    @Test
    void testArtifactIdAndLatestAreMutuallyExclusive() {
        assertThrows(CommandLine.ParameterException.class,
                () -> parse("--artifact-id", "1", "--latest", "-f", "cache.zip"));
    }

    @Test
    void testFileIsRequired() {
        assertThrows(CommandLine.ParameterException.class,
                () -> parse("--artifact-id", "1"));
    }

    @Test
    void testArtifactIdRejectsAppVersion() {
        AviatorSSCDownloadRemediationsCacheCommand command = parse("--artifact-id", "1", "--av", "2", "-f", "cache.zip");

        assertThrows(FcliSimpleException.class, () -> command.getJsonNode(null));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void fileRejectsCorruptionMarker() {
        assertThrows(CommandLine.ParameterException.class,
                () -> parse("--artifact-id", "1", "-f", "C:\\temp\\?\\cache.zip"));
    }

    private static AviatorSSCDownloadRemediationsCacheCommand parse(String... args) {
        AviatorSSCDownloadRemediationsCacheCommand command = new AviatorSSCDownloadRemediationsCacheCommand();
        new CommandLine(command).parseArgs(args);
        return command;
    }
}
