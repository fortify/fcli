/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.common.action.runner.processor.writer.record.impl;

import java.io.Writer;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fortify.cli.common.action.runner.processor.writer.record.IRecordWriter;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterConfig;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterStyles;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterStyles.RecordWriterStyle;
import com.fortify.cli.common.json.JsonHelper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

// TODO Do proper exception handling instead of @neakyThrows
@RequiredArgsConstructor
public class RecordWriterCsv implements IRecordWriter {
    private final RecordWriterStyles styles;
    private final RecordWriterConfig config;
    private Writer writer;
    private CsvGenerator generator;
    
    @Override @SneakyThrows
    public final void append(ObjectNode record) {
        var transformedRecord = transformRecord(record);
        getGenerator(transformedRecord).writeTree(transformedRecord);
    }
    
    private ObjectNode transformRecord(ObjectNode record) {
        var headers = config.getHeaders();
        if ( headers==null ) { return record; }
        var result = JsonHelper.getObjectMapper().createObjectNode();
        headers.entrySet().forEach(e->result.set(e.getValue(), record.get(e.getKey())));
        return result;
    }

    @Override @SneakyThrows
    public final void close() {
        if ( generator!=null) {
            if ( config.isSingular() ) { generator.writeEndArray(); }
            generator.close();
        }
    }
    
    @SneakyThrows
    private final CsvGenerator getGenerator(ObjectNode record) {
        if ( generator==null ) {
            if ( record!=null ) {
                CsvSchema.Builder schemaBuilder = CsvSchema.builder();
                record.fieldNames().forEachRemaining(schemaBuilder::addColumn);
                CsvSchema schema = schemaBuilder.build().withUseHeader(styles.has(RecordWriterStyle.SHOW_HEADERS));
                this.generator = (CsvGenerator)CsvFactory.builder().
                        build().createGenerator(getWriter())
                        .setCodec(new ObjectMapper())
                        .enable(Feature.IGNORE_UNKNOWN);
                this.generator.setSchema(schema);
                if ( config.isSingular() ) {
                    generator.writeStartArray();
                }
            }
        }
        return generator;
    }

    @SneakyThrows
    private final Writer getWriter() {
        if ( writer==null ) { writer = config.getWriterSupplier().get(); }
        return writer;
    }
}
