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
package com.fortify.cli.common.action.helper.ci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.runner.ActionRunnerConfig;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;

public class ActionCiSpelFunctionsRegistryTest {
    @Test
    void testActionRunnerConfigRegistersCiInfoVariablesForCliDefaults() {
        var config = ActionRunnerConfig.builder()
            .action(new Action())
            .progressWriter(NoOpProgressWriterI18n.INSTANCE)
            .onValidationErrors(r -> new RuntimeException("validation"))
            .actionContextConfigurers(Collections.emptyList())
            .actionConfigSpelEvaluatorConfigurers(Collections.emptyList())
            .actionContextSpelEvaluatorConfigurers(Collections.emptyList())
            .defaultFcliRunOptions(Collections.emptyMap())
            .build();

        var spelEvaluator = config.getSpelEvaluator();
        assertEquals("ado", spelEvaluator.evaluate("#ado.type", null, String.class));
        assertEquals("github", spelEvaluator.evaluate("#github.type", null, String.class));
        assertEquals("gitlab", spelEvaluator.evaluate("#gitlab.type", null, String.class));
        assertEquals("bitbucket", spelEvaluator.evaluate("#bitbucket.type", null, String.class));

        // Should be safe to evaluate during CLI option parsing even when no CI env is detected.
        assertNull(spelEvaluator.evaluate("#ado.env?.project", null, String.class));
    }

    private enum NoOpProgressWriterI18n implements IProgressWriterI18n {
        INSTANCE;

        @Override
        public boolean isMultiLineSupported() {
            return false;
        }

        @Override
        public void writeProgress(String message, Object... args) {}

        @Override
        public void writeInfo(String message, Object... args) {}

        @Override
        public void writeInfoWithException(String message, Throwable cause, Object... args) {}

        @Override
        public void writeWarning(String message, Object... args) {}

        @Override
        public void writeWarningWithException(String message, Throwable cause, Object... args) {}

        @Override
        public void clearProgress() {}

        @Override
        public void close() {}

        @Override
        public void writeI18nProgress(String keySuffix, Object... args) {}

        @Override
        public void writeI18nWarning(String keySuffix, Object... args) {}

        @Override
        public String type() {
            return "test";
        }
    }
}
