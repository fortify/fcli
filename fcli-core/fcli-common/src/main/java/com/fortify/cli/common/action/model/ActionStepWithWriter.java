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

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.SampleYamlSnippets;
import com.fortify.cli.common.json.JsonPropertyDescriptionAppend;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.output.writer.record.RecordWriterStyle;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a 'with:session' step, making a session available for the
 * steps specified in the do-block, and cleaning up the session afterwards.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonTypeName("with-writer")
@JsonClassDescription("Define a writer that can be referenced by steps in the `do` block, automatically closing the writer once the steps in the `do` block have completed.")
@SampleYamlSnippets("""
    steps:
        - with:
            writers:
            csvWriter:
                to: ${cli.file}
            type: csv
        do:
            - records.for-each:
                from: ${records}
                record.var-name: record
                do:
                - writer.append: 
                    csvWriter: ${record}
        """)
public final class ActionStepWithWriter extends AbstractActionElementIf {
    @JsonPropertyDescription("""
        Required SpEL template expression; destination where to write the output of this writer. \
        Destination can be specified as one of the following: 
        
        - A file name to write the output to
        - 'stdout' to write the output to stdout
        - 'stderr' to write the output to stderr
        - 'var:varName' to write the output as text into the given 'varName' action variable  
        
        With 'var:varName', the given variable name will be available to steps after the current \
        'with' block has completed. 
        """)
    @JsonProperty(value = "to", required = true) private TemplateExpression to;
    
    @JsonPropertyDescription("""
        Required SpEL template expression defining the writer type. The evaluated \
        expression must evaluate to one of the following types:
        """)
    @JsonPropertyDescriptionAppend(RecordWriterFactory.class)
    @JsonProperty(value = "type", required = true) private TemplateExpression type;
    
    @JsonPropertyDescription("""
        Optional SpEL template expression defining arguments for the given writer type. \
        In most cases, it's much easier to just pass an already formatted object to \
        the 'writer.append' step; this 'type-args' instruction is just meant to provide \
        feature parity with the fcli '--output type=args' command line option. See \
        fcli documentation for details on supported options for the various writer types.
        """)
    @JsonProperty(value = "type-args", required = false) private TemplateExpression typeArgs;
    
    @JsonPropertyDescription("""
        Optional SpEL template expression defining the writer style. If specified, \
        the expression should evaluate to a comma-separated list of style elements \
        to be applied to the output. Supported style elements:
        """)
    @JsonPropertyDescriptionAppend(RecordWriterStyle.RecordWriterStyleElement.class)
    @JsonProperty(value = "style", required = false) private TemplateExpression style;
    
    @Override
    public void postLoad(Action action) {
        // TODO This doesn't seem to get visited; no exception is thrown if one of these fields is not defined
        Action.checkNotNull("to", this, to);
        Action.checkNotNull("type", this, type);
    }
    
    
}