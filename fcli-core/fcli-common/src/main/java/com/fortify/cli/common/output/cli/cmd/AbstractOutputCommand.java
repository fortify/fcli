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
package com.fortify.cli.common.output.cli.cmd;

import java.util.Arrays;
import java.util.List;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.IObjectNodeProducerSupplier;
import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.cli.mixin.TransformationPipelineRunnerConfigFactoryMixin;
import com.fortify.cli.common.output.writer.ISingularSupplier;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

import picocli.CommandLine.Mixin;

/**
 * Base class for commands producing output. A concrete command must implement
 * exactly one of:
 * <ul>
 * <li>{@link IBaseRequestSupplier} - to execute an HTTP request</li>
 * <li>{@link IJsonNodeSupplier} - to supply a JsonNode directly</li>
 * <li>{@link ObjectNodeProducerSupplier} - to stream individual records</li>
 * </ul>
 */
public abstract class AbstractOutputCommand extends AbstractRunnableCommand
        implements
            ISingularSupplier,
            IOutputHelperSupplier,
            IRecordCollectionSupport {
    private java.util.function.Consumer<com.fasterxml.jackson.databind.node.ObjectNode> recordConsumer;
    private boolean suppressStdoutForRecordCollection;
    @Mixin private TransformationPipelineRunnerConfigFactoryMixin transformationPipelineRunnerConfigFactoryMixin;
    private static final List<Class<?>> supportedInterfaces = Arrays.asList(
            IBaseRequestSupplier.class, IJsonNodeSupplier.class, IObjectNodeProducerSupplier.class);
    @Override
    public final Integer call() {
        initialize();
        IOutputHelper outputHelper = getOutputHelper();
        var pipelineMixin = getTransformationPipelineRunnerConfigFactoryMixin();
        StandardOutputConfig outputCfg = outputHelper.getBasicOutputConfig();
        var writerFactory = outputHelper.getOutputWriterFactory();
        if (isInstance(IBaseRequestSupplier.class)) {
            pipelineMixin.writeRequest(outputCfg, ((IBaseRequestSupplier) this).getBaseRequest(), writerFactory);
        } else if (isInstance(IJsonNodeSupplier.class)) {
            pipelineMixin.writeJsonNode(outputCfg, ((IJsonNodeSupplier) this).getJsonNode(), writerFactory);
        } else if (isInstance(IObjectNodeProducerSupplier.class)) {
            IObjectNodeProducer rp = ((IObjectNodeProducerSupplier) this).getObjectNodeProducer();
            // Provided producer assumed already constructed with correct pipeline config elsewhere; if not, wrap?
            pipelineMixin.writeProducer(outputCfg, rp, writerFactory);
        } else {
            throw new FcliBugException(this.getClass().getName() + " must implement exactly one of " + supportedInterfaces);
        }
        return 0;
    }

    private boolean isInstance(Class<?> clazz) {
        return clazz.isAssignableFrom(this.getClass())
                && supportedInterfaces.stream().filter(c -> !c.equals(clazz)).noneMatch(c -> c.isAssignableFrom(this.getClass()));
    }

    public abstract IOutputHelper getOutputHelper();
    protected TransformationPipelineRunnerConfigFactoryMixin getTransformationPipelineRunnerConfigFactoryMixin() { return transformationPipelineRunnerConfigFactoryMixin; }

    // IRecordCollectionSupport
    @Override
    public final void setRecordConsumer(java.util.function.Consumer<com.fasterxml.jackson.databind.node.ObjectNode> consumer,
            boolean suppressStdout) {
        this.recordConsumer = consumer;
        this.suppressStdoutForRecordCollection = suppressStdout;
    }
    @Override
    public final java.util.function.Consumer<com.fasterxml.jackson.databind.node.ObjectNode> getRecordConsumer() {
        return recordConsumer;
    }
    @Override
    public final boolean isStdoutSuppressedForRecordCollection() {
        return suppressStdoutForRecordCollection;
    }
}
