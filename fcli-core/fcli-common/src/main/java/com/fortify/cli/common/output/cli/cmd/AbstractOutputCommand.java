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
import com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom;
import com.fortify.cli.common.json.producer.RequestObjectNodeProducer;
import com.fortify.cli.common.json.producer.RequestObjectNodeProducer.RequestObjectNodeProducerBuilder;
import com.fortify.cli.common.json.producer.SimpleObjectNodeProducer;
import com.fortify.cli.common.json.producer.SimpleObjectNodeProducer.SimpleObjectNodeProducerBuilder;
import com.fortify.cli.common.json.producer.StreamingObjectNodeProducer;
import com.fortify.cli.common.json.producer.StreamingObjectNodeProducer.StreamingObjectNodeProducerBuilder;
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
        getOutputHelper().write(getObjectNodeProducer());
        return 0;
    }

    /**
     * Returns an IObjectNodeProducer for this command. Subclasses should override to provide their own producer.
     * Default implementation builds either a request-based or simple JSON-node producer depending on implemented interfaces.
     */
    protected IObjectNodeProducer getObjectNodeProducer() {
        return getLegacyObjectNodeProducer();
    }

    protected final IObjectNodeProducer getLegacyObjectNodeProducer() {
        if ( this instanceof IBaseRequestSupplier brs ) {
            return buildRequestProducer(brs.getBaseRequest());
        } else if ( this instanceof IJsonNodeSupplier jns) {
            return buildJsonNodeProducer(jns.getJsonNode());
        }
        throw new FcliBugException(this.getClass().getName()+" must provide an IObjectNodeProducer");
    }

    private IObjectNodeProducer buildJsonNodeProducer(JsonNode node) {
        return simpleObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                .source(node).build();
    }

    private IObjectNodeProducer buildRequestProducer(HttpRequest<?> baseRequest) {
        return requestObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                .baseRequest(baseRequest).build();
    }

    /**
    * Convenience method to create and configure a {@link SimpleObjectNodeProducer.SimpleObjectNodeProducerBuilder}.
    * This sets the {@code commandHelper}, then invokes {@link SimpleObjectNodeProducer.SimpleObjectNodeProducerBuilder#applyAllFrom(com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom)}.
     * @param applyFrom Enum indicating what sources to apply (SPEC, PRODUCT, NONE)
     * @return Partially configured builder instance
     */
    protected final SimpleObjectNodeProducerBuilder<?, ?> simpleObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom applyFrom) {
        return SimpleObjectNodeProducer.builder()
                .commandHelper(getCommandHelper())
                .applyAllFrom(applyFrom);
    }

    /**
    * Convenience method to create and configure a {@link RequestObjectNodeProducer.RequestObjectNodeProducerBuilder}.
    * This sets the {@code commandHelper}, then invokes {@link RequestObjectNodeProducer.RequestObjectNodeProducerBuilder#applyAllFrom(com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom)}.
     * @param applyFrom Enum indicating what sources to apply (SPEC, PRODUCT, NONE)
     * @return Partially configured builder instance
     */
    protected final RequestObjectNodeProducerBuilder<?, ?> requestObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom applyFrom) {
        return RequestObjectNodeProducer.builder()
                .commandHelper(getCommandHelper())
                .applyAllFrom(applyFrom);
    }
    
    /**
    * Convenience method to create and configure a {@link StreamingObjectNodeProducer.StreamingObjectNodeProducerBuilder}.
    * This sets the {@code commandHelper}, then invokes {@link StreamingObjectNodeProducer.StreamingObjectNodeProducerBuilder#applyAllFrom(com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom)}.
     * @param applyFrom Enum indicating what sources to apply (SPEC, PRODUCT, NONE)
     * @return Partially configured builder instance
     */
    protected final StreamingObjectNodeProducerBuilder<?, ?> streamingObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom applyFrom) {
        return StreamingObjectNodeProducer.builder()
                .commandHelper(getCommandHelper())
                .applyAllFrom(applyFrom);
    }

    public abstract IOutputHelper getOutputHelper();

    // IRecordCollectionSupport
    @Override
    public final void setRecordConsumer(Consumer<ObjectNode> consumer, boolean suppressStdout) {
        this.recordConsumer = consumer;
        this.stdoutSuppressedForRecordCollection = suppressStdout;
    }
}
