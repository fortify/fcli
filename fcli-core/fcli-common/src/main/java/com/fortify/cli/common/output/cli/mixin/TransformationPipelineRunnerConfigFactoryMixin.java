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
package com.fortify.cli.common.output.cli.mixin;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.RequestObjectNodeProducer;
import com.fortify.cli.common.json.producer.SimpleObjectNodeProducer;
import com.fortify.cli.common.json.producer.pipeline.QueryFilterStage;
import com.fortify.cli.common.json.producer.pipeline.TransformationPipelineRunnerConfig;
import com.fortify.cli.common.json.transform.fields.AddFieldsTransformer;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.common.output.product.NoOpProductHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.paging.INextPageRequestProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.util.JavaHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;

/**
 * Non-abstract mixin responsible for building the {@link TransformationPipelineRunnerConfig}
 * and handling HTTP request update & paging concerns. This mixin is intentionally
 * independent from {@link AbstractOutputHelperMixin}; commands can combine both
 * as needed.
 */
public class TransformationPipelineRunnerConfigFactoryMixin {

    /**
     * Returns an IObjectNodeProducer for the given HttpRequest, using the current output and pipeline config.
     */
    public IObjectNodeProducer getProducerFromRequest(HttpRequest<?> baseRequest) {
        HttpRequest<?> request = updateRequest(baseRequest);
        var nextPageRequestProducer = getNextPageRequestProducer();
        var nextPageUrlProducer = nextPageRequestProducer == null ? getNextPageUrlProducer() : null;
        // Build producer using builder for potential future builder customizations
    return RequestObjectNodeProducer.RequestObjectNodeProducerBuilder.builder()
        .initialRequest(request)
        .productHelper(getProductHelper())
        .nextPageRequestProducer(nextPageRequestProducer)
        .nextPageUrlProducer(nextPageUrlProducer)
        .applyFromSpec(commandHelper.getCommandSpec())
        .build();
    }

    /**
     * Returns an IObjectNodeProducer for the given JsonNode, using the current output and pipeline config.
     */
    public IObjectNodeProducer getProducerFromJsonNode(JsonNode node) {
    return SimpleObjectNodeProducer.builder()
        .source(node)
        .productHelper(getProductHelper())
        .applyFromSpec(commandHelper.getCommandSpec())
        .build();
    }
    @Getter @Mixin private CommandHelperMixin commandHelper;

    public IProductHelper getProductHelper() {
        return commandHelper.getCommandAs(IProductHelperSupplier.class).map(IProductHelperSupplier::getProductHelper)
                .orElse(NoOpProductHelper.instance());
    }

    // ----- Public API -----
    public final IObjectNodeProducer createProducerForJsonNode(JsonNode node) {
        return forJsonNode(getPipelineConfig(), node);
    }
    public final IObjectNodeProducer createProducerForJsonNodeHolder(JsonNodeHolder holder) {
        return forJsonNodeHolder(getPipelineConfig(), holder);
    }

    // ----- Pipeline Config -----
    public final TransformationPipelineRunnerConfig getPipelineConfig() {
        var pipelineCfg = TransformationPipelineRunnerConfig.builder().build();
        addInputTransformersForCommand(pipelineCfg);
        addRecordTransformersForCommand(pipelineCfg);
        addQueryStage(pipelineCfg);
        addCommandActionResultRecordTransformer(pipelineCfg, commandHelper.getCommand());
        return pipelineCfg;
    }

    // ----- Request / Paging helpers -----
    protected final HttpRequest<?> updateRequest(HttpRequest<?> request) {
        request = applyWithDefault(getProductHelper(), IHttpRequestUpdater.class, httpRequestUpdater(request), request);
        for (var mixin : commandHelper.getCommandSpec().mixins().values()) {
            request = applyWithDefault(mixin.userObject(), IHttpRequestUpdater.class, httpRequestUpdater(request), request);
        }
        request = applyWithDefault(commandHelper.getCommand(), IHttpRequestUpdater.class, httpRequestUpdater(request), request);
        return request;
    }
    protected final INextPageRequestProducer getNextPageRequestProducer() {
        return PagingHelper.asNextPageRequestProducer(getUnirestInstance(), getNextPageUrlProducer());
    }
    protected final INextPageUrlProducer getNextPageUrlProducer() {
        return Stream.of(commandHelper.getCommand(), getProductHelper()).map(TransformationPipelineRunnerConfigFactoryMixin::getNextPageUrlProducerFromObject)
                .filter(Objects::nonNull).findFirst().orElse(null);
    }
    protected final UnirestInstance getUnirestInstance() {
        return Stream.of(commandHelper.getCommand()).map(TransformationPipelineRunnerConfigFactoryMixin::getUnirestInstanceFromObject).filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    // ----- Transformer registration -----
    protected final void addRecordTransformersForCommand(TransformationPipelineRunnerConfig cfg) {
        FcliCommandSpecHelper.getAllMixinsStream(commandHelper.getCommandSpec())
            .map(m -> m.userObject())
            .forEach(o->addRecordTransformersFromObject(cfg, o));
        addRecordTransformersFromObject(cfg, getProductHelper());
        addRecordTransformersFromObject(cfg, commandHelper.getCommand());
    }
    protected final void addInputTransformersForCommand(TransformationPipelineRunnerConfig cfg) {
        FcliCommandSpecHelper.getAllMixinsStream(commandHelper.getCommandSpec())
            .map(m -> m.userObject())
            .forEach(o->addInputTransformersFromObject(cfg, o));
        addInputTransformersFromObject(cfg, getProductHelper());
        addInputTransformersFromObject(cfg, commandHelper.getCommand());
    }
    
    // ----- Query registration -----
    private final void addQueryStage(TransformationPipelineRunnerConfig cfg) {
        for (var stage : cfg.recordStages()) { if (stage instanceof QueryFilterStage) { return; } }
        FcliCommandSpecHelper.getQueryExpression(commandHelper.getCommandSpec())
            .ifPresent(qe -> cfg.recordStage(new QueryFilterStage(qe)));
    }

    // ----- Internal helpers (duplicated intentionally for independence) -----
    private static final Function<IHttpRequestUpdater, HttpRequest<?>> httpRequestUpdater(final HttpRequest<?> request) {
        return requestUpdater -> requestUpdater.updateRequest(request);
    }
    private static final INextPageUrlProducer getNextPageUrlProducerFromObject(Object obj) {
        return apply(obj, INextPageUrlProducerSupplier.class, supplier -> supplier.getNextPageUrlProducer());
    }
    private static final UnirestInstance getUnirestInstanceFromObject(Object obj) {
        return apply(obj, IUnirestInstanceSupplier.class, supplier -> supplier.getUnirestInstance());
    }
    private static final <T, R> R applyWithDefaultSupplier(Object obj, Class<T> type, Function<T, R> function,
            Supplier<R> defaultValueSupplier) {
        var result = JavaHelper.as(obj, type).map(function);
        if (defaultValueSupplier != null) { result = result.or(() -> Optional.of(defaultValueSupplier.get())); }
        return result.orElse(null);
    }
    private static final <T, R> R applyWithDefault(Object obj, Class<T> type, Function<T, R> function, R defaultValue) {
        return applyWithDefaultSupplier(obj, type, function, () -> defaultValue);
    }
    private static final <T, R> R apply(Object obj, Class<T> type, Function<T, R> function) {
        return applyWithDefaultSupplier(obj, type, function, null);
    }
    private static final void addRecordTransformersFromObject(TransformationPipelineRunnerConfig cfg, Object obj) {
        apply(obj, IRecordTransformer.class, s -> cfg.recordTransformer(s::transformRecord));
    }
    private static final void addInputTransformersFromObject(TransformationPipelineRunnerConfig cfg, Object obj) {
        apply(obj, IInputTransformer.class, s -> cfg.inputTransformer(s::transformInput));
    }
    private static final void addCommandActionResultRecordTransformer(TransformationPipelineRunnerConfig cfg, Object obj) {
        apply(obj, IActionCommandResultSupplier.class, s -> cfg.recordTransformer(createCommandActionResultRecordTransformer(s)));
    }
    private static final UnaryOperator<JsonNode> createCommandActionResultRecordTransformer(IActionCommandResultSupplier supplier) {
        return new AddFieldsTransformer(IActionCommandResultSupplier.actionFieldName, supplier.getActionCommandResult())
                .overwiteExisting(false)::transform;
    }

    public static IObjectNodeProducer forJsonNode(TransformationPipelineRunnerConfig pipelineCfg, JsonNode node) {
        // Retain static factory for backward compatibility with existing code paths
        return SimpleObjectNodeProducer.builder().source(node).build();
    }

    public static IObjectNodeProducer forJsonNodeHolder(TransformationPipelineRunnerConfig pipelineCfg, JsonNodeHolder holder) {
        return forJsonNode(pipelineCfg, holder.asJsonNode());
    }

    public static IObjectNodeProducer forRequest(TransformationPipelineRunnerConfig pipelineCfg, HttpRequest<?> request,
        INextPageRequestProducer nextPageRequestProducer, INextPageUrlProducer nextPageUrlProducer) {
    return RequestObjectNodeProducer.RequestObjectNodeProducerBuilder.builder()
        .initialRequest(request)
        .nextPageRequestProducer(nextPageRequestProducer)
        .nextPageUrlProducer(nextPageUrlProducer)
        .build();
    }

    // --- New builder applier logic (initial version, focuses on transformers/query only) ---
    // Reflection-based applyFromSpec removed; builders now expose applyFromSpec directly
}
