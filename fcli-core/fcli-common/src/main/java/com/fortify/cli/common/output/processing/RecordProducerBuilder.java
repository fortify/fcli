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
import com.fortify.cli.common.json.record.IRecordProducer;
import com.fortify.cli.common.json.record.producer.JsonNodeRecordProducer;
import com.fortify.cli.common.json.record.producer.RequestRecordProducer;
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

    public static IRecordProducer forRequest(StandardOutputConfig cfg, HttpRequest<?> request,
            INextPageRequestProducer nextPageRequestProducer, INextPageUrlProducer nextPageUrlProducer) {
        return RequestRecordProducer.builder().outputConfig(cfg).initialRequest(request).nextPageRequestProducer(nextPageRequestProducer)
                .nextPageUrlProducer(nextPageUrlProducer).build();
    }

    public static IRecordProducer forJsonNode(StandardOutputConfig cfg, JsonNode node) {
        return JsonNodeRecordProducer.builder().outputConfig(cfg).jsonNode(node).build();
    }

    public static IRecordProducer forJsonNodeHolder(StandardOutputConfig cfg, JsonNodeHolder holder) {
        return forJsonNode(cfg, holder.asJsonNode());
    }
}
