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
package com.fortify.cli.common.spel.wrapper;

import org.springframework.expression.Expression;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * <p>This is a simple wrapper class for a Spring {@link Expression}
 * instance. It's main use is in combination with 
 * {@link SimpleExpressionDeserializer} to allow automatic
 * conversion from String values to simple {@link Expression}
 * instances in JSON/YAML documents.</p>
 * 
 * <p>The reason for needing this wrapper class is to differentiate
 * with templated {@link Expression} instances that are handled 
 * by {@link TemplateExpressionSerializer}.</p>
 */
@JsonDeserialize(using = SimpleExpressionDeserializer.class)
@JsonSerialize(using = SimpleExpressionSerializer.class)
public class SimpleExpression extends WrappedExpression {
    public SimpleExpression(String originalExpressionString, Expression target) {
        super(originalExpressionString, target);
    }
}
