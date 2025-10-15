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


import java.util.function.Consumer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.cli.mixin.TransformationPipelineRunnerConfigFactoryMixin;
import com.fortify.cli.common.output.writer.ISingularSupplier;

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
    private Consumer<ObjectNode> recordConsumer;
    private boolean suppressStdoutForRecordCollection;
    @Mixin private TransformationPipelineRunnerConfigFactoryMixin pipelineMixin;

    @Override
    public Integer call() {
        initialize();
        getOutputHelper().write(getObjectNodeProducer());
        return 0;
    }

    /**
     * Returns an IObjectNodeProducer for this command. Subclasses should override to provide their own producer.
     * For backward compatibility, this implementation checks for IBaseRequestSupplier and IJsonNodeSupplier,
     * and retrieves an appropriate producer from TransformationPipelineRunnerConfigFactoryMixin.
     */
    protected IObjectNodeProducer getObjectNodeProducer() {
        if (this instanceof IBaseRequestSupplier) {
            return pipelineMixin.getProducerFromRequest(((IBaseRequestSupplier) this).getBaseRequest());
        } else if (this instanceof IJsonNodeSupplier) {
            return pipelineMixin.getProducerFromJsonNode(((IJsonNodeSupplier) this).getJsonNode());
        } else {
            throw new FcliBugException(this.getClass().getName() + " must provide an IObjectNodeProducer");
        }
    }

    public abstract IOutputHelper getOutputHelper();

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
