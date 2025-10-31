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
package com.fortify.cli.fod._common.rest.query.cli.mixin;

import org.springframework.expression.Expression;

import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.rest.query.cli.mixin.AbstractServerSideQueryMixin;

import picocli.CommandLine.Option;

public class FoDFiltersParamMixin extends AbstractServerSideQueryMixin {
    @Option(names = "--filters-param", required = false, descriptionKey = "fcli.fod.filters-param")
    @MCPExclude // Not suitable for LLM, as LLM doesn't know option syntax/fields
    private String filtersParam;

    @Override
    protected String getServerSideQueryParamName() {
        return "filters";
    }

    @Override
    public String getServerSideQueryParamOptionValue() {
        return filtersParam;
    }

    public Expression getFilterExpression() {
        return super.getSpelExpression();
    }
}
