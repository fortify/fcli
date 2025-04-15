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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes action usage header and description.
 */
@Reflectable @NoArgsConstructor
@Data
public final class ActionUsage implements IActionElement {
    @JsonPropertyDescription("Required string: Action usage header, displayed in list and help outputs")
    @JsonProperty(value = "header", required = true) private String header;
    
    @JsonPropertyDescription("""
        Required SpEL template expression: Action usage description, displayed in help output and online documentation. \
        The template expression can reference the 'isAsciiDoc' and 'isPlainText' properties to determine whether the \
        contents are to be rendered as AsciiDoc (for online documentation) or plain text (for help output). For internal \
        use only, the template expression can utilize the '#include('path/to/classpath/resource') function to include \
        contents of a class path resource. 
        """)
    @JsonProperty(value = "description", required = true) private TemplateExpression description;
    
    public void postLoad(Action action) {
        Action.checkNotBlank("usage header", header, this);
        Action.checkNotNull("usage description", description, this);
    }
}