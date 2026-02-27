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
package com.fortify.cli.common.action.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spel.SpelHelper;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Model class for messages with optional cause/exception.
 * Used by throw and log.* instructions to support both simple string messages
 * and structured messages with exception causes.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonTypeName("msg-and-cause")
@JsonClassDescription("""
        A message with optional exception cause. This can be supplied as either a simple \
        SpEL template expression (in which case it represents the message text), or as a \
        structured object with `msg` and optional `cause` properties. Used by `throw` and \
        `log.*` instructions.
        """)
public final class MessageWithCause extends AbstractActionStepElement  {
    
    /** Allow for deserializing from a string that specified the message */
    public MessageWithCause(String msgString) {
        this(SpelHelper.parseTemplateExpression(msgString));
    }
    
    public MessageWithCause(TemplateExpression msg) {
        this.msg = msg;
    }
    
    @JsonPropertyDescription("""
        Message text, specified as an SpEL template expression.
        """)
    @JsonProperty(value = "msg", required = false) 
    private TemplateExpression msg;
    
    @JsonPropertyDescription("""
        Optional exception cause, specified as an SpEL template expression that evaluates to a Throwable.
        Can be a direct Throwable reference, wrapped in POJONode, or an ObjectNode with a `pojo` property \
        (e.g., `${lastException}` or `${lastException.pojo}`). If only cause is specified (no msg), the 
        exception will be rethrown if it's an FcliException, otherwise wrapped.
        """)
    @JsonProperty(value = "cause", required = false) 
    private TemplateExpression cause;

    @Override
    public void postLoad(Action action) {}
}
