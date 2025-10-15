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
package com.fortify.cli.common.json.producer.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Configuration object dedicated to {@link com.fortify.cli.common.json.producer.pipeline.TransformationPipelineRunner}.
 * It replaces the transformation & stage responsibilities that previously lived in {@link com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig}.
 * Legacy transformer registration methods are retained; they are wrapped into pipeline stages lazily the first time stages are accessed.
 */
@lombok.Builder
public final class TransformationPipelineRunnerConfig implements ITransformationPipelineRunnerConfig {
    private final List<JsonNodePipelineStage> inputStages = new ArrayList<>();
    private final List<JsonNodePipelineStage> recordStages = new ArrayList<>();
    public TransformationPipelineRunnerConfig inputTransformer(UnaryOperator<JsonNode> transformer) { return inputStage(wrap(transformer)); }
    public TransformationPipelineRunnerConfig recordTransformer(UnaryOperator<JsonNode> transformer) { return recordStage(wrap(transformer)); }
    public TransformationPipelineRunnerConfig recordTransformer(Function<JsonNode, JsonNode> transformer) { return recordStage(wrap(transformer)); }
    public TransformationPipelineRunnerConfig inputStage(JsonNodePipelineStage stage) { inputStages.add(stage); return this; }
    public TransformationPipelineRunnerConfig recordStage(JsonNodePipelineStage stage) { recordStages.add(stage); return this; }

    private JsonNodePipelineStage wrap(Function<JsonNode, JsonNode> fn) {
        return (ctx, n) -> JsonNodePipelineStage.TransformOutcome.continueWith(fn.apply(n));
    }

    @Override public Iterable<JsonNodePipelineStage> inputStages() { return inputStages; }
    @Override public Iterable<JsonNodePipelineStage> recordStages() { return recordStages; }
}
