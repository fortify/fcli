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

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.transform.fields.SelectedFieldsTransformer;
import com.fortify.cli.common.json.transform.flatten.FlattenTransformer;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

//TODO Do proper exception handling instead of @SneakyThrows
@RequiredArgsConstructor
public abstract class AbstractRecordWriter<T> implements IRecordWriter {
    @Getter(value = AccessLevel.PRIVATE, lazy=true) private final Writer writer = createWriter();
    private T out;
    private Function<ObjectNode, ObjectNode> recordFormatter;
    
    public abstract RecordWriterConfig getConfig();
    protected abstract Function<ObjectNode, ObjectNode> createRecordFormatter(ObjectNode objectNode) throws IOException;
    protected abstract T createOut(Writer writer, ObjectNode formattedRecord)  throws IOException;
    protected abstract void append(T out, ObjectNode formattedRecord)  throws IOException;
    protected abstract void close(T out)  throws IOException; 
    
    @Override @SneakyThrows
    public final void append(ObjectNode record) {
        var formattedRecord = getRecordFormatter(record).apply(record);
        append(getOut(formattedRecord), formattedRecord);
    }

    @Override @SneakyThrows
    public final void close() {
        Writer writer = null;
        try {
            writer = getWriter();
            if ( out==null) {
                closeWithNoData(writer);
            } else {
                writer.flush();
                close(out); // This should also close the writer
            }
        } finally {
            // Code above is supposed to close the writer, but just in case it doesn't:
            if ( writer!=null ) {
                try { writer.close(); } catch (Exception e) {}
            }
        }
    }
    
    protected abstract void closeWithNoData(Writer writer) throws IOException;
    
    protected final Function<ObjectNode, ObjectNode> createStructuredOutputTransformer(boolean flatten, Function<String,String> propertyNameFormatter) {
        if ( StringUtils.isNotBlank(getConfig().getArgs()) ) {
            return createSelectedFieldsTransformer(); // This already flattens, so no need to flatten again
        } else if ( flatten ) {
            return createFlattenTransformer(propertyNameFormatter);
        }
        return Function.identity();
    }
    
    protected final Function<ObjectNode, ObjectNode> createFlattenTransformer(Function<String,String> propertyNameFormatter) {
        return new FlattenTransformer(propertyNameFormatter, ".", false)::transformObjectNode;
    }

    protected final Function<ObjectNode, ObjectNode> createSelectedFieldsTransformer() {
        return new SelectedFieldsTransformer(getConfig().getArgs(), false)::transformObjectNode;
    }
    
    @SneakyThrows
    private final Function<ObjectNode, ObjectNode> getRecordFormatter(ObjectNode record) {
        if ( recordFormatter==null ) {
            recordFormatter = createRecordFormatter(record);
        }
        return recordFormatter;
    }
    
    @SneakyThrows
    private final T getOut(ObjectNode formattedRecord) {
        if ( out==null ) {
            out = createOut(getWriter(), formattedRecord);
        }
        return out;
    }
    
    @SneakyThrows
    private final Writer createWriter() {
        return getConfig().getWriterSupplier().get();
    }
}
