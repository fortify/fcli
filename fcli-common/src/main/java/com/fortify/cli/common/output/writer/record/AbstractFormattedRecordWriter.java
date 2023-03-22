package com.fortify.cli.common.output.writer.record;

import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.PropertyPathFormatter;
import com.fortify.cli.common.output.transform.flatten.FlattenTransformer;
import com.fortify.cli.common.util.StringUtils;

public abstract class AbstractFormattedRecordWriter extends AbstractRecordWriter {
    private static final JsonNode NA_NODE = new TextNode("N/A");
    
    private final List<String> fieldPaths;
    
    public AbstractFormattedRecordWriter(RecordWriterConfig config) {
        super(config);
        String options = config.getOptions();
        this.fieldPaths = StringUtils.isBlank(options) ? null : getFieldPaths(options.replaceAll("\\s", ""));
    }
    
    @Override
    protected RecordWriterConfig updateConfig(RecordWriterConfig config) {
        config = super.updateConfig(config);
        String options = config.getOptions();
        if ( config.isAddActionColumn() && !StringUtils.isBlank(options) && !options.contains(IActionCommandResultSupplier.actionFieldName) ) {
            config.setOptions(options+","+IActionCommandResultSupplier.actionFieldName);
        }
        return config;
    }
    
    @Override
    public final void writeRecord(ObjectNode record) {
        writeFormattedRecord(getFormattedRecord(record));
    }
    
    protected Writer getWriter() {
        return getConfig().getWriter();
    }
    
    protected abstract void writeFormattedRecord(ObjectNode record);
    
    /**
     * This method first applies optional field transformations (if {@link #fieldPaths} has been 
     * configured (through {@link RecordWriterConfig#setOptions(String)}), then applies optional
     * flatten transformation (depending on the configured {@link OutputFormat}. Note that field
     * transformation already implicitly flattens the provided record, except for fields that
     * represent a nested {@link ObjectNode}, hence we always optionally flatten the record 
     * independent of whether field transformations were applied or not.  
     * @param record
     * @return Formatted record after applying optional field and flatten transformations
     */
    protected ObjectNode getFormattedRecord(ObjectNode record) {
        return applyOptionalRecordFlattenTransformation(getConfig().getOutputFormat(),
                applyOptionalFieldPathsTransformation(fieldPaths, record));
    }
    
    private static final ObjectNode applyOptionalFieldPathsTransformation(List<String> fieldPaths, ObjectNode record) {
        if ( fieldPaths==null || fieldPaths.isEmpty() ) { return record; }
        ObjectNode formattedRecord = new ObjectMapper().createObjectNode();
        fieldPaths.forEach(
            path -> formattedRecord.set(PropertyPathFormatter.camelCase(path), evaluateValue(record, path))
        );
        return formattedRecord;
    }
    
    private static final ObjectNode applyOptionalRecordFlattenTransformation(OutputFormat outputFormat, ObjectNode record) {
        return !outputFormat.isFlat() 
                ? record 
                : new FlattenTransformer(PropertyPathFormatter::camelCase, ".", false).transformObjectNode(record);
    }

    private static final JsonNode evaluateValue(ObjectNode record, String path) {
        JsonNode result = JsonHelper.evaluateSpELExpression(record, path, JsonNode.class);
        return result!=null ? result : NA_NODE;
    }
    
    private static final List<String> getFieldPaths(String options) {
        return Arrays.asList(options.split(","));
    }
}
