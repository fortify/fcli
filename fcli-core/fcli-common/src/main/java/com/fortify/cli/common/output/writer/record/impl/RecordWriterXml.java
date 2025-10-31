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

import javax.xml.namespace.QName;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecordWriterXml extends AbstractRecordWriterJackson<ToXmlGenerator> {
    @Getter private final RecordWriterConfig config;
    
    @Override
    protected void append(ToXmlGenerator out, ObjectNode formattedRecord) throws IOException {
        out.writeFieldName("item");
        out.writeTree(formattedRecord);
    }
    
    @Override
    protected ToXmlGenerator createGenerator(Writer writer) throws IOException {
        XmlFactory factory = new XmlFactory();
        var result = (ToXmlGenerator)factory.createGenerator(writer)
                .setCodec(new ObjectMapper());
        if ( config.getStyle().isPretty() ) {
            result = (ToXmlGenerator) result.useDefaultPrettyPrinter();
        }
        return result;
    }
    
    @Override
    protected void writeStart(ToXmlGenerator out) throws IOException {
        out.setNextName(new QName(null, "items"));
        out.writeStartObject();
    }
    
    @Override
    protected void writeEnd(ToXmlGenerator out) throws IOException {
        out.writeEndObject();
    }
}
