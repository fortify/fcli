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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;

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

    @Test
    void testMutualExclusivityBetweenArtifactIdAndLatest() throws Exception {
        AviatorSSCApplyRemediationsCommand command = new AviatorSSCApplyRemediationsCommand();

        Field artifactIdField = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("artifactId");
        artifactIdField.setAccessible(true);
        artifactIdField.set(command, "12345");

        Field latestField = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("latest");
        latestField.setAccessible(true);
        latestField.set(command, true);

        Method validateMethod = AviatorSSCApplyRemediationsCommand.class.getDeclaredMethod("validateOptions");
        validateMethod.setAccessible(true);

        FcliSimpleException exception = assertThrows(FcliSimpleException.class,
            () -> {
                try {
                    validateMethod.invoke(command);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            },
            "Using both --artifact-id and --latest should throw FcliSimpleException");

        assertTrue(exception.getMessage().contains("mutually exclusive"),
            "Error message should mention mutual exclusivity");
    }

    @Test
    void testMutualExclusivityBetweenArtifactIdAndAllOpenIssues() throws Exception {
        AviatorSSCApplyRemediationsCommand command = new AviatorSSCApplyRemediationsCommand();

        Field artifactIdField = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("artifactId");
        artifactIdField.setAccessible(true);
        artifactIdField.set(command, "12345");

        Field allOpenIssuesField = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("allOpenIssues");
        allOpenIssuesField.setAccessible(true);
        allOpenIssuesField.set(command, true);

        Method validateMethod = AviatorSSCApplyRemediationsCommand.class.getDeclaredMethod("validateOptions");
        validateMethod.setAccessible(true);

        FcliSimpleException exception = assertThrows(FcliSimpleException.class,
            () -> {
                try {
                    validateMethod.invoke(command);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            },
            "Using both --artifact-id and --all-open-issues should throw FcliSimpleException");

        assertTrue(exception.getMessage().contains("mutually exclusive"),
            "Error message should mention mutual exclusivity");
    }

    @Test
    void testAllOpenIssuesRequiresAppVersion() throws Exception {
        AviatorSSCApplyRemediationsCommand command = new AviatorSSCApplyRemediationsCommand();

        Field allOpenIssuesField = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("allOpenIssues");
        allOpenIssuesField.setAccessible(true);
        allOpenIssuesField.set(command, true);

        Field appVersionResolverField = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("appVersionResolver");
        appVersionResolverField.setAccessible(true);
        SSCAppVersionResolverMixin.OptionalOption mockResolver = new SSCAppVersionResolverMixin.OptionalOption() {
            @Override
            public String getAppVersionNameOrId() {
                return null;
            }
        };
        appVersionResolverField.set(command, mockResolver);

        Method validateMethod = AviatorSSCApplyRemediationsCommand.class.getDeclaredMethod("validateOptions");
        validateMethod.setAccessible(true);

        FcliSimpleException exception = assertThrows(FcliSimpleException.class,
            () -> {
                try {
                    validateMethod.invoke(command);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            },
            "Using --all-open-issues without --av should throw FcliSimpleException");

        assertTrue(exception.getMessage().contains("--av/--appversion is required"),
            "Error message should indicate app version is required for --all-open-issues");
    }

    @Test
    void testValidationPassesWithAllOpenIssuesAndAppVersion() throws Exception {
        AviatorSSCApplyRemediationsCommand command = new AviatorSSCApplyRemediationsCommand();

        Field allOpenIssuesField = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("allOpenIssues");
        allOpenIssuesField.setAccessible(true);
        allOpenIssuesField.set(command, true);

        Field appVersionResolverField = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("appVersionResolver");
        appVersionResolverField.setAccessible(true);
        SSCAppVersionResolverMixin.OptionalOption mockResolver = new SSCAppVersionResolverMixin.OptionalOption() {
            @Override
            public String getAppVersionNameOrId() {
                return "MyApp:main";
            }
        };
        appVersionResolverField.set(command, mockResolver);

        Method validateMethod = AviatorSSCApplyRemediationsCommand.class.getDeclaredMethod("validateOptions");
        validateMethod.setAccessible(true);

        // Should not throw any exception
        validateMethod.invoke(command);
    }

    @Test
    void testEitherArtifactIdOrLatestRequired() throws Exception {
        AviatorSSCApplyRemediationsCommand command = new AviatorSSCApplyRemediationsCommand();

        Method validateMethod = AviatorSSCApplyRemediationsCommand.class.getDeclaredMethod("validateOptions");
        validateMethod.setAccessible(true);

        FcliSimpleException exception = assertThrows(FcliSimpleException.class,
            () -> {
                try {
                    validateMethod.invoke(command);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            },
            "Not providing any option should throw FcliSimpleException");

        assertTrue(exception.getMessage().contains("One of --artifact-id, --latest, or --all-open-issues must be specified"),
            "Error message should indicate one of the options is required");
    }

    @Test
    void testLatestRequiresAppVersion() throws Exception {
        AviatorSSCApplyRemediationsCommand command = new AviatorSSCApplyRemediationsCommand();

        Field latestField = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("latest");
        latestField.setAccessible(true);
        latestField.set(command, true);

        Field appVersionResolverField = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("appVersionResolver");
        appVersionResolverField.setAccessible(true);
        SSCAppVersionResolverMixin.OptionalOption mockResolver = new SSCAppVersionResolverMixin.OptionalOption() {
            @Override
            public String getAppVersionNameOrId() {
                return null;
            }
        };
        appVersionResolverField.set(command, mockResolver);

        Method validateMethod = AviatorSSCApplyRemediationsCommand.class.getDeclaredMethod("validateOptions");
        validateMethod.setAccessible(true);

        FcliSimpleException exception = assertThrows(FcliSimpleException.class,
            () -> {
                try {
                    validateMethod.invoke(command);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            },
            "Using --latest without --av should throw FcliSimpleException");

        assertTrue(exception.getMessage().contains("--av/--appversion is required"),
            "Error message should indicate app version is required for --latest");
    }

    @Test
    void testValidationPassesWithArtifactId() throws Exception {
        AviatorSSCApplyRemediationsCommand command = new AviatorSSCApplyRemediationsCommand();

        Field artifactIdField = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("artifactId");
        artifactIdField.setAccessible(true);
        artifactIdField.set(command, "12345");

        Method validateMethod = AviatorSSCApplyRemediationsCommand.class.getDeclaredMethod("validateOptions");
        validateMethod.setAccessible(true);

        // Should not throw any exception
        validateMethod.invoke(command);
    }

    @Test
    void testValidationPassesWithLatestAndAppVersion() throws Exception {
        AviatorSSCApplyRemediationsCommand command = new AviatorSSCApplyRemediationsCommand();

        Field latestField = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("latest");
        latestField.setAccessible(true);
        latestField.set(command, true);

        Field appVersionResolverField = AviatorSSCApplyRemediationsCommand.class.getDeclaredField("appVersionResolver");
        appVersionResolverField.setAccessible(true);
        SSCAppVersionResolverMixin.OptionalOption mockResolver = new SSCAppVersionResolverMixin.OptionalOption() {
            @Override
            public String getAppVersionNameOrId() {
                return "MyApp:main";
            }
        };
        appVersionResolverField.set(command, mockResolver);

        Method validateMethod = AviatorSSCApplyRemediationsCommand.class.getDeclaredMethod("validateOptions");
        validateMethod.setAccessible(true);

        // Should not throw any exception
        validateMethod.invoke(command);
    }
}
