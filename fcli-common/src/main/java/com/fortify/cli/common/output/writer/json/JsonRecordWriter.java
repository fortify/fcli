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
package com.fortify.cli.common.output.writer.json;

import java.io.PrintWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.writer.IRecordWriter;
import com.fortify.cli.common.output.writer.RecordWriterConfig;

import lombok.SneakyThrows;

public class JsonRecordWriter implements IRecordWriter {
    private final RecordWriterConfig config;
    private JsonGenerator generator;
    
    public JsonRecordWriter(RecordWriterConfig config) {
        this.config = config;
    }
    
    private PrintWriter getPrintWriter() {
        return config.getPrintWriter();
    }
    
    @SneakyThrows
    private JsonGenerator getGenerator() {
        if ( generator==null ) {
            PrettyPrinter pp = !config.isPretty() ? null : new DefaultPrettyPrinter(); 
            this.generator = JsonFactory.builder().
                    build().createGenerator(getPrintWriter())
                    .setPrettyPrinter(pp)
                    .setCodec(new ObjectMapper())
                    .disable(Feature.AUTO_CLOSE_TARGET);
            if ( !config.isSingular() ) {
                generator.writeStartArray();
            }
        }
        return generator;
    }

    @Override @SneakyThrows
    public void writeRecord(ObjectNode record) {
        getGenerator().writeTree(record);
    }

    @Override @SneakyThrows
    public void finishOutput() {
        if ( !config.isSingular() ) {
            getGenerator().writeEndArray();
            getGenerator().close();
        }
        getPrintWriter().println(); // End with a newline
    }
}
