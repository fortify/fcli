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
import com.fortify.cli.common.json.producer.pipeline.TransformationPipelineRunner;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JsonNodeRecordProducer implements IObjectNodeProducer {
    @Getter private final StandardOutputConfig recordProducerConfig;
    private final JsonNode jsonNode;
    private final TransformationPipelineRunner runner;
    public static JsonNodeRecordProducer of(StandardOutputConfig cfg, JsonNode node) { return new JsonNodeRecordProducer(cfg, node); }
    public JsonNodeRecordProducer(StandardOutputConfig recordProducerConfig, JsonNode jsonNode) {
        this.recordProducerConfig = recordProducerConfig;
        this.jsonNode = jsonNode;
        this.runner = new TransformationPipelineRunner(recordProducerConfig);
    }
    @Override
    public void forEach(IObjectNodeProducer.IObjectNodeConsumer consumer) { runner.process(jsonNode, consumer::accept); }
    public JsonNode jsonNode() { return jsonNode; }
}
