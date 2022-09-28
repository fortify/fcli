package com.fortify.cli.common.output.cli.mixin;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputFormatConfigConverter.OutputFormatIterable;
import com.fortify.cli.common.output.cli.mixin.filter.OptionAnnotationHelper;
import com.fortify.cli.common.output.cli.mixin.filter.OutputFilter;
import com.fortify.cli.common.output.transform.PropertyPathFormatter;
import com.fortify.cli.common.output.transform.flatten.FlattenTransformer;
import com.fortify.cli.common.output.transform.jsonpath.JsonPathTransformer;
import com.fortify.cli.common.output.writer.IRecordWriter;
import com.fortify.cli.common.output.writer.OutputFormat;
import com.fortify.cli.common.output.writer.RecordWriterConfig;
import com.fortify.cli.common.rest.runner.IfFailureHandler;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import lombok.Data;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.Messages;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Spec;

@ReflectiveAccess
public class OutputMixin {
    private CommandSpec mixee;
    private OptionAnnotationHelper optionAnnotationHelper;
    
    @Spec(Spec.Target.MIXEE)
    public void setMixee(CommandSpec mixee) {
        this.mixee = mixee;
        this.optionAnnotationHelper = new OptionAnnotationHelper(mixee);
    }
    
    @ArgGroup(headingKey = "arggroup.output.heading", exclusive = false)
    private OutputOptionsArgGroup outputOptionsArgGroup;
    
    @Data
    public static final class OutputFormatConfig {
        private final OutputFormat outputFormat;
        private final String options;
    }

    private static final class OutputOptionsArgGroup {
        @CommandLine.Option(names = {"-o", "--output"}, order=1, converter = OutputFormatConfigConverter.class, completionCandidates = OutputFormatIterable.class, paramLabel = "format[=<options>]")
        private OutputFormatConfig outputFormatConfig;
        
        @CommandLine.Option(names = {"--output-to-file"}, order=7)
        private String outputFile; 
    }
    
    public OutputOptionsWriter getWriter() {
        return new OutputOptionsWriter(getOutputOptionsWriterConfig());
    }
    
    public void write(JsonNode jsonNode) {
        write(writer->writer::write, jsonNode);
    }

    public void write(HttpRequest<?> httpRequest) {
        write(writer->writer::write, httpRequest);
    }
    
    public void write(HttpRequest<?> httpRequest, Function<HttpResponse<JsonNode>, String> nextPageUrlProducer) {
        write(writer->writer::write, httpRequest, nextPageUrlProducer);
    }
    
    public void write(HttpResponse<JsonNode> httpResponse) {
        write(writer->writer::write, httpResponse);
    }
    
    private <T> void write(Function<OutputOptionsWriter, Consumer<T>> consumer, T input) {
        try ( var writer = getWriter() ) {
            consumer.apply(writer).accept(input);
        }
    }
    
    private <T1, T2> void write(Function<OutputOptionsWriter, BiConsumer<T1, T2>> consumer, T1 input1, T2 input2) {
        try ( var writer = getWriter() ) {
            consumer.apply(writer).accept(input1, input2);
        }
    }
    
    private OutputConfig getOutputOptionsWriterConfig() {
        Object mixeeObject = mixee.userObject();
        if ( mixeeObject instanceof IOutputConfigSupplier ) {
            return ((IOutputConfigSupplier)mixeeObject).getOutputOptionsWriterConfig();
        } else {
            return new OutputConfig();
        }
    }

    public final class OutputOptionsWriter implements AutoCloseable { // TODO Implement interface, make implementation private
        private final OutputMixin optionsHandler = OutputMixin.this;
        private final OutputOptionsArgGroup optionsArgGroup = optionsHandler.outputOptionsArgGroup!=null ? optionsHandler.outputOptionsArgGroup : new OutputOptionsArgGroup();
        private final OutputConfig config;
        private final OutputFormat outputFormat;
        private final PrintWriter printWriter;
        private final IRecordWriter recordWriter;
        
        public OutputOptionsWriter(OutputConfig config) {
            this.config = config;
            this.outputFormat = getOutputFormat();
            this.printWriter = createPrintWriter(config);
            this.recordWriter = outputFormat.getRecordWriterFactory().createRecordWriter(createOutputWriterConfig());
        }
        
        public void write(JsonNode jsonNode) {
            jsonNode = config.applyInputTransformations(outputFormat, jsonNode);
            if ( jsonNode!=null ) {
                if ( jsonNode.isArray() ) {
                    jsonNode.elements().forEachRemaining(this::writeRecord);
                } else if ( jsonNode.isObject() ) {
                    writeRecord(jsonNode);
                } else {
                    throw new RuntimeException("Not sure what to do here");
                }
            }
        }
        
        public void write(HttpRequest<?> httpRequest) {
            httpRequest.asObject(JsonNode.class)
                .ifSuccess(this::write)
                .ifFailure(IfFailureHandler::handle); // Just in case no error interceptor was registered for this request
        }
        
        @SuppressWarnings("unchecked") // TODO Can we get rid of this warning in a better way?
        public void write(HttpRequest<?> httpRequest, Function<HttpResponse<JsonNode>, String> nextPageUrlProducer) {
            httpRequest.asPaged(r->r.asObject(JsonNode.class), nextPageUrlProducer)
                .ifSuccess(this::write)
                .ifFailure(IfFailureHandler::handle); // Just in case no error interceptor was registered for this request
        }

        public void write(HttpResponse<JsonNode> httpResponse) {
            write(httpResponse.getBody());
        }

        @SneakyThrows
        private void writeRecord(JsonNode jsonNode) {
            // TODO Add null checks in case any input or record transformation returns null?
            jsonNode = config.applyRecordTransformations(outputFormat, jsonNode);
            jsonNode = applyOutputFilterTransformation(outputFormat, jsonNode);
            jsonNode = applyFieldRenameTransformation(outputFormat, jsonNode);
            jsonNode = applyFlattenTransformation(outputFormat, jsonNode);
            if ( jsonNode!=null ) {
                if(jsonNode.getNodeType() == JsonNodeType.ARRAY) {
                    if(jsonNode.size()>0) recordWriter.writeRecord((ObjectNode) new ObjectMapper().readTree(jsonNode.get(0).toString()));
                } else {
                    recordWriter.writeRecord((ObjectNode) jsonNode);
                }
            }
        }

        protected JsonNode applyOutputFilterTransformation(OutputFormat outputFormat, JsonNode data) {
            // TODO Improve this?
            for ( OptionSpec optionSpec : optionAnnotationHelper.optionsWithAnnotationStream(OutputFilter.class).collect(Collectors.toList()) ) {
                data = applyOutputFilterTransformation(outputFormat, data, optionSpec); 
            }
            return data;
        }
        
        private JsonNode applyOutputFilterTransformation(OutputFormat outputFormat, JsonNode data, OptionSpec optionSpec) {
            if ( !data.isEmpty() ) {
                String fieldName = OptionAnnotationHelper.getOptionTargetName(optionSpec, OutputFilter.class);
                Object value = optionSpec.getValue();
                if ( value!=null ) {
                    String format = value instanceof String ? "[?(@.%s == \"%s\")]" : "[?(@.%s == %s)]";
                    data = new JsonPathTransformer(String.format(format, fieldName, value)).transform(data);
                }
            }
            return data;
        }
        
        protected JsonNode applyFlattenTransformation(OutputFormat outputFormat, JsonNode data) {
            if ( OutputFormat.isFlat(outputFormat) ) {
                data = new FlattenTransformer(PropertyPathFormatter::camelCase, ".", false).transform(data);
            }
            return data;
        }
        
        protected JsonNode applyFieldRenameTransformation(OutputFormat outputFormat, JsonNode jsonNode) {
            // jsonNode = applyI18nTransformation(outputFormat, jsonNode); // TODO Rename fields based on message resources
            // jsonNode = applyFieldFormatTransformation(outputFormat, jsonNode); // TODO Rename fields based on outputFormat.getDefaultFieldNameFormatter()
            return jsonNode;
        }

        private OutputFormat getOutputFormat() {
            OutputFormat result = optionsArgGroup.outputFormatConfig==null 
                    ? config.defaultFormat() 
                    : optionsArgGroup.outputFormatConfig.getOutputFormat();
            if ( result == null ) {
                result = OutputFormat.table;
            }
            return result;
        }
        
        private Messages getMessages() {
            ResourceBundle resourceBundle = mixee.resourceBundle();
            return resourceBundle==null ? null : new Messages(mixee, resourceBundle);
        }
        
        private RecordWriterConfig createOutputWriterConfig() {
            return RecordWriterConfig.builder()
                    .printWriter(printWriter)
                    .options(getOutputWriterOptions())
                    .singular(config.singular())
                    .build();
        }
        
        private String getOutputWriterOptions() {
            OutputFormatConfig config = optionsArgGroup.outputFormatConfig;
            if ( config!=null && config.getOptions()!=null && !config.getOptions().isBlank() ) {
                return config.getOptions();
            } else {
                String keySuffix = "output."+getOutputFormat().getMessageKey()+".options";
                Messages messages = getMessages();
                return getClosestMatch(messages, keySuffix);
            }
        }

        private String getClosestMatch(Messages messages, String keySuffix) {
            CommandSpec commandSpec = messages.commandSpec();
            String value = null;
            while ( commandSpec!=null && value==null ) {
                String key = commandSpec.qualifiedName(".")+"."+keySuffix;
                value = messages.getString(key, null);
                commandSpec = commandSpec.parent();
            }
            return value;
        }

        private final PrintWriter createPrintWriter(OutputConfig config) {
            try {
                return optionsArgGroup.outputFile == null || "-".equals(optionsArgGroup.outputFile)
                        ? new PrintWriter(System.out)
                        : new PrintWriter(optionsArgGroup.outputFile);
            } catch ( FileNotFoundException e) {
                throw new IllegalArgumentException("Output file "+optionsArgGroup.outputFile.toString()+" cannot be accessed");
            }
        }

        @Override
        public void close() {
            recordWriter.finishOutput();
            printWriter.flush();
            // TODO Close printwriter and/or underlying streams except for System.out
        }
    }
    
    public static interface IDefaultFieldNameFormatterProvider {
        public Function<String, String> getDefaultFieldNameFormatter(OutputFormat outputFormat);
    }
    /*
    private final class I18nDefaultFieldNameFormatterProvider implements IDefaultFieldNameFormatterProvider {
        private final Messages messages;
        I18nDefaultFieldNameFormatterProvider() {
            this.messages = getMessages();
        }
        @Override
        public Function<String, String> getDefaultFieldNameFormatter(OutputFormat outputFormat) {
            return field -> getDefaultFieldName(outputFormat, field);
        }

        private String getDefaultFieldName(OutputFormat outputFormat, String field) {
            String[] keys = {
                String.format("output.%s.field.%s.name", outputFormat.name(), field),
                String.format("output.field.%s.name", field),
            };
            
            return Stream.of(keys)
            .map(this::getMessageString)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(outputFormat.getDefaultFieldNameFormatter().apply(field));
        }
        
        private String getMessageString(String key) {
            return messages==null ? null : messages.getString(key, null);
        }
    }
    */
}


