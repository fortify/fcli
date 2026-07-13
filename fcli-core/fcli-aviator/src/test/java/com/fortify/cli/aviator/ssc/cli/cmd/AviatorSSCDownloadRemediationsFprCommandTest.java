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

import com.fortify.cli.common.exception.FcliSimpleException;

import picocli.CommandLine;

class AviatorSSCDownloadRemediationsFprCommandTest {
    @Test
    void testArtifactIdAndLatestAreMutuallyExclusive() {
        assertThrows(CommandLine.ParameterException.class,
                () -> parse("--artifact-id", "1", "--latest"));
    }

    @Test
    void testAllRejectsFileOption() {
        AviatorSSCDownloadRemediationsFprCommand command = parse("--all", "--av", "1", "-f", "one.fpr");

        assertThrows(FcliSimpleException.class, () -> command.getJsonNode(null));
    }

    @Test
    void testAllRequiresDirectoryOption() {
        AviatorSSCDownloadRemediationsFprCommand command = parse("--all", "--av", "1");

        assertThrows(FcliSimpleException.class, () -> command.getJsonNode(null));
    }

    @Test
    void testArtifactIdRejectsAppVersion() {
        AviatorSSCDownloadRemediationsFprCommand command = parse("--artifact-id", "1", "--av", "2");

        assertThrows(FcliSimpleException.class, () -> command.getJsonNode(null));
    }

    private static AviatorSSCDownloadRemediationsFprCommand parse(String... args) {
        AviatorSSCDownloadRemediationsFprCommand command = new AviatorSSCDownloadRemediationsFprCommand();
        new CommandLine(command).parseArgs(args);
        return command;
    }
}