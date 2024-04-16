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
package com.fortify.cli.common.output.writer.record.yaml;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fortify.cli.common.output.writer.record.AbstractFormattedRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;

import lombok.SneakyThrows;

public class YamlRecordWriter extends AbstractFormattedRecordWriter {
    private YAMLGenerator generator;

    public YamlRecordWriter(RecordWriterConfig config) {
        super(config);
    }
    
    @SneakyThrows
    private YAMLGenerator getGenerator() {
        if ( generator==null ) {
            YAMLFactory factory = new YAMLFactory();
            this.generator = (YAMLGenerator)factory.createGenerator(getWriter());
            generator.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
                    .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
                    .setCodec(new ObjectMapper())
                    .disable(Feature.AUTO_CLOSE_TARGET);
            if ( getConfig().isPretty() ) generator = (YAMLGenerator)generator.useDefaultPrettyPrinter();
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
        }
        getGenerator().close();
    }
}
