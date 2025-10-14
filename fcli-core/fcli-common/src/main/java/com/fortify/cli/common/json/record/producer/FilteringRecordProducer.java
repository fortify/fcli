package com.fortify.cli.common.json.record.producer;

import java.util.function.Predicate;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.record.IRecordConsumer;
import com.fortify.cli.common.json.record.IRecordProducer;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.common.spel.query.QueryExpression;

import lombok.Builder;

/**
 * Decorator that filters records based on either a Predicate or a QueryExpression.
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
        if ( r==null ) { return Break.FALSE; }
        if ( predicate!=null && !predicate.test(r) ) { return Break.FALSE; }
        if ( queryExpression!=null && !queryExpression.matches(r) ) { return Break.FALSE; }
        return consumer.accept(r);
    }
}
