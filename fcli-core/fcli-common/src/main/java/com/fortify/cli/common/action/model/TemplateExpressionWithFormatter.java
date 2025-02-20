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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.SpelHelper;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
public class TemplateExpressionWithFormatter extends AbstractActionElementIf {
    @JsonPropertyDescription("""
        The value to use for this instruction, may be specified as an SpEL template expression.
        """)
    @JsonProperty(value = "value", required = false) private TemplateExpression value;
    @JsonPropertyDescription("""
        The formatter to use for this instruction, may be used as an SpEL template expression. \
        If 'value' is specified, the given value will be provided as input for the formatter. \
        If 'value' is not specified, the set of current action variables will be provided as \
        input for the formatter.
        """)
    @JsonProperty(value = "fmt", required = false) private TemplateExpression fmt;
    
    /** Allow for deserializing from string rather than object */
    public TemplateExpressionWithFormatter(String valueString) {
        this.value = SpelHelper.parseTemplateExpression(valueString);
    }
    
    /** Allow for deserializing from boolean rather than object */
    public TemplateExpressionWithFormatter(boolean booleanValue) {
        this(String.format("${%s}", booleanValue));
    }
    // TODO Add extra constructors, for example for numeric values?
    
    @Override
    public void postLoad(Action action) {}
}
