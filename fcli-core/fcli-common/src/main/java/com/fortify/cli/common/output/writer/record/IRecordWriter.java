/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.common.output.writer.record;

import java.io.Closeable;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface IRecordWriter extends Closeable {
    void append(ObjectNode node);
    void close();
    /**
     * Set response-level metadata to be included in the output when envelope
     * style is active. Must be called before {@link #close()} for the metadata
     * to be written. Writers that do not support envelope style ignore this.
     */
    default void setResponseMetadata(ObjectNode metadata) {}
}
