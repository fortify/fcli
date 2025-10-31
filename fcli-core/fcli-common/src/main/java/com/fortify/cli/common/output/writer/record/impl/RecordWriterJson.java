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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecordWriterJson extends AbstractRecordWriterJackson<JsonGenerator> {
    @Getter private final RecordWriterConfig config;
    
    @Override
    protected JsonGenerator createGenerator(Writer writer) throws IOException {
        PrettyPrinter pp = !config.getStyle().isPretty() ? null : new DefaultPrettyPrinter(); 
        return JsonFactory.builder().
            build().createGenerator(writer)
            .setPrettyPrinter(pp)
            .setCodec(new ObjectMapper());
    }
    
    @Override
    protected void writeStart(JsonGenerator out) throws IOException {
        if ( config.getStyle().isArray() ) { out.writeStartArray(); }
    }
    
    @Override
    protected void writeEnd(JsonGenerator out) throws IOException {
        if ( config.getStyle().isArray() ) { out.writeEndArray(); }
    }
}
