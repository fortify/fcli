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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes a request target.
 */
@Reflectable @NoArgsConstructor
@Data
public final class ActionRequestTarget implements IActionElement {
    @JsonPropertyDescription("Required string: Request target name, referenceable from 'request' steps.")
    @JsonProperty(required = true) private String name;
    
    @JsonPropertyDescription("Required SpEL template expression: Base URL to use for REST requests to this request target.")
    @JsonProperty(required = true) private TemplateExpression baseUrl;
    
    @JsonPropertyDescription("Optional map(string,SpEL template expression): Headers to be sent to this request target on every request.")
    @JsonProperty(required = false) private Map<String, TemplateExpression> headers;
    
    // TODO Add support for next page URL producer
    // TODO ? Add proxy support ?
    
    public final void postLoad(Action action) {
        Action.checkNotBlank("request target name", name, this);
        Action.checkNotNull("request target base URL", baseUrl, this);
    }
}