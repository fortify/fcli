package com.fortify.cli.common.output.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.spel.query.QueryExpression;

/**
 * Record transformer that filters records using a QueryExpression. If the
 * record doesn't match, returns null so downstream pipeline skips it.
 */
public class QueryExpressionRecordFilter implements IRecordTransformer {
    private final QueryExpression queryExpression;
    public QueryExpressionRecordFilter(QueryExpression queryExpression) { this.queryExpression = queryExpression; }
    @Override
    public JsonNode transformRecord(JsonNode input) {
        if ( input==null || queryExpression==null ) { return input; }
        return queryExpression.matches(input) ? input : null;
    }
}
