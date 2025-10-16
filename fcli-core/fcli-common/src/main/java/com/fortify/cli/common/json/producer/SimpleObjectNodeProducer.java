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
package com.fortify.cli.common.json.producer;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * Producer that iterates over records from a pre-supplied {@link JsonNode}.
 */
public class SimpleObjectNodeProducer extends AbstractObjectNodeProducer {
    private final JsonNode source;
    protected SimpleObjectNodeProducer(SimpleObjectNodeProducerBuilder b) {
    super(b.inputTransformers, b.recordTransformers, b.queryExpression);
        this.source = b.source;
    }
    @Override public void forEach(IObjectNodeConsumer consumer) { process(source, consumer); }
    public static SimpleObjectNodeProducerBuilder builder() { return new SimpleObjectNodeProducerBuilder(); }
    public static class SimpleObjectNodeProducerBuilder extends AbstractObjectNodeProducerBuilder<SimpleObjectNodeProducer, SimpleObjectNodeProducerBuilder> {
        private JsonNode source;
        public SimpleObjectNodeProducerBuilder source(JsonNode source) { this.source = source; return self(); }
        @Override protected SimpleObjectNodeProducerBuilder self() { return this; }
        @Override public SimpleObjectNodeProducer build() { return new SimpleObjectNodeProducer(this); }
    }
}
