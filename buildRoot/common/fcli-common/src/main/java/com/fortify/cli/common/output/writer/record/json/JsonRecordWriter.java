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
package com.fortify.cli.common.output.writer.record.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.writer.record.AbstractFormattedRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;

import lombok.SneakyThrows;

public class JsonRecordWriter extends AbstractFormattedRecordWriter {
    private JsonGenerator generator;
    
    public JsonRecordWriter(RecordWriterConfig config) {
        super(config);
    }
    
    @SneakyThrows
    private JsonGenerator getGenerator() {
        if ( generator==null ) {
            PrettyPrinter pp = !getConfig().isPretty() ? null : new DefaultPrettyPrinter(); 
            this.generator = JsonFactory.builder().
                    build().createGenerator(getWriter())
                    .setPrettyPrinter(pp)
                    .setCodec(new ObjectMapper())
                    .disable(Feature.AUTO_CLOSE_TARGET);
            if ( !getConfig().isSingular() ) {
                generator.writeStartArray();
            }
        }
        return generator;
    }

    @Override @SneakyThrows
    public void writeFormattedRecord(ObjectNode record) {
        getGenerator().writeTree(record);
    }

    @Override @SneakyThrows
    public void close() {
        if ( !getConfig().isSingular() ) {
            getGenerator().writeEndArray();
            getGenerator().close();
        }
        getWriter().write("\n"); // End with a newline
    }
}
