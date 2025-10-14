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
package com.fortify.cli.common.output.transform.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.spel.query.QueryExpression;

/**
 * Pipeline stage that filters records (or inputs) using a QueryExpression. If
 * the expression doesn't match, the current node is skipped.
 */
public class QueryFilterStage implements JsonNodePipelineStage {
    private final QueryExpression queryExpression;
    public QueryFilterStage(QueryExpression queryExpression) {
        this.queryExpression = queryExpression;
    }
    @Override
    public TransformOutcome apply(TransformContext ctx, JsonNode node) {
        if (node == null || queryExpression == null) {
            return TransformOutcome.continueWith(node);
        }
        return queryExpression.matches(node) ? TransformOutcome.continueWith(node) : TransformOutcome.skip();
    }
}
