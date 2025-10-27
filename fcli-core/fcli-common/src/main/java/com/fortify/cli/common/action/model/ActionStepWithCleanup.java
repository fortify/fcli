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
package com.fortify.cli.common.action.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a 'with:cleanup' step allowing initialization and cleanup steps to
 * be run around a set of steps. 
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonTypeName("with-cleanup")
@JsonClassDescription("Define steps to run before and after the steps defined in the `do` block.")
public final class ActionStepWithCleanup extends AbstractActionElementIf {
    // TODO Add property that allows for installing a shutdown hook
    @JsonPropertyDescription("""
        Required list of initialization steps to be run before the steps in the do-block will be \
        run. If initialization fails, the steps in the do-block will not be run, but steps in the \
        cleanup-block will still be run.
        """)
    @JsonProperty(value = "init", required = true) private List<ActionStep> initSteps;
    
    @JsonPropertyDescription("""
        Required list of cleanup steps. These steps will run even if the initialization steps or \
        the steps in the do-block terminated with a failure.
        """)
    @JsonProperty(value = "cleanup", required = true) private List<ActionStep> cleanupSteps;
    
    @Override
    public void postLoad(Action action) {
        Action.checkNotNull("init", this, initSteps);
        Action.checkNotNull("cleanup", this, cleanupSteps);
    }
}