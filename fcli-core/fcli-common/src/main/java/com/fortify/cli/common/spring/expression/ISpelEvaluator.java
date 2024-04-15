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
package com.fortify.cli.common.spring.expression;

import org.springframework.expression.Expression;

public interface ISpelEvaluator {
    <R> R evaluate(Expression expression, Object input, Class<R> returnClass);
    <R> R evaluate(String expression, Object input, Class<R> returnClass);
}