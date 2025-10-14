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
import com.fortify.cli.common.json.producer.JsonNodeProducers.ObjectNodeProducer;
import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.writer.ISingularSupplier;

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
    private static final List<Class<?>> supportedInterfaces = Arrays.asList(IBaseRequestSupplier.class, IJsonNodeSupplier.class,
        ObjectNodeProducerSupplier.class);
    @Override
    public final Integer call() {
        initialize();
        IOutputHelper outputHelper = getOutputHelper();
        if (isInstance(IBaseRequestSupplier.class)) {
            outputHelper.write(((IBaseRequestSupplier) this).getBaseRequest());
        } else if (isInstance(IJsonNodeSupplier.class)) {
            outputHelper.write(((IJsonNodeSupplier) this).getJsonNode());
        } else if (isInstance(ObjectNodeProducerSupplier.class)) {
            ObjectNodeProducer rp = ((ObjectNodeProducerSupplier) this).getObjectNodeProducer();
            outputHelper.write(rp);
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
