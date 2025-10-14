package com.fortify.cli.common.json.record.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class JsonNodeRecordProducer extends AbstractTransformingRecordProducer {
    @Getter private final JsonNode jsonNode;
    public static JsonNodeRecordProducer of(StandardOutputConfig cfg, JsonNode node) { return JsonNodeRecordProducer.builder().outputConfig(cfg).jsonNode(node).build(); }
    @Override protected void produceRawInputs(RawInputConsumer consumer) { consumer.accept(jsonNode); }
}
