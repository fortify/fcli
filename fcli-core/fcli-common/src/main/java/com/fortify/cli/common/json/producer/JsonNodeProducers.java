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

/**
 * Producer interfaces for iterating over JSON structures. These mirror legacy
 * {@link IRecordProducer} but allow binding via type-specific consumers without manual
 * type checks. Implementations should call the given consumer for every element and
 * stop iterating if the consumer returns {@link com.fortify.cli.common.util.Break#TRUE}.
 */
public final class JsonNodeProducers {
    private JsonNodeProducers() {}
    @FunctionalInterface public interface JsonNodeProducer { void forEach(JsonNodeConsumers.JsonNodeConsumer consumer); }
    @FunctionalInterface public interface ObjectNodeProducer { void forEach(JsonNodeConsumers.ObjectNodeConsumer consumer); }
    @FunctionalInterface public interface ArrayNodeProducer { void forEach(JsonNodeConsumers.ArrayNodeConsumer consumer); }
}
