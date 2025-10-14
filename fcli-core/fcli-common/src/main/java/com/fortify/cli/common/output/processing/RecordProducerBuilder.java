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
package com.fortify.cli.common.output.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.JsonNodeRecordProducer;
import com.fortify.cli.common.json.producer.RequestRecordProducer;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.rest.paging.INextPageRequestProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;

import kong.unirest.HttpRequest;

/**
 * Central factory for creating record producers, decoupling producer
 * construction from {@code AbstractOutputHelperMixin} so that mixin focuses
 * only on output concerns.
 */
public final class RecordProducerBuilder {
    private RecordProducerBuilder() {
    }

    public static IObjectNodeProducer forRequest(StandardOutputConfig outputCfg, TransformationPipelineRunnerConfig pipelineCfg, HttpRequest<?> request,
            INextPageRequestProducer nextPageRequestProducer, INextPageUrlProducer nextPageUrlProducer) {
    return new RequestRecordProducer(pipelineCfg, request, nextPageRequestProducer, nextPageUrlProducer);
    }

    public static IObjectNodeProducer forJsonNode(StandardOutputConfig outputCfg, TransformationPipelineRunnerConfig pipelineCfg, JsonNode node) {
        return JsonNodeRecordProducer.of(pipelineCfg, node);
    }

    public static IObjectNodeProducer forJsonNodeHolder(StandardOutputConfig outputCfg, TransformationPipelineRunnerConfig pipelineCfg, JsonNodeHolder holder) {
        return forJsonNode(outputCfg, pipelineCfg, holder.asJsonNode());
    }
}
