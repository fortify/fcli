package com.fortify.cli.common.output.writer.output.standard;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.spi.ISingularSupplier;
import com.fortify.cli.common.output.writer.IMessageResolver;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.query.OutputWriterWithQuery;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.IRecordWriterFactory;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig.RecordWriterConfigBuilder;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.runner.IfFailureHandler;
import com.fortify.cli.common.util.CommandSpecHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.common.variable.FcliVariableHelper;
import com.fortify.cli.common.variable.FcliVariableHelper.VariableType;
import com.fortify.cli.common.variable.IPredefinedVariableUnsupported;
import com.fortify.cli.common.variable.PredefinedVariable;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Model.CommandSpec;

@ReflectiveAccess 
public class StandardOutputWriter implements IOutputWriter {
    private final StandardOutputConfig outputConfig;
    private final OutputFormat outputFormat;
    private final CommandSpec commandSpec;
    private final IOutputOptions outputOptions;
    
    public StandardOutputWriter(CommandSpec commandSpec, IOutputOptions outputOptions, StandardOutputConfig defaultOutputConfig) {
        // Make sure that we get the CommandSpec for the actual command being invoked,
        // not some intermediate Mixin
        this.commandSpec = commandSpec.commandLine()==null ? commandSpec : commandSpec.commandLine().getCommandSpec();
        this.outputOptions = outputOptions;
        this.outputConfig = getOutputConfigOrDefault(commandSpec, defaultOutputConfig);
        this.outputFormat = getOutputFormatOrDefault(outputConfig, outputOptions);
    }
    
    /**
     * Write the given {@link JsonNode} to the configured output(s)
     */
    @Override
    public void write(JsonNode jsonNode) {
        try ( IRecordWriter recordWriter = new OutputAndVariableRecordWriter() ) {
            writeRecords(recordWriter, jsonNode);
        }
    }

    /**
     * Write the output of the given {@link HttpRequest} to the configured output(s)
     */
    @Override
    public void write(HttpRequest<?> httpRequest) {
        write(httpRequest, null);
    }
    
    /**
     * Write the output of the given, potentially paged {@link HttpRequest}, to the 
     * configured output(s), invoking the given {@link INextPageUrlProducer} to retrieve 
     * all pages
     */
    @Override
    public void write(HttpRequest<?> httpRequest, INextPageUrlProducer nextPageUrlProducer) {
        try ( IRecordWriter recordWriter = new OutputAndVariableRecordWriter() ) {
            if ( nextPageUrlProducer==null ) {
                writeRecords(recordWriter, httpRequest);
            } else {
                writeRecords(recordWriter, httpRequest, nextPageUrlProducer);
            }
        }
    }
    
    /** 
     * Write the given {@link HttpResponse} to the configured output(s)
     */
    @Override
    public void write(HttpResponse<JsonNode> httpResponse) {
        try ( IRecordWriter recordWriter = new OutputAndVariableRecordWriter() ) {
            writeRecords(recordWriter, httpResponse);
        }
    }
    
    /**
     * Write records returned by the given {@link HttpRequest} to the given
     * {@link IRecordWriter}.
     * @param recordWriter
     * @param httpRequest
     */
    private final void writeRecords(IRecordWriter recordWriter, HttpRequest<?> httpRequest) {
        httpRequest.asObject(JsonNode.class)
            .ifSuccess(r->writeRecords(recordWriter, r))
            .ifFailure(IfFailureHandler::handle); // Just in case no error interceptor was registered for this request
    }
    
    /**
     * Write records returned by the given, potentially paged {@link HttpRequest}
     * to the given {@link IRecordWriter}, invoking the given {@link INextPageUrlProducer} 
     * to retrieve all pages
     * @param recordWriter
     * @param httpRequest
     * @param nextPageUrlProducer
     */
    private final void writeRecords(IRecordWriter recordWriter, HttpRequest<?> httpRequest, INextPageUrlProducer nextPageUrlProducer) {
        PagingHelper.pagedRequest(httpRequest, nextPageUrlProducer)
            .ifSuccess(r->writeRecords(recordWriter, r))
            .ifFailure(IfFailureHandler::handle); // Just in case no error interceptor was registered for this request
    }

    /**
     * Write records provided by the given {@link HttpResponse} to the given
     * {@link IRecordWriter}
     * @param recordWriter
     * @param httpResponse
     */
    private final void writeRecords(IRecordWriter recordWriter, HttpResponse<JsonNode> httpResponse) {
        writeRecords(recordWriter, httpResponse.getBody());
    }
    
    /**
     * Transform the given {@link JsonNode} using the configured input transformations,
     * then write the transformed input to the given {@link IRecordWriter}. If the 
     * transformed input is null, nothing will be written. If the transformed input 
     * represents a JSON array, each of its entries will be written. If the transformed 
     * input represents a JSON object, the individual object will be written. For other
     * node types, an exception will be thrown.  
     * @param recordWriter
     * @param jsonNode
     */
    private final void writeRecords(IRecordWriter recordWriter, JsonNode jsonNode) {
        jsonNode = outputConfig.applyInputTransformations(outputFormat, jsonNode);
        if ( jsonNode!=null ) {
            if ( jsonNode.isArray() ) {
                jsonNode.elements().forEachRemaining(record->writeRecord(recordWriter, record));
            } else if ( jsonNode.isObject() ) {
                writeRecord(recordWriter, jsonNode);
            } else {
                throw new IllegalStateException("Unsupported node type: "+jsonNode.getNodeType());
            }
        }
    }

    /**
     * Transform the given {@link JsonNode} using the configured record transformers and filters, 
     * then write the transformed record to the given {@link IRecordWriter}. If the transformed 
     * record is null or an empty array, nothing will be written. If the transformed record is a 
     * non-empty array, the first array entry will be written. Otherwise, the transformed record
     * will be written as-is.
     * @param recordWriter
     * @param record
     */
    @SneakyThrows // TODO Do we want to use SneakyThrows?
    private final void writeRecord(IRecordWriter recordWriter, JsonNode record) {
        // TODO Add null checks in case any input or record transformation returns null?
        record = record==null ? null : outputConfig.applyRecordTransformations(outputFormat, record);
        record = record==null ? null : applyRecordOutputFilters(outputFormat, record);
        if ( record!=null ) {
            if(record.getNodeType() == JsonNodeType.ARRAY) {
                if(record.size()>0) recordWriter.writeRecord((ObjectNode) new ObjectMapper().readTree(record.get(0).toString()));
            } else {
                recordWriter.writeRecord((ObjectNode) record);
            }
        }
    }
    
    /**
     * Return the {@link StandardOutputConfig} from the current command if the command
     * implements {@link IOutputConfigSupplier}, otherwise return the provided default
     * output configuration.
     * @param commandSpec
     * @param defaultOutputConfig
     * @return
     */
    private static final StandardOutputConfig getOutputConfigOrDefault(CommandSpec commandSpec, StandardOutputConfig defaultOutputConfig) {
        Object cmd = commandSpec.userObject();
        if ( cmd instanceof IOutputConfigSupplier ) {
            return ((IOutputConfigSupplier)cmd).getOutputConfig();
        } else {
            return defaultOutputConfig;
        }
    }
    
    /**
     * Return the {@link OutputFormat} from the given {@link IOutputOptions} if available,
     * otherwise return the {@link OutputFormat} from the given {@link StandardOutputConfig}
     * if available, otherwise return {@link OutputFormat#table} 
     * @param outputConfig
     * @param outputOptions
     * @return
     */
    private static final OutputFormat getOutputFormatOrDefault(StandardOutputConfig outputConfig, IOutputOptions outputOptions) {
        OutputFormat result = outputOptions==null || outputOptions.getOutputFormatConfig()==null 
                ? outputConfig.defaultFormat() 
                : outputOptions.getOutputFormatConfig().getOutputFormat();
        if ( result == null ) {
            result = OutputFormat.table;
        }
        return result;
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
    
    /**
     * {@link IRecordWriter} implementation that combines {@link OutputRecordWriter} and
     * {@link VariableRecordWriter}, allowing records to be simultaneously written to both
     * the configured output and a variable (if enabled)
     * @author rsenden
     *
     */
    private final class OutputAndVariableRecordWriter implements IRecordWriter {
        private final OutputRecordWriter ouputRecordWriter = new OutputRecordWriter();
        private final VariableRecordWriter variableRecordWriter = new VariableRecordWriter();
        
        /**
         * Write the given record to our {@link OutputRecordWriter} instance, and
         * to our {@link VariableRecordWriter} instance if it is enabled
         */
        @Override
        public void writeRecord(ObjectNode record) {
            ouputRecordWriter.writeRecord(record);
            if ( variableRecordWriter.isEnabled() ) {
                variableRecordWriter.writeRecord(record);
            }
        }
        
        /**
         * Close our {@link OutputRecordWriter} instance, and our {@link VariableRecordWriter}
         * instance if it is enabled
         */
        @Override
        public void close() {
            ouputRecordWriter.close();
            if ( variableRecordWriter.isEnabled() ) {
                variableRecordWriter.close();
            }
        }
    }
    
    /**
     * Abstract base class for {@link OutputRecordWriter} and {@link VariableRecordWriter},
     * providing common functionality.
     * @author rsenden
     *
     */
    private abstract class AbstractRecordWriterWrapper implements IRecordWriter, IMessageResolver {
        /**
         * Get the wrapped {@link IRecordWriter} instance from our subclass,
         * and write the given record to it.
         */
        @Override
        public final void writeRecord(ObjectNode record) {
            getWrappedRecordWriter().writeRecord(record);
        }
        
        /**
         * Get the wrapped {@link IRecordWriter} and close it, then call the
         * {@link #closeOutput()} method to allow any underlying resources to
         * be closed.
         */
        @Override
        public final void close() {
            getWrappedRecordWriter().close();
            closeOutput();
        }

        /**
         * Implementation for {@link IMessageResolver#getMessageString(String)}.
         */
        @Override
        public final String getMessageString(String keySuffix) {
            return CommandSpecHelper.getMessageString(commandSpec, keySuffix);
        }
        
        /**
         * Create a {@link RecordWriterConfigBuilder} instance with some
         * properties pre-configured.
         * @return
         */
        protected final RecordWriterConfigBuilder createRecordWriterConfigBuilder() {
            return RecordWriterConfig.builder()
                    .singular(isSingularOutput())
                    .messageResolver(this)
                    .cmd(commandSpec.userObject());
        }
        
        /**
         * Method to be implemented by subclasses to return the wrapped {@link IRecordWriter}
         * instance.
         * @return
         */
        protected abstract IRecordWriter getWrappedRecordWriter();
        
        /**
         * Method to be implemented by subclasses to close any underlying resources.
         */
        protected abstract void closeOutput();
        
        /**
         * If the command being invoked implements the {@link ISingularSupplier} interface,
         * return the value returned by the {@link ISingularSupplier#isSingular()} method,
         * otherwise return false.
         * @return
         */
        protected boolean isSingularOutput() {
            Object cmd = commandSpec.userObject();
            return cmd instanceof ISingularSupplier
                    ? ((ISingularSupplier)cmd).isSingular()
                    : false;
        }
    }
    
    /**
     * This {@link AbstractRecordWriterWrapper} implementation handles writing records
     * to the configured output.
     * @author rsenden
     *
     */
    private final class OutputRecordWriter extends AbstractRecordWriterWrapper {
        private final Writer writer;
        @Getter private final IRecordWriter wrappedRecordWriter;
        
        /**
         * This constructor creates the wrapped {@link IRecordWriter} and its 
         * underlying {@link Writer}.
         */
        public OutputRecordWriter() {
            this.writer = createWriter();
            this.wrappedRecordWriter = getRecordWriterFactory().createRecordWriter(createRecordWriterConfig());
        }
        
        /**
         * Flush the underlying writer, and close it if it doesn't represent System.out 
         */
        @Override
        protected void closeOutput() {
            try {
                writer.flush();
                // Close output when writing to file; we don't want to close System.out
                if ( writer instanceof BufferedWriter ) {
                    writer.close();
                }
            } catch (IOException e) {
                System.err.println("WARN: Error closing output");
            }   
        }
        
        /**
         * Create a {@link RecordWriterConfig} instance based on output configuration
         * @return
         */
        private RecordWriterConfig createRecordWriterConfig() {
            return createRecordWriterConfigBuilder()
                    .writer(writer)
                    .options(getOutputWriterOptions())
                    .outputFormat(outputFormat)
                    .build();
        }
        
        /**
         * Get the {@link IRecordWriterFactory} instance from the configured
         * {@link OutputFormat}
         * @return
         */
        private IRecordWriterFactory getRecordWriterFactory() {
            return outputFormat.getRecordWriterFactory();
        }
        
        /**
         * Create the underlying writer; either a {@link PrintWriter} instance
         * that wraps {@link System#out}, or a {@link BufferedWriter} when file
         * output is enabled
         * @return
         */
        private Writer createWriter() {
            String outputFile = outputOptions.getOutputFile();
            try {
                return outputFile == null || "-".equals(outputFile)
                        ? new PrintWriter(System.out)
                        : new BufferedWriter(new FileWriter(outputFile, false));
            } catch ( IOException e) {
                throw new IllegalArgumentException("Output file "+outputFile+" cannot be accessed");
            }
        }
        
        /**
         * Return the output writer options configured in our {@link OutputFormatConfig}
         * instance (representing user-supplied options) if available, otherwise return
         * the default options configured in the resource bundle, otherwise return null.
         * @return
         */
        private String getOutputWriterOptions() {
            OutputFormatConfig config = outputOptions.getOutputFormatConfig();
            if ( config!=null && StringUtils.isNotBlank(config.getOptions()) ) {
                return config.getOptions();
            } else {
                String keySuffix = "output."+outputFormat.getMessageKey()+".options";
                return getMessageString(keySuffix);
            }
        }
    }
    
    /**
     * This {@link AbstractRecordWriterWrapper} implementation handles writing records
     * to a variable, if enabled.
     * 
     * @author rsenden
     *
     */
    private final class VariableRecordWriter extends AbstractRecordWriterWrapper {
        private final VariableDefinition variableDefinition;
        private final Writer writer;
        @Getter private final IRecordWriter wrappedRecordWriter;
        
        /**
         * This constructor gets the {@link VariableStoreConfig} from our {@link IOutputOptions}
         * instance; if this configuration is not null, our {@link VariableDefinition},
         * {@link Writer} and wrapped {@link IRecordWriter} will be initialized accordingly. 
         */
        public VariableRecordWriter() {
            VariableStoreConfig variableStoreConfig = outputOptions.getVariableStoreConfig();
            this.variableDefinition = variableStoreConfig==null ? null : createVariableDefinition(variableStoreConfig);
            this.writer = variableStoreConfig==null ? null : createWriter(this.variableDefinition);
            this.wrappedRecordWriter = variableStoreConfig==null ? null : getRecordWriterFactory().createRecordWriter(createRecordWriterConfig());
        }
        
        /**
         * Return whether storing data in a variable is enabled
         * @return
         */
        public final boolean isEnabled() {
            return variableDefinition!=null;
        }
        
        /**
         * Close the underlying writer. This method should only be called 
         * if {@link #isEnabled()} returns true.
         */
        @Override
        protected void closeOutput() {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                System.err.println("WARN: Error closing output file");
            }   
        }
        
        /**
         * Create a {@link RecordWriterConfig} instance based on variable configuration.
         * This method should not be called if our {@link VariableDefinition} is null.
         * @return
         */
        private RecordWriterConfig createRecordWriterConfig() {
            return createRecordWriterConfigBuilder()
                    .writer(writer)
                    .options(variableDefinition.getVariableOptions())
                    .outputFormat(OutputFormat.json)
                    .build();
        }
        
        /**
         * Return the {@link IRecordWriterFactory} instance provided by
         * {@link OutputFormat#json}.
         * @return
         */
        private IRecordWriterFactory getRecordWriterFactory() {
            return OutputFormat.json.getRecordWriterFactory();
        }
        
        /**
         * Create the underlying writer based on the given {@link VariableDefinition}.
         * This method should not be called if a null parameter. 
         * @param variableDefinition
         * @return
         */
        private Writer createWriter(VariableDefinition variableDefinition) {
            return FcliVariableHelper.getVariableContentsPrintWriter(variableDefinition.getVariableType(), variableDefinition.getVariableName());
        }
        
        /**
         * Create a {@link VariableDefinition} instance based on the given {@link VariableStoreConfig}.
         * If the variable name equals '{@value FcliVariableHelper#PREDEFINED_VARIABLE_PLACEHOLDER}',
         * the variable name and options will be retrieved from the {@link PredefinedVariable} annotation
         * provided by the command being invoked or any of its parent commands. This method will perform
         * various validations, throwing an exception if criteria are not met. 
         * @param variableStoreConfig
         * @return
         */
        private VariableDefinition createVariableDefinition(VariableStoreConfig variableStoreConfig) {
            Object cmd = commandSpec.userObject();
            String variableName = variableStoreConfig.getVariableName();
            String options = variableStoreConfig.getOptions();
            VariableType variableType = VariableType.USER_PROVIDED;
            if ( FcliVariableHelper.PREDEFINED_VARIABLE_PLACEHOLDER.equals(variableName) ) {
                if ( StringUtils.isNotBlank(options) ) { 
                    throw new IllegalArgumentException(String.format("Option --store doesn't support options for variable placeholder '%s'", FcliVariableHelper.PREDEFINED_VARIABLE_PLACEHOLDER));
                }
                if ( cmd instanceof IPredefinedVariableUnsupported || !isSingularOutput() ) {
                    throw new IllegalArgumentException(String.format("Option --store doesn't support variable placeholder '%s' on this command", FcliVariableHelper.PREDEFINED_VARIABLE_PLACEHOLDER));
                }
                PredefinedVariable predefinedVariable = CommandSpecHelper.findAnnotation(commandSpec, PredefinedVariable.class);
                if ( predefinedVariable==null ) {
                    throw new IllegalArgumentException(String.format("Option --store doesn't support variable placeholder '%s' on this command tree", FcliVariableHelper.PREDEFINED_VARIABLE_PLACEHOLDER));
                } else {
                    variableName = FcliVariableHelper.resolveVariableName(cmd, predefinedVariable.name());
                    options = predefinedVariable.field();
                    variableType = VariableType.PREDEFINED;
                }
            }
            return new VariableDefinition(variableName, options, variableType);
        }
        
        /**
         * This class holds variable name, options, and type.
         */
        @Data
        private final class VariableDefinition {
            private final String variableName;
            private final String variableOptions;
            private final VariableType variableType;
        }
    }
}


