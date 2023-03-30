package com.fortify.cli.common.rest.query;

import org.springframework.expression.Expression;

public interface IServerSideQueryParamValueGenerator {
    String getServerSideQueryParamValue(Expression expression);
}