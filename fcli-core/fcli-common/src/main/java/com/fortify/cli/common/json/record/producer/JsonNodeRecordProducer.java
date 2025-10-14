package com.fortify.cli.common.json.record.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

public class JsonNodeRecordProducer extends AbstractTransformingRecordProducer {
    private final JsonNode jsonNode;
    public JsonNodeRecordProducer(StandardOutputConfig outputConfig, JsonNode jsonNode) {
        super(outputConfig); this.jsonNode=jsonNode; }
    @Override
    protected void produceRawInputs(RawInputConsumer consumer) { consumer.accept(jsonNode); }
}
