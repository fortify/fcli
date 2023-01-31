package com.fortify.cli.common.output.writer.output.standard;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.spi.ISingularSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.writer.CommandSpecMessageResolver;
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
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.common.variable.EncryptVariable;
import com.fortify.cli.common.variable.FcliVariableHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Model.CommandSpec;

@ReflectiveAccess 
public class StandardOutputWriter implements IOutputWriter {
    private static final Logger LOG = LoggerFactory.getLogger(StandardOutputWriter.class);
    private final StandardOutputConfig outputConfig;
    private final OutputFormat outputFormat;
    private final CommandSpec commandSpec;
    private final IOutputOptions outputOptions;
    private final IMessageResolver messageResolver;
    
    public StandardOutputWriter(CommandSpec commandSpec, IOutputOptions outputOptions, StandardOutputConfig defaultOutputConfig) {
        // Make sure that we get the CommandSpec for the actual command being invoked,
        // not some intermediate Mixin
        this.commandSpec = commandSpec.commandLine()==null ? commandSpec : commandSpec.commandLine().getCommandSpec();
        this.outputOptions = outputOptions;
        this.outputConfig = getOutputConfigOrDefault(commandSpec, defaultOutputConfig);
        this.outputFormat = getOutputFormatOrDefault(outputConfig, outputOptions);
        this.messageResolver = new CommandSpecMessageResolver(this.commandSpec);
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
            JsonNodeType nodeType = record.getNodeType();
            switch ( nodeType ) {
            case ARRAY: if(record.size()>0) recordWriter.writeRecord((ObjectNode) new ObjectMapper().readTree(record.get(0).toString())); break;
            case OBJECT: recordWriter.writeRecord((ObjectNode) record); break;
            case NULL: case MISSING: break;
            default: throw new RuntimeException("Invalid node type: "+nodeType);
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
    private abstract class AbstractRecordWriterWrapper implements IRecordWriter {
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
         * Create a {@link RecordWriterConfigBuilder} instance with some
         * properties pre-configured.
         * @return
         */
        protected final RecordWriterConfigBuilder createRecordWriterConfigBuilder() {
            Object cmd = commandSpec.userObject();
            return RecordWriterConfig.builder()
                    .singular(isSingularOutput())
                    .messageResolver(messageResolver)
                    .addActionColumn(cmd!=null && cmd instanceof IActionCommandResultSupplier);
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
                writer.write("\n\n");
                writer.flush();
                // Close output when writing to file; we don't want to close System.out
                if ( writer instanceof BufferedWriter ) {
                    writer.close();
                }
            } catch (IOException e) {
                LOG.warn("WARN: Error closing output");
            }   
        }
        
        /**
         * Create a {@link RecordWriterConfig} instance based on output configuration
         * @return
         */
        private RecordWriterConfig createRecordWriterConfig() {
            String options = outputOptions==null || outputOptions.getOutputFormatConfig()==null
                    ? null 
                    : outputOptions.getOutputFormatConfig().getOptions();
            return createRecordWriterConfigBuilder()
                    .writer(writer)
                    .options(options)
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
                LOG.warn("Error closing output file", e);
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
         * This method should not be called with a null parameter. 
         * @param variableDefinition
         * @return
         */
        private Writer createWriter(VariableDefinition variableDefinition) {
            return FcliVariableHelper.getVariableContentsWriter(variableDefinition.getVariableName(), variableDefinition.getDefaultPropertyName(), variableDefinition.encrypt);
        }
        
        /**
         * Create a {@link VariableDefinition} instance based on the given {@link VariableStoreConfig}.
         * The optional defaultPropertyName value will be retrieved from the {@link DefaultVariablePropertyName} 
         * annotation provided by the command being invoked or any of its parent commands. This method will perform
         * various validations, throwing an exception if criteria are not met. 
         * @param variableStoreConfig
         * @return
         */
        private VariableDefinition createVariableDefinition(VariableStoreConfig variableStoreConfig) {
            String variableName = variableStoreConfig.getVariableName();
            String options = variableStoreConfig.getOptions();
            DefaultVariablePropertyName defaultPropertyNameAnnotation = CommandSpecHelper.findAnnotation(commandSpec, DefaultVariablePropertyName.class);
            String defaultPropertyName = defaultPropertyNameAnnotation==null ? null : defaultPropertyNameAnnotation.value();
            boolean encrypt = CommandSpecHelper.findAnnotation(commandSpec, EncryptVariable.class)!=null;
            return VariableDefinition.builder()
                    .variableName(variableName)
                    .variableOptions(options)
                    .defaultPropertyName(defaultPropertyName)
                    .encrypt(encrypt).build();
        }
    }
    
    /**
     * This class holds variable name, options, and type.
     */
    @Data @Builder
    private static final class VariableDefinition {
        private final String variableName;
        private final String variableOptions;
        private final String defaultPropertyName;
        private final boolean encrypt;
    }
}


