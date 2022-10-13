package com.fortify.cli.common.output.writer.output.standard;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.spi.ISingularSupplier;
import com.fortify.cli.common.output.writer.IMessageResolver;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.query.OutputWriterWithQuery;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.runner.IfFailureHandler;
import com.fortify.cli.common.util.CommandSpecHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.common.variable.FcliVariableHelper;
import com.fortify.cli.common.variable.FcliVariableHelper.VariableType;
import com.fortify.cli.common.variable.IMinusVariableUnsupported;
import com.fortify.cli.common.variable.MinusVariableDefinition;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import lombok.SneakyThrows;
import picocli.CommandLine.Model.CommandSpec;

@ReflectiveAccess 
public class StandardOutputWriter implements IOutputWriter {
    private final StandardOutputConfig defaultOutputConfig;
    private final CommandSpec commandSpec;
    private final IOutputOptions outputOptions;
    
    public StandardOutputWriter(CommandSpec commandSpec, IOutputOptions outputOptions, StandardOutputConfig defaultOutputConfig) {
     // Make sure that we get the CommandSpec for the actual command being invoked,
        // not some intermediate Mixin
        this.commandSpec = commandSpec.commandLine()==null ? commandSpec : commandSpec.commandLine().getCommandSpec();
        this.outputOptions = outputOptions;
        this.defaultOutputConfig = defaultOutputConfig;
    }
    
    public OutputOptionsWriter getWriter() {
        return new OutputOptionsWriter(getOutputOptionsWriterConfig());
    }
    
    @Override
    public void write(JsonNode jsonNode) {
        write(writer->writer::write, jsonNode);
    }

    @Override
    public void write(HttpRequest<?> httpRequest) {
        write(writer->writer::write, httpRequest);
    }
    
    @Override
    public void write(HttpRequest<?> httpRequest, INextPageUrlProducer nextPageUrlProducer) {
        if ( nextPageUrlProducer==null ) {
            write(httpRequest);
        } else {
            write(writer->writer::write, httpRequest, nextPageUrlProducer);
        }
    }
    
    @Override
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
    
    private StandardOutputConfig getOutputOptionsWriterConfig() {
        Object mixeeObject = commandSpec.userObject();
        if ( mixeeObject instanceof IOutputConfigSupplier ) {
            return ((IOutputConfigSupplier)mixeeObject).getOutputConfig();
        } else {
            return defaultOutputConfig;
        }
    }
    
    /**
     * This method allows for applying output filters. The standard {@link StandardOutputWriter}
     * doesn't apply any filters, but {@link OutputWriterWithQuery} provides an implementation
     * for this method.
     * @param outputFormat
     * @param data
     * @return
     */
    protected JsonNode applyRecordOutputFilters(OutputFormat outputFormat, JsonNode record) {
        return record;
    }

    public final class OutputOptionsWriter implements AutoCloseable, IMessageResolver { // TODO Implement interface, make implementation private
        private final StandardOutputWriter parent = StandardOutputWriter.this;
        private final IOutputOptions outputOptions = parent.outputOptions;
        private final StandardOutputConfig config;
        private final OutputFormat outputFormat;
        private final PrintWriter printWriter;
        private final IRecordWriter[] recordWriters;
        
        public OutputOptionsWriter(StandardOutputConfig config) {
            this.config = config;
            this.outputFormat = getOutputFormat();
            this.printWriter = createPrintWriter(config);
            this.recordWriters = getRecordWriters(config);
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
        
        public void write(HttpRequest<?> httpRequest, INextPageUrlProducer nextPageUrlProducer) {
            PagingHelper.pagedRequest(httpRequest, nextPageUrlProducer)
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
                for ( IRecordWriter recordWriter : recordWriters ) {
                    if(record.getNodeType() == JsonNodeType.ARRAY) {
                        if(record.size()>0) recordWriter.writeRecord((ObjectNode) new ObjectMapper().readTree(record.get(0).toString()));
                    } else {
                        recordWriter.writeRecord((ObjectNode) record);
                    }
                }
            }
        }
        
        private IRecordWriter[] getRecordWriters(StandardOutputConfig config) {
            List<IRecordWriter> recordWritersList = new ArrayList<>();
            recordWritersList.add(outputFormat.getRecordWriterFactory().createRecordWriter(createOutputWriterConfig()));
            VariableStoreConfig variableStoreConfig = outputOptions.getVariableStoreConfig();
            if ( variableStoreConfig!=null ) {
                recordWritersList.add(OutputFormat.json.getRecordWriterFactory().createRecordWriter(createOutputStoreWriterConfig(variableStoreConfig)));           
            }
            return recordWritersList.toArray(IRecordWriter[]::new);
        }

        private OutputFormat getOutputFormat() {
            OutputFormat result = outputOptions.getOutputFormatConfig()==null 
                    ? config.defaultFormat() 
                    : outputOptions.getOutputFormatConfig().getOutputFormat();
            if ( result == null ) {
                result = OutputFormat.table;
            }
            return result;
        }
        
        private RecordWriterConfig createOutputWriterConfig() {
            return RecordWriterConfig.builder()
                    .printWriter(printWriter)
                    .options(getOutputWriterOptions())
                    .outputFormat(getOutputFormat())
                    .singular(isSingularOutput())
                    .messageResolver(this)
                    .cmd(commandSpec.userObject())
                    .build();
        }
        
        // TODO Clean up this code, preferably move some code to some helper class
        private RecordWriterConfig createOutputStoreWriterConfig(VariableStoreConfig variableStoreConfig) {
            Object cmd = commandSpec.userObject();
            String variableName = variableStoreConfig.getVariableName();
            String options = variableStoreConfig.getOptions();
            VariableType variableType = VariableType.USER_PROVIDED;
            if ( "-".equals(variableName) ) {
                if ( StringUtils.isNotBlank(options) ) { 
                    throw new IllegalArgumentException("Option --store doesn't support options for variable alias '-'");
                }
                if ( cmd instanceof IMinusVariableUnsupported || !isSingularOutput() ) {
                    throw new IllegalArgumentException("Option --store doesn't support variable alias '-' on this command");
                }
                MinusVariableDefinition minusVariableDefinition = CommandSpecHelper.findAnnotation(commandSpec, MinusVariableDefinition.class);
                if ( minusVariableDefinition==null ) {
                    throw new IllegalArgumentException("Option --store doesn't support variable alias '-' on this command tree");
                } else {
                    variableName = FcliVariableHelper.resolveVariableName(cmd, minusVariableDefinition.name());
                    options = minusVariableDefinition.field();
                    variableType = VariableType.PREDEFINED;
                }
            }
            return RecordWriterConfig.builder()
                    .printWriter(FcliVariableHelper.getVariableContentsPrintWriter(variableType, variableName))
                    .options(options)
                    .outputFormat(OutputFormat.json)
                    .singular(isSingularOutput())
                    .messageResolver(this)
                    .build();
        }
        
        private boolean isSingularOutput() {
            Object cmd = commandSpec.userObject();
            return cmd instanceof ISingularSupplier
                    ? ((ISingularSupplier)cmd).isSingular()
                    : false;
        }
        
        private String getOutputWriterOptions() {
            OutputFormatConfig config = outputOptions.getOutputFormatConfig();
            if ( config!=null && StringUtils.isNotBlank(config.getOptions()) ) {
                return config.getOptions();
            } else {
                String keySuffix = "output."+getOutputFormat().getMessageKey()+".options";
                return getMessageString(keySuffix);
            }
        }

        @Override
        public String getMessageString(String keySuffix) {
            return CommandSpecHelper.getMessageString(commandSpec, keySuffix);
        }

        private final PrintWriter createPrintWriter(StandardOutputConfig config) {
            String outputFile = outputOptions.getOutputFile();
            try {
                return outputFile == null || "-".equals(outputFile)
                        ? new PrintWriter(System.out)
                        : new PrintWriter(outputFile);
            } catch ( FileNotFoundException e) {
                throw new IllegalArgumentException("Output file "+outputFile+" cannot be accessed");
            }
        }

        @Override
        public void close() {
            for ( IRecordWriter recordWriter : recordWriters ) {
                recordWriter.finishOutput();
                printWriter.flush();
                // TODO Close printwriter and/or underlying streams except for System.out
            }
        }
    }
}


