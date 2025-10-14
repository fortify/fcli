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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class JsonNodeRecordProducer extends AbstractTransformingRecordProducer {
    @Getter
    private final JsonNode jsonNode;
    public static JsonNodeRecordProducer of(StandardOutputConfig cfg, JsonNode node) {
        return JsonNodeRecordProducer.builder().outputConfig(cfg).jsonNode(node).build();
    }
    @Override
    protected void produceRawInputs(RawInputConsumer consumer) {
        consumer.accept(jsonNode);
    }
}
