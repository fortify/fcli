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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.fortify.cli.fod.aviator.cmd.FoDAviatorDownloadRemediationsCacheCommand;

import picocli.CommandLine;

class FoDAviatorDownloadRemediationsCacheCommandTest {
    @Test
    void releaseIsRequired() {
        assertThrows(CommandLine.ParameterException.class,
                () -> parse("-f", "cache.zip"));
    }

    @Test
    void fileIsRequired() {
        assertThrows(CommandLine.ParameterException.class,
                () -> parse("--release", "1"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void fileRejectsCorruptionMarker() {
        assertThrows(CommandLine.ParameterException.class,
                () -> parse("--release", "1", "-f", "C:\\temp\\?\\cache.zip"));
    }

    private static void parse(String... args) {
        new CommandLine(new FoDAviatorDownloadRemediationsCacheCommand()).parseArgs(args);
    }
}
