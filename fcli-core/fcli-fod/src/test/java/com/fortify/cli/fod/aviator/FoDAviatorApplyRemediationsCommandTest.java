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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator._common.util.AviatorIssueIdFilterUtils;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.fod.aviator.cmd.FoDAviatorApplyRemediationsCommand;

import picocli.CommandLine;

class FoDAviatorApplyRemediationsCommandTest {
    @Test
    void testSourceCodeDirectoryHasDefaultValue() throws Exception {
        FoDAviatorApplyRemediationsCommand command = new FoDAviatorApplyRemediationsCommand();

        Field field = FoDAviatorApplyRemediationsCommand.class.getDeclaredField("sourceCodeDirectory");
        field.setAccessible(true);
        String fieldValue = (String) field.get(command);

        assertNotNull(fieldValue,
            "sourceCodeDirectory must have default value to prevent NPE when --source-dir not specified");

        assertEquals(System.getProperty("user.dir"), fieldValue,
            "sourceCodeDirectory default should be current working directory");
    }

    @Test
    void testSourceCodeDirectoryCanBeOverridden() throws Exception {
        FoDAviatorApplyRemediationsCommand command = new FoDAviatorApplyRemediationsCommand();

        Field field = FoDAviatorApplyRemediationsCommand.class.getDeclaredField("sourceCodeDirectory");
        field.setAccessible(true);

        String customPath = "/custom/source/directory";
        field.set(command, customPath);

        String fieldValue = (String) field.get(command);

        assertEquals(customPath, fieldValue,
            "sourceCodeDirectory should be overridable when --source-dir option is provided");
    }

    @Test
    void testBlankSourceCodeDirectoryThrowsException() throws Exception {
        FoDAviatorApplyRemediationsCommand command = parse("--from-cache", "cache.zip");

        Field field = FoDAviatorApplyRemediationsCommand.class.getDeclaredField("sourceCodeDirectory");
        field.setAccessible(true);
        field.set(command, "");

        assertThrows(FcliSimpleException.class, command::getJsonNode,
            "Blank sourceCodeDirectory should throw FcliSimpleException");
    }

    @Test
    void testNormalizeIssueIdsTrimsAndDeduplicates() throws Exception {
        FoDAviatorApplyRemediationsCommand command = parse("--from-cache", "cache.zip", "--issue-ids", " ISSUE-1 , , ISSUE-2,ISSUE-1 ");
        assertEquals(Set.of("ISSUE-1", "ISSUE-2"), AviatorIssueIdFilterUtils.normalizeIssueIds(getIssueIds(command)));
    }

    @Test
    void testNormalizeIssueIdsRejectsOnlyBlankValues() throws Exception {
        FoDAviatorApplyRemediationsCommand command = parse("--from-cache", "cache.zip", "--issue-ids", " ,  , ");
        assertThrows(FcliSimpleException.class, () -> AviatorIssueIdFilterUtils.normalizeIssueIds(getIssueIds(command)));
    }

    @Test
    void testIssueIdsOptionParsesIntoField() throws Exception {
        FoDAviatorApplyRemediationsCommand command = parse("--from-cache", "cache.zip", "--issue-ids", "ISSUE-1,ISSUE-2");
        Field field = FoDAviatorApplyRemediationsCommand.class.getDeclaredField("issueIds");
        field.setAccessible(true);
        assertEquals(List.of("ISSUE-1", "ISSUE-2"), field.get(command));
    }

    @Test
    void testFromCacheParsesPath() throws Exception {
        FoDAviatorApplyRemediationsCommand command = parse("--from-cache", "remediations.zip");
        Field sourceSelectorField = FoDAviatorApplyRemediationsCommand.class.getDeclaredField("sourceSelector");
        sourceSelectorField.setAccessible(true);
        FoDAviatorApplyRemediationsCommand.SourceMixin sourceSelector =
                (FoDAviatorApplyRemediationsCommand.SourceMixin) sourceSelectorField.get(command);
        assertEquals(Path.of("remediations.zip"), sourceSelector.getFromCache());
        assertTrue(sourceSelector.isFromCacheSelected());
    }

    @Test
    void testReleaseAndFromCacheTogetherThrowsException() {
        assertThrows(CommandLine.ParameterException.class,
                () -> parse("--release", "1", "--from-cache", "local.zip"));
    }

    @Test
    void testIssueIdsWithoutFromCacheThrowsException() {
        FoDAviatorApplyRemediationsCommand command = parse("--release", "1", "--issue-ids", "ISSUE-1");

        assertThrows(FcliSimpleException.class, command::getJsonNode);
    }

    @Test
    void testOnlineReleaseSelectionParses() {
        FoDAviatorApplyRemediationsCommand command = parse("--release", "1");
        assertTrue(command != null);
    }

    private static FoDAviatorApplyRemediationsCommand parse(String... args) {
        FoDAviatorApplyRemediationsCommand command = new FoDAviatorApplyRemediationsCommand();
        new CommandLine(command).parseArgs(args);
        return command;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getIssueIds(FoDAviatorApplyRemediationsCommand command) throws Exception {
        Field field = FoDAviatorApplyRemediationsCommand.class.getDeclaredField("issueIds");
        field.setAccessible(true);
        return (List<String>) field.get(command);
    }
}
