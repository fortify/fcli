/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
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
package com.fortify.cli.common.output.writer.record.json_properties;

import java.util.TreeSet;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.writer.record.AbstractRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.fortify.cli.common.util.StringUtils;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import lombok.SneakyThrows;

public class JsonPropertiesRecordWriter extends AbstractRecordWriter {
    private static final String SEPARATOR = ":::";
    private final TreeSet<String> paths = new TreeSet<>();
    
    public JsonPropertiesRecordWriter(RecordWriterConfig config) {
        super(config);
    }

    @Override @SneakyThrows
    public void writeRecord(ObjectNode record) {
        addProperties("", record);
    }
    
    private void addProperties(String parent, ObjectNode record) {
        record.fields().forEachRemaining(e->addProperties(parent, e.getKey(), e.getValue()));
    }
    
    private void addProperties(String parent, ArrayNode record) {
        addProperty(parent, record);
        for ( int i = 0 ; i < record.size() ; i++ ) {
            JsonNode value = record.get(i);
            addProperty(parent+"["+i+"]", value);
            addProperties(parent, "["+i+"]", value);
        }
    }
    
    private void addProperties(String parent, String name, JsonNode value) {
        String fullName = StringUtils.isBlank(parent) || name.startsWith("[") 
                ? parent+name : parent+"."+name;
        if ( value instanceof ObjectNode ) { addProperties(fullName, (ObjectNode)value); }
        else if (value instanceof ArrayNode ) {addProperties(fullName, (ArrayNode)value); }
        else { addProperty(fullName, value); }
    }
    
    private void addProperty(String name, JsonNode value) {
        paths.add(name+SEPARATOR+value.getClass().getSimpleName());
    }

    @Override @SneakyThrows
    public void close() {
        Column[] columnObjects = Stream.of("Name", "Type").map(field->
            new Column()
                .dataAlign(HorizontalAlign.LEFT)
                .headerAlign(HorizontalAlign.LEFT)
                .header(field))
                .toArray(Column[]::new);
        String[][] data = paths.stream().map(e->e.split(SEPARATOR)).toArray(String[][]::new);
        getConfig().getWriter().write(
            AsciiTable.getTable(AsciiTable.NO_BORDERS, columnObjects, data));
    }
}
