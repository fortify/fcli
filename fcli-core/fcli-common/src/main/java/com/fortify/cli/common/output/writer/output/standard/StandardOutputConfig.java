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
package com.fortify.cli.common.output.writer.output.standard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.producer.pipeline.JsonNodePipelineStage;
import com.fortify.cli.common.output.processing.IRecordProducerConfig;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
// TODO Add null checks in case any input or record transformation returns null?
public class StandardOutputConfig implements IRecordProducerConfig {
    @Getter @Setter private RecordWriterFactory defaultFormat;
    // Legacy transformer lists retained for backward compatibility; wrapped into
    // pipeline stages lazily
    private final List<Function<JsonNode, JsonNode>> inputTransformers = new ArrayList<>();
    private final List<Function<JsonNode, JsonNode>> recordTransformers = new ArrayList<>();
    // New unified stage lists
    private final List<JsonNodePipelineStage> inputStages = new ArrayList<>();
    private final List<JsonNodePipelineStage> recordStages = new ArrayList<>();

    public final StandardOutputConfig inputTransformer(UnaryOperator<JsonNode> transformer) {
        inputTransformers.add(transformer);
        return this;
    }

    public final StandardOutputConfig recordTransformer(UnaryOperator<JsonNode> transformer) {
        recordTransformers.add(transformer);
        return this;
    }
    public final StandardOutputConfig recordTransformer(Function<JsonNode, JsonNode> transformer) {
        recordTransformers.add(transformer);
        return this;
    }

    // New stage registration methods
    public final StandardOutputConfig inputStage(JsonNodePipelineStage stage) {
        inputStages.add(stage);
        return this;
    }
    public final StandardOutputConfig recordStage(JsonNodePipelineStage stage) {
        recordStages.add(stage);
        return this;
    }

    private void ensureLegacyTransformersWrapped() {
        if (!inputTransformers.isEmpty()) {
            inputTransformers.forEach(t -> inputStages.add((ctx, n) -> JsonNodePipelineStage.TransformOutcome.continueWith(t.apply(n))));
            inputTransformers.clear();
        }
        if (!recordTransformers.isEmpty()) {
            recordTransformers.forEach(t -> recordStages.add((ctx, n) -> JsonNodePipelineStage.TransformOutcome.continueWith(t.apply(n))));
            recordTransformers.clear();
        }
    }

    @Override
    public final List<JsonNodePipelineStage> inputStages() {
        ensureLegacyTransformersWrapped();
        return inputStages;
    }
    @Override
    public final List<JsonNodePipelineStage> recordStages() {
        ensureLegacyTransformersWrapped();
        return recordStages;
    }

    public static final StandardOutputConfig csv() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.csv);
    }

    public static final StandardOutputConfig json() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.json);
    }

    public static final StandardOutputConfig table() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.table);
    }

    public static final StandardOutputConfig xml() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.xml);
    }

    public static final StandardOutputConfig yaml() {
        return new StandardOutputConfig().defaultFormat(RecordWriterFactory.yaml);
    }

    public static final StandardOutputConfig details() {
        return yaml();
    }
}
