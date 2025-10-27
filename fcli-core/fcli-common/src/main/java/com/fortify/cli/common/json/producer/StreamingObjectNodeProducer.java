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
package com.fortify.cli.common.json.producer;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.util.Break;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Streaming producer that iterates over records provided by a {@link Stream} of {@link ObjectNode}.
 * Unlike {@link SimpleObjectNodeProducer}, this avoids collecting nodes into an intermediate ArrayNode.
 */
@SuperBuilder
public class StreamingObjectNodeProducer extends AbstractObjectNodeProducer {
    /** Supplier for lazily obtaining a stream of ObjectNode instances. */
    @Getter private final Supplier<Stream<ObjectNode>> streamSupplier;

    @Override
    public void forEach(IObjectNodeConsumer consumer) {
        try ( var stream = Objects.requireNonNull(streamSupplier.get(), "streamSupplier returned null") ) {
            var it = stream.iterator();
            while ( it.hasNext() ) {
                var node = it.next();
                if ( node!=null && Break.TRUE == processSingleRecord(node, consumer) ) { break; }
            }
        }
    }
}