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

import com.fortify.cli.aviator._common.cli.mixin.ApplyRemediationsOptionsMixin;
import com.fortify.cli.aviator._common.output.cli.cmd.AbstractAviatorApplyRemediationsCommand;
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
        Field applyOptionsField = AbstractAviatorApplyRemediationsCommand.class.getDeclaredField("applyOptions");
        applyOptionsField.setAccessible(true);
        ApplyRemediationsOptionsMixin applyOptions = (ApplyRemediationsOptionsMixin) applyOptionsField.get(command);
        Field sourceDirField = ApplyRemediationsOptionsMixin.class.getDeclaredField("sourceCodeDirectory");
        sourceDirField.setAccessible(true);
        sourceDirField.set(applyOptions, "");
        assertThrows(FcliSimpleException.class, command::getJsonNode);
    }

    @Test
    void previewFlagParsedCorrectly() throws Exception {
        FoDAviatorApplyRemediationsCommand command = parse("--from-cache", "remediations.zip", "--preview");
        assertTrue(getApplyOptions(command).isPreviewMode());
    }

    @Test
    void previewWorksWithIssueIds() throws Exception {
        FoDAviatorApplyRemediationsCommand command = parse("--from-cache", "cache.zip", "--preview", "--issue-ids", "ISSUE-1,ISSUE-2");
        assertTrue(getApplyOptions(command).isPreviewMode());
        assertEquals(2, getApplyOptions(command).getIssueIds().size());
    }

    @Test
    void previewWorksWithOnlineSelection() throws Exception {
        FoDAviatorApplyRemediationsCommand command = parse("--release", "123", "--preview");
        assertTrue(getApplyOptions(command).isPreviewMode());
    }

    private static FoDAviatorApplyRemediationsCommand parse(String... args) {
        FoDAviatorApplyRemediationsCommand command = new FoDAviatorApplyRemediationsCommand();
        new CommandLine(command).parseArgs(args);
        return command;
    }

    private static ApplyRemediationsOptionsMixin getApplyOptions(FoDAviatorApplyRemediationsCommand command)
            throws Exception {
        Field field = AbstractAviatorApplyRemediationsCommand.class.getDeclaredField("applyOptions");
        field.setAccessible(true);
        return (ApplyRemediationsOptionsMixin) field.get(command);
    }
}
