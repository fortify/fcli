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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.util.Break;

/**
 * Consumer interfaces for specific JsonNode subtypes. These allow producers to
 * signal more specific types without casting at call sites. Each consumer returns a
 * {@link com.fortify.cli.common.util.Break} to allow short-circuiting iteration.
 */
public final class JsonNodeConsumers {
    private JsonNodeConsumers() {}
    @FunctionalInterface public interface JsonNodeConsumer { Break accept(JsonNode node); }
    @FunctionalInterface public interface ObjectNodeConsumer { Break accept(ObjectNode node); }
    @FunctionalInterface public interface ArrayNodeConsumer { Break accept(ArrayNode node); }
}
