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
package com.fortify.cli.common.output.writer.record.impl;

import java.io.IOException;
import java.io.Writer;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.transform.PropertyPathFormatter;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractRecordWriterJackson<T extends JsonGenerator> extends AbstractRecordWriter<T> {
    @Override
    protected void append(T out, ObjectNode formattedRecord) throws IOException {
        out.writeTree(formattedRecord);
    }
    
    @Override
    protected Function<ObjectNode, ObjectNode> createRecordFormatter(ObjectNode objectNode) throws IOException {
        // For JSON, we optionally flatten, converting the dot-separated property path to camel case
        return createStructuredOutputTransformer(getConfig().getStyle().isFlat(), PropertyPathFormatter::camelCase);
    }
    
    @Override
    protected void close(T out) throws IOException {
        writeEnd(out);
        out.close();
    }
    
    @Override
    protected void closeWithNoData(Writer writer) throws IOException {
        var out = createGenerator(writer);
        writeStart(out);
        writeEnd(out);
        writer.flush();
        out.close();
    }
    
    @Override
    protected T createOut(Writer writer, ObjectNode formattedRecord) throws IOException {
        if ( formattedRecord==null ) { return null; }
        var result = createGenerator(writer);
        writeStart(result);
        return result;
    }

    protected abstract T createGenerator(Writer writer) throws IOException;
    protected abstract void writeStart(T out) throws IOException;
    protected abstract void writeEnd(T out) throws IOException;
}
