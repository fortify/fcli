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

import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.SampleYamlSnippets;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a forEach element, allowing iteration over the output of
 * a given input.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper=true)
@JsonInclude(Include.NON_NULL)
@JsonTypeName("for-each")
@JsonClassDescription("Repeat the steps listed in the `do` block for each record provided by the `from` instruction.")
@SampleYamlSnippets("""
        steps:
          - records.for-each:
              from: ${collection}
              record.var-name: currentRecord
              do:
                - log.debug: ${currentRecord}
                - ...
        """)
public final class ActionStepRecordsForEach extends AbstractActionElementForEachRecord {
    @JsonPropertyDescription("""
        Required SpEL template expression, evaluating to either an array of values to be iterated over, \
        or an IActionStepForEachProcessor instance like returned by ${#ssc.ruleDescriptionsProcessor(appVersionId)}. \
        For each of the records in the given array or as produced by the IActionStepForEachProcessor, \
        the steps given in the 'do' instruction will be executed until the breakIf condition (is specified) \
        evaluates to true. The steps in the 'do' instruction may reference the current record through the \
        action variable name specified through 'record.var-name'.
        """)
    @JsonProperty(value = "from", required = true) private TemplateExpression from;
    
    public final void _postLoad(Action action) {
        Action.checkNotNull("from", action, this);
    }
    
    
    @FunctionalInterface
    public static interface IActionStepForEachProcessor {
        /** Implementations of this method should invoke the given function for every
         *  JsonNode to be processed, and terminate processing if the given function
         *  returns false. */ 
        public void process(Function<JsonNode, Boolean> consumer);
    }
}