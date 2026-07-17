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

import com.fortify.cli.fod.aviator.cmd.FoDAviatorDownloadRemediationsCacheCommand;

import picocli.CommandLine;

class FoDAviatorDownloadRemediationsCacheCommandTest {
    @Test
    void testReleaseIsRequired() {
        FoDAviatorDownloadRemediationsCacheCommand command = new FoDAviatorDownloadRemediationsCacheCommand();

        assertThrows(CommandLine.ParameterException.class,
                () -> new CommandLine(command).parseArgs("-f", "cache.zip"));
    }

    @Test
    void testFileIsRequired() {
        FoDAviatorDownloadRemediationsCacheCommand command = new FoDAviatorDownloadRemediationsCacheCommand();

        assertThrows(CommandLine.ParameterException.class,
                () -> new CommandLine(command).parseArgs("--release", "1"));
    }

    @Test
    void testReleaseWithFileOptionParses() {
        FoDAviatorDownloadRemediationsCacheCommand command = new FoDAviatorDownloadRemediationsCacheCommand();

        new CommandLine(command).parseArgs("--release", "1", "-f", "remediations.zip");
    }
}
