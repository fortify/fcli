/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
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
