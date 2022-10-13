package com.fortify.cli.common.output.cli.mixin;

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
import com.fortify.cli.common.output.cli.mixin.query.OutputMixinWithQuery;
import com.fortify.cli.common.output.cli.mixin.spi.output.IMinusVariableUnsupported;
import com.fortify.cli.common.output.cli.mixin.spi.output.ISingularSupplier;
import com.fortify.cli.common.output.cli.mixin.spi.output.MinusVariableDefinition;
import com.fortify.cli.common.output.writer.IMessageResolver;
import com.fortify.cli.common.output.writer.OutputFormat;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.OutputFormatConfig;
import com.fortify.cli.common.output.writer.output.OutputOptionsArgGroup;
import com.fortify.cli.common.output.writer.output.OutputStoreConfig;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.runner.IfFailureHandler;
import com.fortify.cli.common.util.CommandSpecHelper;
import com.fortify.cli.common.util.FcliVariableHelper;
import com.fortify.cli.common.util.FcliVariableHelper.VariableType;
import com.fortify.cli.common.util.StringUtils;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * TODO Refactor this class once all commands have been refactored to use CommandOutputWriterMixin;
 *      all picocli annotatations should be removed, as they will be passed by an IOutputMixinFactory
 *      through our constructor. As OutputMixin by then will no longer be a mixin, OutputMixin and
 *      subclasses should be renamed to OutputWriter, and moved to a new writer.output package.
 * @author rsenden
 *
 */
@ReflectiveAccess 
public class OutputMixin implements IOutputWriter {
    @Getter private final OutputConfig defaultOutputConfig;
    
    // TODO Make final once all command implementations use an IOutputMixinFactory
    @Getter private CommandSpec commandSpec;
    
    // TODO Make final & use interface instead once ArgGroup has been fully moved to factory
    @ArgGroup(headingKey = "arggroup.output.heading", exclusive = false)
    private OutputOptionsArgGroup outputOptionsArgGroup;
    
    
    // TODO Remove once all command implementations use an IOutputMixinFactory
    public OutputMixin() {
        this.defaultOutputConfig =new OutputConfig();
    }
    
    public OutputMixin(CommandSpec mixee, OutputOptionsArgGroup outputOptionsArgGroup, OutputConfig defaultOutputConfig) {
        setMixee(mixee);
        this.outputOptionsArgGroup = outputOptionsArgGroup;
        this.defaultOutputConfig = defaultOutputConfig;
    }

    // TODO Remove once all command implementations use an IOutputMixinFactory
    @Spec(Spec.Target.MIXEE)
    public void setMixee(CommandSpec mixee) {
        // Make sure that we get the CommandSpec for the actual command being invoked,
        // not some intermediate Mixin
        this.commandSpec = mixee.commandLine()==null ? mixee : mixee.commandLine().getCommandSpec();
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
    
    private OutputConfig getOutputOptionsWriterConfig() {
        Object mixeeObject = commandSpec.userObject();
        if ( mixeeObject instanceof IOutputConfigSupplier ) {
            return ((IOutputConfigSupplier)mixeeObject).getOutputConfig();
        } else {
            return defaultOutputConfig;
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

    public final class OutputOptionsWriter implements AutoCloseable, IMessageResolver { // TODO Implement interface, make implementation private
        private final OutputMixin optionsHandler = OutputMixin.this;
        private final OutputOptionsArgGroup optionsArgGroup = optionsHandler.outputOptionsArgGroup!=null ? optionsHandler.outputOptionsArgGroup : new OutputOptionsArgGroup();
        private final OutputConfig config;
        private final OutputFormat outputFormat;
        private final PrintWriter printWriter;
        private final IRecordWriter[] recordWriters;
        
        public OutputOptionsWriter(OutputConfig config) {
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
        
        private IRecordWriter[] getRecordWriters(OutputConfig config) {
            List<IRecordWriter> recordWritersList = new ArrayList<>();
            recordWritersList.add(outputFormat.getRecordWriterFactory().createRecordWriter(createOutputWriterConfig()));
            OutputStoreConfig outputStoreConfig = optionsArgGroup.getOutputStoreConfig();
            if ( outputStoreConfig!=null ) {
                recordWritersList.add(OutputFormat.json.getRecordWriterFactory().createRecordWriter(createOutputStoreWriterConfig(outputStoreConfig)));           }
            return recordWritersList.toArray(IRecordWriter[]::new);
        }

        private OutputFormat getOutputFormat() {
            OutputFormat result = optionsArgGroup.getOutputFormatConfig()==null 
                    ? config.defaultFormat() 
                    : optionsArgGroup.getOutputFormatConfig().getOutputFormat();
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
        private RecordWriterConfig createOutputStoreWriterConfig(OutputStoreConfig outputStoreConfig) {
            String variableName = outputStoreConfig.getVariableName();
            String options = outputStoreConfig.getOptions();
            VariableType variableType = VariableType.USER_PROVIDED;
            if ( "-".equals(variableName) ) {
                CommandSpec cmdSpec = getCommandSpec();
                Object cmd = cmdSpec.userObject();
                if ( StringUtils.isNotBlank(options) ) { 
                    throw new IllegalArgumentException("Option --store doesn't support options for variable alias '-'");
                }
                if ( cmd instanceof IMinusVariableUnsupported || !isSingularOutput() ) {
                    throw new IllegalArgumentException("Option --store doesn't support variable alias '-' on this command");
                }
                MinusVariableDefinition minusVariableDefinition = CommandSpecHelper.findAnnotation(cmdSpec, MinusVariableDefinition.class);
                if ( minusVariableDefinition==null ) {
                    throw new IllegalArgumentException("Option --store doesn't support variable alias '-' on this command");
                } else {
                    variableName = minusVariableDefinition.name();
                    options = minusVariableDefinition.options();
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
            CommandSpec cmdSpec = getCommandSpec();
            Object cmd = cmdSpec.userObject();
            return cmd instanceof ISingularSupplier
                    ? ((ISingularSupplier)cmd).isSingular()
                    : config.singular();
        }
        
        private String getOutputWriterOptions() {
            OutputFormatConfig config = optionsArgGroup.getOutputFormatConfig();
            if ( config!=null && StringUtils.isNotBlank(config.getOptions()) ) {
                return config.getOptions();
            } else {
                String keySuffix = "output."+getOutputFormat().getMessageKey()+".options";
                return getMessageString(keySuffix);
            }
        }

        @Override
        public String getMessageString(String keySuffix) {
            return CommandSpecHelper.getMessageString(getCommandSpec(), keySuffix);
        }

        private final PrintWriter createPrintWriter(OutputConfig config) {
            String outputFile = optionsArgGroup.getOutputFile();
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


