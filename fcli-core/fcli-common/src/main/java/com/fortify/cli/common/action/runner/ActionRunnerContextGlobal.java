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
package com.fortify.cli.common.action.runner;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.ActionStepCheckEntry;
import com.fortify.cli.common.action.model.ActionStepCheckEntry.CheckStatus;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;

import lombok.Getter;
import lombok.Setter;

/**
 * Holds global (shared) state for an action execution. A single instance is shared
 * across the root {@link ActionRunnerContext} and all its children.
 */
@Getter
final class ActionRunnerContextGlobal {
    private final ObjectMapper objectMapper = JsonHelper.getObjectMapper();
    private final ActionRunnerConfig config;
    private final IProgressWriterI18n progressWriter;
    private final ObjectNode parameterValues;
    private final Map<ActionStepCheckEntry, CheckStatus> checkStatuses = new LinkedHashMap<>();
    @Setter private int exitCode = 0;
    @Setter private boolean exitRequested = false;

    ActionRunnerContextGlobal(ActionRunnerConfig config, IProgressWriterI18n progressWriter, ObjectNode parameterValues) {
        this.config = config;
        this.progressWriter = progressWriter;
        this.parameterValues = parameterValues;
    }
}
