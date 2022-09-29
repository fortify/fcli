package com.fortify.cli.common.output.writer;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.PropertyPathFormatter;
import com.jayway.jsonpath.PathNotFoundException;

import lombok.Getter;

public abstract class AbstractFieldsRecordWriter implements IRecordWriter {
    private static final JsonNode NA_NODE = new TextNode("N/A");
    
    @Getter private final RecordWriterConfig config;
    private final List<String> fieldPaths;
    
    public AbstractFieldsRecordWriter(RecordWriterConfig config) {
        this.config = config;
        String options = config.getOptions();
        this.fieldPaths = options==null || options.isBlank() ? null : getFieldPaths(options);
    }
    
    @Override
    public final void writeRecord(ObjectNode record) {
        ObjectNode processedRecord = fieldPaths==null ? record : new ObjectMapper().createObjectNode();
        if ( fieldPaths!=null ) {
            fieldPaths.forEach(
                path -> processedRecord.set(PropertyPathFormatter.camelCase(path), evaluateValue(record, path))
            );
        }
        _writeRecord(processedRecord);
    }

    private JsonNode evaluateValue(ObjectNode record, String path) {
        try {
            return JsonHelper.evaluateJsonPath(record, path, JsonNode.class);
        } catch ( PathNotFoundException e ) {
            return NA_NODE; 
        }
    }

    protected abstract void _writeRecord(ObjectNode record);
    
    private static final List<String> getFieldPaths(String options) {
        return Arrays.asList(options.split(","));
    }
}
