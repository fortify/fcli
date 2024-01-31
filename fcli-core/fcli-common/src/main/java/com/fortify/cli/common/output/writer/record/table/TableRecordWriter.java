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
package com.fortify.cli.common.output.writer.record.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.transform.PropertyPathFormatter;
import com.fortify.cli.common.output.writer.record.AbstractFormattedRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import lombok.SneakyThrows;

public class TableRecordWriter extends AbstractFormattedRecordWriter {
    public static enum TableType { HEADERS, NO_HEADERS }
    private final TableType tableType;
    private String[] fields;
    private final List<String[]> rows = new ArrayList<>();
    
    public TableRecordWriter(TableType tableType, RecordWriterConfig config) {
        super(config);
        this.tableType = tableType;
    }

    @Override @SneakyThrows
    public void writeFormattedRecord(ObjectNode record) {
        String[] columns = getFields(record);
        String[] row = getRow(record, columns);
        rows.add(row);
    }

    @Override @SneakyThrows
    public void close() {
        getWriter().write(getTable(fields, rows.toArray(new String[rows.size()][])));
    }

    private String getTable(String[] fields, String[][] data) {
        if ( fields == null ) {
            return "No data"; // TODO properly handle this
        } else {
            Column[] columnObjects = Stream.of(fields).map(field->
                    new Column()
                        .dataAlign(HorizontalAlign.LEFT)
                        .headerAlign(HorizontalAlign.LEFT)
                        .header(TableType.HEADERS==tableType ? getHeader(field) : null))
                        .toArray(Column[]::new);
            return AsciiTable.getTable(AsciiTable.NO_BORDERS, columnObjects, data); 
        }
    }

    private String getHeader(String fieldName) {
        String header = getConfig().getMessageResolver().getMessageString("output.header."+fieldName);
        return header!=null ? header : PropertyPathFormatter.humanReadable(getNormalizedFieldName(fieldName));
    }

    private String getNormalizedFieldName(String fieldName) {
        return fieldName.replaceAll("String$", ""); 
    }

    private String[] getFields(ObjectNode firstObjectNode) {
        if ( fields==null ) { 
            fields = asStream(firstObjectNode.fieldNames()).toArray(String[]::new);
        }
        return fields;
    }

    // TODO: Some REST APIs will return a varying set of properties for individual records. We should review null processing here.
    private String[] getRow(ObjectNode record, String[] columns) {
        for(String propertyName : columns){
            if (record.get(propertyName) == null) {
                record.put(propertyName, "null");
            }
        }
        return Stream.of(columns).map(record::get).map(JsonNode::asText).map(v->"null".equals(v)?"N/A":v).toArray(String[]::new);
    }

    private static final <T> Stream<T> asStream(Iterator<T> sourceIterator) {
        Iterable<T> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

}
