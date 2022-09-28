package com.fortify.cli.common.output.writer;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.PropertyPathFormatter;

import lombok.Getter;

public abstract class AbstractFieldsRecordWriter implements IRecordWriter {
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
                path -> processedRecord.set(PropertyPathFormatter.camelCase(path), JsonHelper.evaluateJsonPath(record, path, JsonNode.class))
            );
        }
        _writeRecord(processedRecord);
    }

    protected abstract void _writeRecord(ObjectNode record);
    
    private static final List<String> getFieldPaths(String options) {
        return Arrays.asList(options.split(","));
    }
}
