/*
 * Copyright 2021-2025 Open Text.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.exception.FcliSimpleException;

class AviatorSSCApplyRemediationsCommandTest {
    @Test
    void testSourceCodeDirectoryHasDefaultValue() throws Exception {
        AviatorSSCApplyRemediationsCommand command = new AviatorSSCApplyRemediationsCommand();

        Field field = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("sourceCodeDirectory");
        field.setAccessible(true);
        String fieldValue = (String) field.get(command);

        assertNotNull(fieldValue,
            "sourceCodeDirectory must have default value to prevent NPE when --source-dir not specified");

        assertEquals(System.getProperty("user.dir"), fieldValue,
            "sourceCodeDirectory default should be current working directory");
    }

    @Test
    void testSourceCodeDirectoryCanBeOverridden() throws Exception {
        AviatorSSCApplyRemediationsCommand command = new AviatorSSCApplyRemediationsCommand();

        Field field = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("sourceCodeDirectory");
        field.setAccessible(true);

        String customPath = "/custom/source/directory";
        field.set(command, customPath);

        String fieldValue = (String) field.get(command);

        assertEquals(customPath, fieldValue,
            "sourceCodeDirectory should be overridable when --source-dir option is provided");
    }

    @Test
    void testBlankSourceCodeDirectoryThrowsException() throws Exception {
        AviatorSSCApplyRemediationsCommand command = new AviatorSSCApplyRemediationsCommand();

        Field field = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("sourceCodeDirectory");
        field.setAccessible(true);
        field.set(command, "");

        assertThrows(FcliSimpleException.class, () -> command.getJsonNode(null),
            "Blank sourceCodeDirectory should throw FcliSimpleException");
    }
}
