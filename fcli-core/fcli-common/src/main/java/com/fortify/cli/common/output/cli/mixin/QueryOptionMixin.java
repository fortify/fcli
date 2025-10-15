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
package com.fortify.cli.common.output.cli.mixin;

import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.spel.query.QueryExpression;
import com.fortify.cli.common.spel.query.QueryExpressionTypeConverter;

import lombok.Getter;
import picocli.CommandLine.Option;

public final class QueryOptionMixin {
    @Option(names = {"-q", "--query"}, order = 1, converter = QueryExpressionTypeConverter.class, paramLabel = "<SpEL expression>")
    @MCPExclude // Not suitable for LLM, as LLM doesn't know option syntax/fields
    @Getter
    private QueryExpression queryExpression;
}
