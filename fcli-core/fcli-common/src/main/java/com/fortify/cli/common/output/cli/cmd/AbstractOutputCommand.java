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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.RequestObjectNodeProducer;
import com.fortify.cli.common.json.producer.RequestObjectNodeProducer.RequestObjectNodeProducerBuilder;
import com.fortify.cli.common.json.producer.SimpleObjectNodeProducer;
import com.fortify.cli.common.json.producer.SimpleObjectNodeProducer.SimpleObjectNodeProducerBuilder;
import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.writer.ISingularSupplier;

import kong.unirest.HttpRequest;
import lombok.Getter;

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
        implements ISingularSupplier, IOutputHelperSupplier, IRecordCollectionSupport 
{
    @Getter private Consumer<ObjectNode> recordConsumer;
    @Getter private boolean stdoutSuppressedForRecordCollection;

    @Override
    public Integer call() {
        initialize();
        getOutputHelper().write(getObjectNodeProducer());
        return 0;
    }

    /**
     * Returns an IObjectNodeProducer for this command. Subclasses should override to provide their own producer.
     * Default implementation builds either a request-based or simple JSON-node producer depending on implemented interfaces.
     */
    protected IObjectNodeProducer getObjectNodeProducer() {
        if ( this instanceof IBaseRequestSupplier brs ) {
            return buildRequestProducer(brs.getBaseRequest());
        } else if ( this instanceof IJsonNodeSupplier jns) {
            return buildJsonNodeProducer(jns.getJsonNode());
        }
        throw new FcliBugException(this.getClass().getName()+" must provide an IObjectNodeProducer");
    }

    private IObjectNodeProducer buildJsonNodeProducer(JsonNode node) {
        return simpleObjectNodeProducerBuilder(true).source(node).build();
    }

    private IObjectNodeProducer buildRequestProducer(HttpRequest<?> initialRequest) {
        return requestObjectNodeProducerBuilder(true).initialRequest(initialRequest).build();
    }

    /**
     * Convenience method to create and configure a {@link SimpleObjectNodeProducer.SimpleObjectNodeProducerBuilder}.
     * This sets the {@code commandHelper}, and if {@code applyFromSpec} is true, {@link SimpleObjectNodeProducer.SimpleObjectNodeProducerBuilder#applyAllFromSpec()} is invoked.
     * @param applyAllFromSpec Whether to invoke {@code applyFromSpec()} on the builder
     * @return Partially configured builder instance
     */
    protected final SimpleObjectNodeProducerBuilder<?, ?> simpleObjectNodeProducerBuilder(boolean applyAllFromSpec) {
        var b = SimpleObjectNodeProducer.builder().commandHelper(getCommandHelper());
        if ( applyAllFromSpec ) { b.applyAllFromSpec(); }
        return b;
    }

    /**
     * Convenience method to create and configure a {@link RequestObjectNodeProducer.RequestObjectNodeProducerBuilder}.
     * This sets the {@code commandHelper}, and if {@code applyFromSpec} is true, {@link RequestObjectNodeProducer.RequestObjectNodeProducerBuilder#applyAllFromSpec()} is invoked.
     * @param applyAllFromSpec Whether to invoke {@code applyFromSpec()} on the builder
     * @return Partially configured builder instance
     */
    protected final RequestObjectNodeProducerBuilder<?, ?> requestObjectNodeProducerBuilder(boolean applyAllFromSpec) {
        var b = RequestObjectNodeProducer.builder().commandHelper(getCommandHelper());
        if ( applyAllFromSpec ) { b.applyAllFromSpec(); }
        return b;
    }

    public abstract IOutputHelper getOutputHelper();

    // IRecordCollectionSupport
    @Override
    public final void setRecordConsumer(Consumer<ObjectNode> consumer, boolean suppressStdout) {
        this.recordConsumer = consumer;
        this.stdoutSuppressedForRecordCollection = suppressStdout;
    }
}
