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
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.JsonNodeRecordProducer;
import com.fortify.cli.common.json.producer.RequestRecordProducer;
import com.fortify.cli.common.json.producer.pipeline.QueryFilterStage;
import com.fortify.cli.common.json.producer.pipeline.TransformationPipelineRunnerConfig;
import com.fortify.cli.common.json.transform.fields.AddFieldsTransformer;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.common.output.product.NoOpProductHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.rest.paging.INextPageRequestProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.spel.query.IQueryExpressionSupplier;
import com.fortify.cli.common.util.JavaHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Non-abstract mixin responsible for building the {@link TransformationPipelineRunnerConfig}
 * and handling HTTP request update & paging concerns. This mixin is intentionally
 * independent from {@link AbstractOutputHelperMixin}; commands can combine both
 * as needed.
 */
public class TransformationPipelineRunnerConfigFactoryMixin {
    @Getter @Mixin private CommandHelperMixin commandHelper;

    public IProductHelper getProductHelper() {
        return commandHelper.getCommandAs(IProductHelperSupplier.class).map(IProductHelperSupplier::getProductHelper)
                .orElse(NoOpProductHelper.instance());
    }

    // ----- Public API -----
    public final void writeRequest(StandardOutputConfig outputCfg, HttpRequest<?> baseRequest, IOutputWriterFactory outputWriterFactory) {
        HttpRequest<?> request = updateRequest(baseRequest);
        var nextPageRequestProducer = getNextPageRequestProducer();
        var nextPageUrlProducer = nextPageRequestProducer == null ? getNextPageUrlProducer() : null;
        var pipelineCfg = getPipelineConfig(outputWriterFactory);
        IObjectNodeProducer producer = forRequest(outputCfg, pipelineCfg, request, nextPageRequestProducer, nextPageUrlProducer);
        createOutputWriter(outputWriterFactory, outputCfg).write(producer);
    }
    public final void writeProducer(StandardOutputConfig outputCfg, IObjectNodeProducer producer, IOutputWriterFactory outputWriterFactory) {
        createOutputWriter(outputWriterFactory, outputCfg).write(producer);
    }
    public final void writeJsonNode(StandardOutputConfig outputCfg, JsonNode node, IOutputWriterFactory outputWriterFactory) {
        writeProducer(outputCfg, createProducerForJsonNode(outputCfg, node, outputWriterFactory), outputWriterFactory);
    }
    public final IObjectNodeProducer createProducerForJsonNode(StandardOutputConfig outputCfg, JsonNode node, IOutputWriterFactory outputWriterFactory) {
        return forJsonNode(outputCfg, getPipelineConfig(outputWriterFactory), node);
    }
    public final IObjectNodeProducer createProducerForJsonNodeHolder(StandardOutputConfig outputCfg, com.fortify.cli.common.json.JsonNodeHolder holder, IOutputWriterFactory outputWriterFactory) {
        return forJsonNodeHolder(outputCfg, getPipelineConfig(outputWriterFactory), holder);
    }

    // ----- Pipeline Config -----
    public final TransformationPipelineRunnerConfig getPipelineConfig(IOutputWriterFactory outputWriterFactory) {
        Object cmd = commandHelper.getCommand();
        var pipelineCfg = TransformationPipelineRunnerConfig.builder().build();
        addInputTransformersForCommand(pipelineCfg, cmd);
        addRecordTransformersForCommand(pipelineCfg, cmd);
        addQueryStage(pipelineCfg, commandHelper.getCommandSpec(), cmd, outputWriterFactory);
        addCommandActionResultRecordTransformer(pipelineCfg, cmd);
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
    protected final void addRecordTransformersForCommand(TransformationPipelineRunnerConfig cfg, Object cmd) {
        for (var mixin : commandHelper.getCommandSpec().mixins().values()) { addRecordTransformersFromObject(cfg, mixin.userObject()); }
        addRecordTransformersFromObject(cfg, getProductHelper());
        addRecordTransformersFromObject(cfg, cmd);
        addCommandActionResultRecordTransformer(cfg, cmd);
    }
    protected final void addInputTransformersForCommand(TransformationPipelineRunnerConfig cfg, Object cmd) {
        for (var mixin : commandHelper.getCommandSpec().mixins().values()) { addInputTransformersFromObject(cfg, mixin.userObject()); }
        addInputTransformersFromObject(cfg, getProductHelper());
        addInputTransformersFromObject(cfg, cmd);
    }
    
    // ----- Query registration -----
    private static void addQueryStage(TransformationPipelineRunnerConfig cfg, CommandSpec commandSpec, Object command, Object outputWriterFactory) {
        for (var stage : cfg.recordStages()) { if (stage instanceof QueryFilterStage) { return; } }
        // Scan mixins
        for (var mixin : commandSpec.mixins().values()) {
            var o = mixin.userObject();
            if (addQueryStageFromObject(cfg, o)) {
                return;
            }
        }
        // Command
        if (addQueryStageFromObject(cfg, command)) {
            return;
        }
        // Writer factory (may hold arg groups)
        addQueryStageFromObject(cfg, outputWriterFactory);
    }
    private static boolean addQueryStageFromObject(TransformationPipelineRunnerConfig cfg, Object o) {
        if (o instanceof IQueryExpressionSupplier s && s.getQueryExpression() != null) {
            cfg.recordStage(new QueryFilterStage(s.getQueryExpression()));
            return true;
        }
        return false;
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
    @SuppressWarnings("deprecation")
    private static final void addRecordTransformersFromObject(TransformationPipelineRunnerConfig cfg, Object obj) {
        apply(obj, IRecordTransformer.class, s -> cfg.recordTransformer(s::transformRecord));
    }
    @SuppressWarnings("deprecation")
    private static final void addInputTransformersFromObject(TransformationPipelineRunnerConfig cfg, Object obj) {
        apply(obj, IInputTransformer.class, s -> cfg.inputTransformer(s::transformInput));
    }
    private static final void addCommandActionResultRecordTransformer(TransformationPipelineRunnerConfig cfg, Object cmd) {
        apply(cmd, IActionCommandResultSupplier.class, s -> cfg.recordTransformer(createCommandActionResultRecordTransformer(s)));
    }
    private static final UnaryOperator<JsonNode> createCommandActionResultRecordTransformer(IActionCommandResultSupplier supplier) {
        return new AddFieldsTransformer(IActionCommandResultSupplier.actionFieldName, supplier.getActionCommandResult())
                .overwiteExisting(false)::transform;
    }

    private static final IOutputWriter createOutputWriter(IOutputWriterFactory factory, StandardOutputConfig outputCfg) {
        return factory.createOutputWriter(outputCfg);
    }

    public static IObjectNodeProducer forJsonNode(StandardOutputConfig outputCfg, TransformationPipelineRunnerConfig pipelineCfg, JsonNode node) {
        return JsonNodeRecordProducer.of(pipelineCfg, node);
    }

    public static IObjectNodeProducer forJsonNodeHolder(StandardOutputConfig outputCfg, TransformationPipelineRunnerConfig pipelineCfg, JsonNodeHolder holder) {
        return forJsonNode(outputCfg, pipelineCfg, holder.asJsonNode());
    }

    public static IObjectNodeProducer forRequest(StandardOutputConfig outputCfg, TransformationPipelineRunnerConfig pipelineCfg, HttpRequest<?> request,
            INextPageRequestProducer nextPageRequestProducer, INextPageUrlProducer nextPageUrlProducer) {
    return new RequestRecordProducer(pipelineCfg, request, nextPageRequestProducer, nextPageUrlProducer);
    }
}
