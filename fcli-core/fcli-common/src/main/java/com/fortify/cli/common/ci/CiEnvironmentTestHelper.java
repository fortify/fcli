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
package com.fortify.cli.common.ci;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fortify.cli.common.ci.ado.AdoEnvironment;
import com.fortify.cli.common.ci.bitbucket.BitbucketEnvironment;
import com.fortify.cli.common.ci.github.GitHubEnvironment;
import com.fortify.cli.common.ci.gitlab.GitLabEnvironment;

import lombok.Getter;

/**
 * Helper methods for tests to list all CI-related environment variable names,
 * allowing those to be explicitly cleared before running unit and functional tests.
 * This class is part of fcli production code to allow access by both unit tests
 * and functional tests.
 */
public final class CiEnvironmentTestHelper {
    private CiEnvironmentTestHelper() {}

    private static final List<Class<?>> CI_ENVIRONMENT_CLASSES = List.of(
        GitHubEnvironment.class,
        GitLabEnvironment.class,
        AdoEnvironment.class,
        BitbucketEnvironment.class
    );

    /**
     * Lazily cached immutable set of CI-related environment variable names.
     */
    @Getter(lazy = true)
    private static final Set<String> allCiEnvironmentVariableNames = Collections.unmodifiableSet(
        resolveAllEnvironmentVariableNames()
    );
    
    /**
     * Clear all CI-related environment variables by setting the fcli.env.* system properties
     * to an empty string. Note that this only works for in-process environment variable resolution,
     * i.e., when running unit tests.
     */
    public static final void clearAllCiEnvironmentVariables() {
        getAllCiEnvironmentVariableNames()
            .forEach(envVarName -> System.setProperty("fcli.env." + envVarName, ""));
    }

    private static Set<String> resolveAllEnvironmentVariableNames() {
        var envVarNames = new LinkedHashSet<String>();
        for ( Class<?> environmentClass : CI_ENVIRONMENT_CLASSES ) {
            collectEnvironmentVariableNames(environmentClass, envVarNames);
        }
        return envVarNames;
    }

    private static void collectEnvironmentVariableNames(Class<?> environmentClass, Set<String> target) {
        for ( Field field : environmentClass.getDeclaredFields() ) {
            if ( Modifier.isStatic(field.getModifiers())
                    && Modifier.isFinal(field.getModifiers())
                    && field.getType().equals(String.class)
                    && field.getName().startsWith("ENV_") ) {
                try {
                    field.setAccessible(true);
                    var value = (String)field.get(null);
                    if ( value != null ) {
                        target.add(value);
                    }
                } catch ( IllegalAccessException e ) {
                    throw new IllegalStateException("Unable to access CI environment field "+field.getName(), e);
                }
            }
        }
    }
}
