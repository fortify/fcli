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

import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.SampleYamlSnippets;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a request target.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonTypeName("rest.target")
@JsonClassDescription("Define a (third-party) REST target against which REST calls can be executed.")
@SampleYamlSnippets("""
        steps:
        - rest.target:
            example:
                baseUrl: https://example.com/my-app
                headers:
                Authorization: ${exampleAuth}
        """)
public final class ActionStepRestTargetEntry extends AbstractActionElementIf {
    @JsonPropertyDescription("""
        Required SpEL template expression: Base URL to use for REST requests to this request target.
        """)
    @JsonProperty(value = "baseUrl", required = true) private TemplateExpression baseUrl;
    
    @JsonPropertyDescription("""
        Optional map(string,SpEL template expression): Headers to be sent to this request target on every request.
        """)
    @JsonProperty(value = "headers", required = false) private LinkedHashMap<String, TemplateExpression> headers;
    
    // TODO Add support for next page URL producer
    // TODO ? Add proxy support ?
    
    public final void postLoad(Action action) {
        Action.checkNotNull("request target base URL", baseUrl, this);
    }
}