/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.output.writer.record.xml;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fortify.cli.common.output.writer.record.AbstractFormattedRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;

import lombok.SneakyThrows;

// TODO Add support for writing a single item if config.singular()==true
public class XmlRecordWriter extends AbstractFormattedRecordWriter {
    private ToXmlGenerator generator;

    public XmlRecordWriter(RecordWriterConfig config) {
        super(config);
    }
    
    @SneakyThrows
    private ToXmlGenerator getGenerator() {
        if ( generator==null ) {
            XmlFactory factory = new XmlFactory();
            this.generator = (ToXmlGenerator)factory.createGenerator(getWriter())
                    .setCodec(new ObjectMapper())
                    .disable(Feature.AUTO_CLOSE_TARGET);
            if ( getConfig().isPretty() ) generator = (ToXmlGenerator)generator.useDefaultPrettyPrinter();
            generator.setNextName(new QName(null, "items"));
            generator.writeStartObject();
        }
        return generator;
    }

    @Override @SneakyThrows
    public void writeFormattedRecord(ObjectNode record) {
        getGenerator().writeFieldName("item");
        getGenerator().writeTree(record);
    }
    
    @Override @SneakyThrows
    public void close() {
        getGenerator().writeEndObject();
        getGenerator().close();
    }
}
