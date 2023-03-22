package com.fortify.cli.common.output.cli.mixin.impl;

import com.fortify.cli.common.output.query.IQueryExpressionSupplier;
import com.fortify.cli.common.output.query.QueryExpression;
import com.fortify.cli.common.output.query.QueryExpressionTypeConverter;

import lombok.Getter;
import picocli.CommandLine;

public final class QueryOptionsArgGroup implements IQueryExpressionSupplier {
    @CommandLine.Option(names = {"-q", "--query"}, order=1, converter = QueryExpressionTypeConverter.class, paramLabel = "<SpEL expression>")
    @Getter private QueryExpression queryExpression;
}