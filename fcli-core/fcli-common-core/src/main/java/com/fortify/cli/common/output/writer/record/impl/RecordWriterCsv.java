/*
 * Copyright 2021-2026 Open Text.
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// TODO This class uses a Jackson generator, so should we extend from AbstractRecordWriterJackson? 
@RequiredArgsConstructor
public class RecordWriterCsv extends AbstractRecordWriter<JsonGenerator> {
    @Getter private final RecordWriterConfig config;
    
    @Override
    protected void append(JsonGenerator out, ObjectNode formattedRecord) throws IOException {
        out.writeTree(formattedRecord);
    }
    
    @Override
    protected Function<ObjectNode, ObjectNode> createRecordFormatter(ObjectNode objectNode) throws IOException {
        // For CSV, we always flatten, keeping the original dot-separated property path as headers
        return createStructuredOutputTransformer(true, Function.identity());
    }   
    
    @Override
    protected void close(JsonGenerator out) throws IOException {
        if ( config.getStyle().isArray() ) { out.writeEndArray(); }
        out.close();
    }
    
    @Override
    protected void closeWithNoData(Writer writer) throws IOException {
        writer.close();
    }
    
    @Override
    protected JsonGenerator createOut(Writer writer, ObjectNode formattedRecord) throws IOException {
        if ( formattedRecord==null ) { return null; }
        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        formattedRecord.fieldNames().forEachRemaining(schemaBuilder::addColumn);
        CsvSchema schema = schemaBuilder.build().withUseHeader(config.getStyle().withHeaders());
        var result = (CsvGenerator)CsvFactory.builder().
                build().createGenerator(writer)
                .setCodec(new ObjectMapper())
                .enable(Feature.IGNORE_UNKNOWN);
        result.setSchema(schema);
        if ( config.getStyle().isArray() ) {
            result.writeStartArray();
        }
        // Escape formula-triggering values as they're written, without mutating the input ObjectNode
        return config.getStyle().isCsvEscape() ? new FormulaEscapingGenerator(result) : result;
    }
    
    /**
     * Prevents CSV formula injection (CWE-1236) by prefixing values starting with a formula-trigger
     * character with a single quote, applied as Jackson writes each value to the underlying generator.
     * Unlike pre-scanning and mutating the record's {@link ObjectNode}, this also covers non-textual
     * node values (e.g. {@code POJONode}) that still get serialized as strings, leaves a potentially
     * shared {@link ObjectNode} untouched, and avoids a separate pass over record fields.
     */
    private static final class FormulaEscapingGenerator extends JsonGeneratorDelegate {
        // Leading characters that spreadsheet applications interpret as the start of a formula
        private static final String FORMULA_TRIGGER_CHARS = "=+-@\t\r\n";
        
        FormulaEscapingGenerator(JsonGenerator delegate) {
            super(delegate);
        }
        
        @Override
        public void writeTree(TreeNode rootNode) throws IOException {
            // Invoke the codec with 'this' as target (JsonGenerator's default writeTree() behavior),
            // instead of JsonGeneratorDelegate's default of forwarding to the delegate and bypassing
            // the writeString() overrides below.
            if ( rootNode==null ) {
                writeNull();
            } else {
                getCodec().writeValue(this, rootNode);
            }
        }
        
        @Override
        public void writeString(String text) throws IOException {
            super.writeString(escape(text));
        }
        
        @Override
        public void writeString(char[] text, int offset, int len) throws IOException {
            super.writeString(escape(new String(text, offset, len)));
        }
        
        @Override
        public void writeString(SerializableString text) throws IOException {
            super.writeString(escape(text.getValue()));
        }
        
        private static String escape(String text) {
            return text!=null && !text.isEmpty() && FORMULA_TRIGGER_CHARS.indexOf(text.charAt(0))>=0
                    ? "'"+text : text;
        }
    }
}
