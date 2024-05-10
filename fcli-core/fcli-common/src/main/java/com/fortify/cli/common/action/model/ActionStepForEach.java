/**
 * Copyright 2023 Open Text.
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a forEach element, allowing iteration over the output of
 * a given input.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper=true)
public final class ActionStepForEach extends AbstractActionStepForEach {
    /** Processor that runs the forEach steps. This expression must evaluate to an
     *  IActionStepForEachProcessor instance. */
    @JsonPropertyDescription("Required if 'values' not specified: Reference to a processor that generates JSON records and executes the given steps for each record. For now, the only available processor is ${#ssc.ruleDescriptionsProcessor(parameters.appversion.id)} for processing rule descriptions from the current state FPR file, used by some built-in actions.")
    @JsonProperty(required = false) private TemplateExpression processor;
    
    @JsonPropertyDescription("Required if 'processor' not specified: Array of values to be iterated over.")
    @JsonProperty(required = false) private TemplateExpression values;
    
    public final void _postLoad(Action action) {}
    
    @FunctionalInterface
    public static interface IActionStepForEachProcessor {
        /** Implementations of this method should invoke the given function for every
         *  JsonNode to be processed, and terminate processing if the given function
         *  returns false. */ 
        public void process(Function<JsonNode, Boolean> consumer);
    }
}