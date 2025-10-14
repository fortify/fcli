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
package com.fortify.cli.common.json.record.producer;

import java.util.function.Predicate;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.record.IRecordConsumer;
import com.fortify.cli.common.json.record.IRecordProducer;
import com.fortify.cli.common.spel.query.QueryExpression;
import com.fortify.cli.common.util.Break;

import lombok.Builder;

/**
 * Decorator that filters records based on either a Predicate or a
 * QueryExpression.
 */
@Builder
public class FilteringRecordProducer implements IRecordProducer {
    private final IRecordProducer delegate;
    private final Predicate<ObjectNode> predicate;
    private final QueryExpression queryExpression;

    @Override
    public void forEach(IRecordConsumer consumer) {
        delegate.forEach(r -> filter(r, consumer));
    }

    private Break filter(ObjectNode r, IRecordConsumer consumer) {
        if (r == null) {
            return Break.FALSE;
        }
        if (predicate != null && !predicate.test(r)) {
            return Break.FALSE;
        }
        if (queryExpression != null && !queryExpression.matches(r)) {
            return Break.FALSE;
        }
        return consumer.accept(r);
    }
}
