/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.output.writer.record.csv;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fortify.cli.common.output.writer.record.AbstractFormattedRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;

import lombok.SneakyThrows;

public class CsvRecordWriter extends AbstractFormattedRecordWriter {
    public static enum CsvType { HEADERS, NO_HEADERS }
    private final CsvType csvType;
    private CsvGenerator generator;
    
    public CsvRecordWriter(CsvType csvType, RecordWriterConfig config) {
        super(config);
        this.csvType = csvType;
    }
    
    @SneakyThrows
    private CsvGenerator getGenerator(ObjectNode record) {
        if ( generator==null ) {
            if ( record!=null ) {
                CsvSchema.Builder schemaBuilder = CsvSchema.builder();
                record.fieldNames().forEachRemaining(schemaBuilder::addColumn);
                CsvSchema schema = schemaBuilder.build()
                        .withUseHeader(CsvType.HEADERS==csvType);
                this.generator = (CsvGenerator)CsvFactory.builder().
                        build().createGenerator(getWriter())
                        .setCodec(new ObjectMapper())
                        .enable(Feature.IGNORE_UNKNOWN)
                        .disable(Feature.AUTO_CLOSE_TARGET);
                this.generator.setSchema(schema);
                if ( !getConfig().isSingular() ) {
                    generator.writeStartArray();
                }
            }
        }
        return generator;
    }

    @Override @SneakyThrows
    public void writeFormattedRecord(ObjectNode record) {
        getGenerator(record).writeTree(record);
    }
    
    @Override @SneakyThrows
    public void close() {
        if ( !getConfig().isSingular() && generator!=null ) {
            generator.writeEndArray();
            generator.close();
        }
    }

}
