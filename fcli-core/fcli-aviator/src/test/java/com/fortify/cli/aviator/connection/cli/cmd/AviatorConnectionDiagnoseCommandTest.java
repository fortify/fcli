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
package com.fortify.cli.aviator.connection.cli.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.connection.cli.mixin.AviatorConnectionDiagnoseSourceArgGroup;

import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

class AviatorConnectionDiagnoseCommandTest {
    @Test
    void parseAllowsUrlWithOptionalToken() throws ReflectiveOperationException {
        var cmd = parse("--url", "aviator.example.com", "--token", "string:abc");
        var sourceArgGroup = getSourceArgGroup(cmd);

        assertNotNull(sourceArgGroup.getUrlSource());
        assertEquals("aviator.example.com", sourceArgGroup.getUrlSource().getUrl());
        assertEquals("string:abc", sourceArgGroup.getUrlSource().getTextSource());
        assertNull(sourceArgGroup.getAviatorSession());
        assertNull(sourceArgGroup.getAdminConfig());
    }

    @Test
    void parseAllowsAviatorSessionAlias() throws ReflectiveOperationException {
        var cmd = parse("--av-session", "default");
        var sourceArgGroup = getSourceArgGroup(cmd);

        assertEquals("default", sourceArgGroup.getAviatorSession());
        assertNull(sourceArgGroup.getUrlSource());
        assertNull(sourceArgGroup.getAdminConfig());
    }

    @Test
    void parseRejectsMultipleSourceModes() {
        assertThrows(ParameterException.class,
            () -> parse("--url", "aviator.example.com", "--aviator-session", "default"));
    }

    private static AviatorConnectionDiagnoseCommand parse(String... args) {
        var cmd = new AviatorConnectionDiagnoseCommand();
        new CommandLine(cmd).parseArgs(args);
        return cmd;
    }

    private static AviatorConnectionDiagnoseSourceArgGroup getSourceArgGroup(AviatorConnectionDiagnoseCommand cmd)
            throws ReflectiveOperationException {
        Field field = AviatorConnectionDiagnoseCommand.class.getDeclaredField("sourceArgGroup");
        field.setAccessible(true);
        return (AviatorConnectionDiagnoseSourceArgGroup) field.get(cmd);
    }
}