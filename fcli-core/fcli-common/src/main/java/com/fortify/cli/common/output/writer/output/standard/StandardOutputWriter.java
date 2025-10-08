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
package com.fortify.cli.common.output.writer.output.standard;

import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.writer.CommandSpecMessageResolver;
import com.fortify.cli.common.output.writer.IMessageResolver;
import com.fortify.cli.common.output.writer.ISingularSupplier;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.OutputRecordWriterFactory;
import com.fortify.cli.common.output.writer.output.OutputRecordWriterFactory.OutputRecordWriterFactoryBuilder;
import com.fortify.cli.common.output.writer.output.query.OutputWriterWithQuery;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.output.writer.record.RecordWriterStyle;
import com.fortify.cli.common.output.writer.record.util.AppendOnCloseWriterWrapper;
import com.fortify.cli.common.output.writer.record.util.NonClosingWriterWrapper;
import com.fortify.cli.common.rest.paging.INextPageRequestProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.unirest.IfFailureHandler;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.common.variable.EncryptVariable;
import com.fortify.cli.common.variable.FcliVariableHelper;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Model.CommandSpec;

public class StandardOutputWriter implements IOutputWriter {
    //private static final Logger LOG = LoggerFactory.getLogger(StandardOutputWriter.class);
    private final StandardOutputConfig outputConfig;
    private final RecordWriterFactory recordWriterFactory;
    private final CommandSpec commandSpec;
    private final IOutputOptions outputOptions;
    private final IMessageResolver messageResolver;
    private static IRecordWriter recordCollector;
    private static boolean suppressOutput;
    
    public StandardOutputWriter(CommandSpec commandSpec, IOutputOptions outputOptions, StandardOutputConfig defaultOutputConfig) {
        // Make sure that we get the CommandSpec for the actual command being invoked,
        // not some intermediate Mixin
        this.commandSpec = commandSpec.commandLine()==null ? commandSpec : commandSpec.commandLine().getCommandSpec();
        this.outputOptions = outputOptions;
        this.outputConfig = getOutputConfigOrDefault(commandSpec, defaultOutputConfig);
        this.recordWriterFactory = getRecordWriterFactoryOrDefault(outputConfig, outputOptions);
        this.messageResolver = new CommandSpecMessageResolver(this.commandSpec);
    }
    
    public static final void collectRecords(Consumer<ObjectNode> consumer, boolean suppressOutput) {
        final var oldRecordCollector = StandardOutputWriter.recordCollector;
        final var oldSuppressOutput = StandardOutputWriter.suppressOutput;
        StandardOutputWriter.suppressOutput = suppressOutput;
        StandardOutputWriter.recordCollector = new IRecordWriter() {  
            @Override
            public void append(ObjectNode record) {
                consumer.accept(record);
            }
            
            @Override
            public void close() {
                StandardOutputWriter.recordCollector = oldRecordCollector;
                StandardOutputWriter.suppressOutput = oldSuppressOutput;
            }
        };
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
        try ( IRecordWriter recordWriter = new OutputAndVariableRecordWriter() ) {
            writeRecords(recordWriter, httpRequest);
        }
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
     * Write the output of the given, potentially paged {@link HttpRequest}, to the 
     * configured output(s), invoking the given {@link INextPageRequestProducer} to retrieve 
     * all pages
     */
    @Override
    public void write(HttpRequest<?> httpRequest, INextPageRequestProducer nextPageRequestProducer) {
        try ( IRecordWriter recordWriter = new OutputAndVariableRecordWriter() ) {
            if ( nextPageRequestProducer==null ) {
                writeRecords(recordWriter, httpRequest);
            } else {
                writeRecords(recordWriter, httpRequest, nextPageRequestProducer);
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
     * Write records returned by the given, potentially paged {@link HttpRequest}
     * to the given {@link IRecordWriter}, invoking the given {@link INextPageRequestProducer} 
     * to retrieve all pages
     * @param recordWriter
     * @param httpRequest
     * @param nextPageRequestProducer
     */
    private final void writeRecords(IRecordWriter recordWriter, HttpRequest<?> initialRequest, INextPageRequestProducer nextPageRequestProducer) {
        PagingHelper.processPages(initialRequest, nextPageRequestProducer, r->writeRecords(recordWriter, r));
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
        jsonNode = outputConfig.applyInputTransformations(jsonNode);
        if ( jsonNode!=null ) {
            if ( jsonNode.isArray() ) {
                jsonNode.elements().forEachRemaining(record->writeRecord(recordWriter, record));
            } else if ( jsonNode.isObject() ) {
                writeRecord(recordWriter, jsonNode);
            } else {
                throw new FcliBugException("Unsupported node type: "+jsonNode.getNodeType());
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
        record = record==null ? null : outputConfig.applyRecordTransformations(record);
        record = record==null ? null : applyRecordOutputFilters(record);
        if ( record!=null ) {
            JsonNodeType nodeType = record.getNodeType();
            switch ( nodeType ) {
            case ARRAY: if(record.size()>0) recordWriter.append((ObjectNode) new ObjectMapper().readTree(record.get(0).toString())); break;
            case OBJECT: recordWriter.append((ObjectNode) record); break;
            case NULL: case MISSING: break;
            default: throw new FcliBugException("Invalid node type: "+nodeType);
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
     * Return the {@link OutputFormatConfig} from the given {@link IOutputOptions} if available,
     * otherwise return the {@link OutputFormatConfig} from the given {@link StandardOutputConfig}
     * if available, otherwise return {@link OutputFormatConfig#table} 
     * @param outputConfig
     * @param outputOptions
     * @return
     */
    private static final RecordWriterFactory getRecordWriterFactoryOrDefault(StandardOutputConfig outputConfig, IOutputOptions outputOptions) {
        var result = outputOptions==null || outputOptions.getOutputFormatConfig()==null 
                ? outputConfig.defaultFormat() 
                : outputOptions.getOutputFormatConfig().getRecordWriterFactory();
        if ( result == null ) {
            result = RecordWriterFactory.table;
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
    protected JsonNode applyRecordOutputFilters(JsonNode record) {
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
        private final IRecordWriter outputRecordWriter = createOutputRecordWriter();
        private final IRecordWriter recordCollector = StandardOutputWriter.recordCollector;
        private final VariableRecordWriter variableRecordWriter = new VariableRecordWriter();
        
        /**
         * Write the given record to our {@link OutputRecordWriter} instance, and
         * to our {@link VariableRecordWriter} instance if it is enabled
         */
        @Override
        public void append(ObjectNode record) {
            if ( outputRecordWriter!=null ) {outputRecordWriter.append(record);}
            if ( recordCollector!=null ) {recordCollector.append(record);}
            if ( variableRecordWriter.isEnabled() ) {
                variableRecordWriter.append(record);
            }
        }
        
        /**
         * Close our {@link OutputRecordWriter} instance, and our {@link VariableRecordWriter}
         * instance if it is enabled
         */
        @Override
        public void close() {
            if ( outputRecordWriter!=null ) {outputRecordWriter.close();}
            if ( recordCollector!=null ) {recordCollector.close();}
            if ( variableRecordWriter.isEnabled() ) {
                variableRecordWriter.close();
            }
        }
        
        private final IRecordWriter createOutputRecordWriter() {
            return StandardOutputWriter.suppressOutput ? null : createUnsuppressedOutputRecordWriter();
        }

        private IRecordWriter createUnsuppressedOutputRecordWriter() {
            Object cmd = commandSpec.userObject();
            var recordWriterArgs = outputOptions==null || outputOptions.getOutputFormatConfig()==null
                    ? null : outputOptions.getOutputFormatConfig().getRecordWriterArgs();
            return OutputRecordWriterFactory.builder()
                    .singular(isSingularOutput())
                    .messageResolver(messageResolver)
                    .addActionColumn(cmd!=null && cmd instanceof IActionCommandResultSupplier)
                    .recordWriterArgs(recordWriterArgs)
                    .recordWriterFactory(recordWriterFactory)
                    .recordWriterStyle(RecordWriterStyle.apply(outputOptions.getOutputStyleElements()))
                    .writerSupplier(()->createWriter())
                    .build()
                    .createRecordWriter();
        }
        
        @SneakyThrows
        private final Writer createWriter() {
            var outputFile = outputOptions.getOutputFile();
            if ( outputFile==null ) {
                return new AppendOnCloseWriterWrapper("\n\n", new NonClosingWriterWrapper(new OutputStreamWriter(System.out)));
            } else {
                return new FileWriter(outputFile);
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
        public final void append(ObjectNode record) {
            getWrappedRecordWriter().append(record);
        }
        
        /**
         * Get the wrapped {@link IRecordWriter} and close it, then call the
         * {@link #closeOutput()} method to allow any underlying resources to
         * be closed.
         */
        @Override
        public final void close() {
            getWrappedRecordWriter().close();
        }
        
        /**
         * Create a {@link RecordWriterConfigBuilder} instance with some
         * properties pre-configured.
         * @return
         */
        protected final OutputRecordWriterFactoryBuilder createRecordWriterConfigBuilder() {
            Object cmd = commandSpec.userObject();
            return OutputRecordWriterFactory.builder()
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
    }
    
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
    
    /**
     * This {@link AbstractRecordWriterWrapper} implementation handles writing records
     * to a variable, if enabled.
     * 
     * @author rsenden
     *
     */
    private final class VariableRecordWriter extends AbstractRecordWriterWrapper {
        private final VariableDefinition variableDefinition;
        @Getter private final IRecordWriter wrappedRecordWriter;
        
        /**
         * This constructor gets the {@link VariableStoreConfig} from our {@link IOutputOptions}
         * instance; if this configuration is not null, our {@link VariableDefinition},
         * {@link Writer} and wrapped {@link IRecordWriter} will be initialized accordingly. 
         */
        public VariableRecordWriter() {
            VariableStoreConfig variableStoreConfig = outputOptions.getVariableStoreConfig();
            this.variableDefinition = variableStoreConfig==null ? null : createVariableDefinition(variableStoreConfig);
            this.wrappedRecordWriter = variableStoreConfig==null ? null : createOutputRecordWriterFactory().createRecordWriter();
        }
        
        /**
         * Return whether storing data in a variable is enabled
         * @return
         */
        public final boolean isEnabled() {
            return variableDefinition!=null;
        }
        
        /**
         * Create a {@link RecordWriterConfig} instance based on variable configuration.
         * This method should not be called if our {@link VariableDefinition} is null.
         * @return
         */
        private OutputRecordWriterFactory createOutputRecordWriterFactory() {
            return createRecordWriterConfigBuilder()
                    .writerSupplier(()->createWriter(variableDefinition))
                    .recordWriterArgs(variableDefinition.getRecordWriterArgs())
                    .recordWriterFactory(RecordWriterFactory.json)
                    .build();
        }
        
        /**
         * Create the underlying writer based on the given {@link VariableDefinition}.
         * This method should not be called with a null parameter. 
         * @param variableDefinition
         * @return
         */
        private Writer createWriter(VariableDefinition variableDefinition) {
            return variableDefinition==null 
                    ? null 
                    : FcliVariableHelper.getVariableContentsWriter(variableDefinition.getVariableName(), variableDefinition.getDefaultPropertyName(), variableDefinition.isSingular(), variableDefinition.encrypt);
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
            String recordWriterArgs = variableStoreConfig.getRecordWriterArgs();
            DefaultVariablePropertyName defaultPropertyNameAnnotation = FcliCommandSpecHelper.findAnnotation(commandSpec, DefaultVariablePropertyName.class);
            String defaultPropertyName = defaultPropertyNameAnnotation==null ? null : defaultPropertyNameAnnotation.value();
            boolean encrypt = FcliCommandSpecHelper.findAnnotation(commandSpec, EncryptVariable.class)!=null;
            return VariableDefinition.builder()
                    .variableName(variableName)
                    .recordWriterArgs(recordWriterArgs)
                    .singular(isSingularOutput())
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
        private final String recordWriterArgs;
        private final String defaultPropertyName;
        private final boolean singular;
        private final boolean encrypt;
    }
}


