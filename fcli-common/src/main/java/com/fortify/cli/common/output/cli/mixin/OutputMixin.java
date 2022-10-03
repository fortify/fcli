package com.fortify.cli.common.output.cli.mixin;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputFormatConfigConverter.OutputFormatIterable;
import com.fortify.cli.common.output.cli.mixin.query.OutputMixinWithQuery;
import com.fortify.cli.common.output.writer.IRecordWriter;
import com.fortify.cli.common.output.writer.OutputFormat;
import com.fortify.cli.common.output.writer.RecordWriterConfig;
import com.fortify.cli.common.rest.runner.IfFailureHandler;
import com.fortify.cli.common.util.StringUtils;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.Messages;
import picocli.CommandLine.Spec;

@ReflectiveAccess
public class OutputMixin {
    @Getter private CommandSpec mixee;
    
    @Spec(Spec.Target.MIXEE)
    public void setMixee(CommandSpec mixee) {
        this.mixee = mixee;
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
    
    /**
     * This method allows for applying output filters. The standard {@link OutputMixin}
     * doesn't apply any filters, but {@link OutputMixinWithQuery} provides an implementation
     * for this method.
     * @param outputFormat
     * @param data
     * @return
     */
    protected JsonNode applyRecordOutputFilters(OutputFormat outputFormat, JsonNode record) {
        return record;
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
        private void writeRecord(JsonNode record) {
            // TODO Add null checks in case any input or record transformation returns null?
            record = record==null ? null : config.applyRecordTransformations(outputFormat, record);
            record = record==null ? null : applyRecordOutputFilters(outputFormat, record);
            if ( record!=null ) {
                if(record.getNodeType() == JsonNodeType.ARRAY) {
                    if(record.size()>0) recordWriter.writeRecord((ObjectNode) new ObjectMapper().readTree(record.get(0).toString()));
                } else {
                    recordWriter.writeRecord((ObjectNode) record);
                }
            }
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
                    .outputFormat(getOutputFormat())
                    .singular(config.singular())
                    .build();
        }
        
        private String getOutputWriterOptions() {
            OutputFormatConfig config = optionsArgGroup.outputFormatConfig;
            if ( config!=null && StringUtils.isNotBlank(config.getOptions()) ) {
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
}


