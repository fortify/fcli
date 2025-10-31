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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.SampleYamlSnippets;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a 'with.cleanup' step allowing initialization and cleanup steps to
 * be run around a set of steps. 
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonTypeName("with")
@JsonClassDescription("Run the steps in the `do` block within the context of one or more writers or sessions.")
@SampleYamlSnippets(copyFrom = {ActionStepWithSession.class, ActionStepWithWriter.class})
public final class ActionStepWith extends AbstractActionElementIf {
    @JsonPropertyDescription("""
        This instruction allows for running a set of initialization steps before running the \
        steps specified in the do-block. Once the steps specified in the do-block have completed \
        either successfully or with failure, the steps specified in the given cleanup-block will \
        be run. 
        """)
    // TODO Do we want to provide this functionality? And what would be a proper name for this instruction?
    @JsonIgnore /* @JsonProperty(value = "cleanup", required = false) */ private ActionStepWithCleanup cleanup;
    
    @JsonPropertyDescription("""
        This instruction allows for setting up one or more sessions before running the steps \
        specified in the do-block, and logging out of those sessions once the steps in the do-block \
        have completed wither successfully or with failure.
        
        Note that for now, these sessions can only be referenced by explicitly specifying the --session \
        option on 'run.fcli' instructions. The 'rest.call' instructions and any SpEL functions that are \
        dependent on an fcli session will use the session that was specified through the --session \
        option on the 'fcli * action run' command; they will ignore sessions created through the \
        'with:session' instruction.
        """)
    @JsonProperty(value = "sessions", required = false) private List<ActionStepWithSession> sessions;
    
    @JsonPropertyDescription("""
        This instruction allows for setting up one or more record writers that can referenced by \
        writer.append steps in the associated do-block. After all steps in the do-block have been \
        executed, the writer will be closed. This instruction takes a map, with map keys defining \
        writer identifiers, and map values defining the writer configurations. The number of records \
        that have been appended to the current writer can be accessed through the 'writerId.count' \
        variable.
        """)
    @JsonProperty(value = "writers", required = false) private Map<String, ActionStepWithWriter> writers;
    
    // TODO Add property that allows for installing a shutdown hook
    
    @JsonPropertyDescription("""
        Required list of steps to be run within the context of the given configuration.
        """)
    @JsonProperty(value = "do", required = true) private List<ActionStep> _do;
    
    @Override
    public void postLoad(Action action) {
        Action.checkNotNull("do", this, _do);
    }
    
    
}