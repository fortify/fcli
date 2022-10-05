package com.fortify.cli.common.output.cli.mixin.spi;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Model.CommandSpec;

@ReflectiveAccess
public abstract class AbstractOutputHelper implements IOutputHelper {
    @Getter private IProductHelper productHelper;
    
    public void setProductHelper(IProductHelper productHelper) {
        this.productHelper = productHelper;
        productHelper.setOutputHelper(this);
    }
    
    @Override
    public void write(UnirestInstance unirest, HttpRequest<?> baseRequest) {
        HttpRequest<?> request = productHelper.updateRequest(unirest, baseRequest);
        Function<HttpResponse<JsonNode>, String> nextPageUrlProducer = productHelper.getNextPageUrlProducer(unirest, request);
        createOutputWriter().write(request, nextPageUrlProducer);
    }
    
    @Override
    public void write(UnirestInstance unirest, JsonNode jsonNode) {
        createOutputWriter().write(jsonNode);
    }
    
    @Override
    public void write(UnirestInstance unirest, JsonNodeHolder jsonNodeHolder) {
        write(unirest, jsonNodeHolder.asJsonNode());
    }
    
    @Override
    public OutputConfig getBasicOutputConfig() {
        Object cmd = getMixee().userObject();
        if ( cmd instanceof IBasicOutputConfigSupplier ) {
            return ((IBasicOutputConfigSupplier)cmd).getBasicOutputConfig();
        } else {
            throw new IllegalStateException(cmd.getClass().getName()+" must implement IBasicOutputConfigSupplier, or use an IOutputHelper implementation that provides a basic output configuration");
        }
    }
    
    @Override
    public IOutputWriterFactory getOutputWriterFactory() {
        Object cmd = getMixee().userObject();
        if ( cmd instanceof IOutputWriterFactorySupplier ) {
            return ((IOutputWriterFactorySupplier)cmd).getOutputWriterFactory();
        } else {
            throw new IllegalStateException(cmd.getClass().getName()+" must implement IOutputWriterFactorySupplier, or use an IOutputHelper implementation that provides an output factory");
        }
    }

    protected IOutputWriter createOutputWriter() {
        return getOutputWriterFactory().createOutputWriter(getMixee(), getOutputConfig());
    }
    
    /** 
     * Get the output configuration based on {@link IBasicOutputConfigSupplier},
     * {@link IInputTransformerSupplier} and {@link IRecordTransformerSupplier}
     * implementations. If the command implementation (mixee) implements any of
     * these interfaces, that implementation will be used. Otherwise, for 
     * {@link IBasicOutputConfigSupplier}, the implementation in this class or
     * one of its subclasses will be used. For {@link IInputTransformerSupplier}
     * and {@link IRecordTransformerSupplier}, the implementation provided
     * by the {@link IProductHelper} will be used.
     *  
     * @return
     */
    protected OutputConfig getOutputConfig() {
        Object cmd = getMixee().userObject();
        OutputConfig basicOutputConfig = cmd instanceof IBasicOutputConfigSupplier
                ? ((IBasicOutputConfigSupplier)cmd).getBasicOutputConfig()
                : getBasicOutputConfig();
        UnaryOperator<JsonNode> inputTransformer = cmd instanceof IInputTransformerSupplier
                ? ((IInputTransformerSupplier)cmd).getInputTransformer()
                : productHelper.getInputTransformer();
        UnaryOperator<JsonNode> recordTransformer = cmd instanceof IRecordTransformerSupplier
                ? ((IRecordTransformerSupplier)cmd).getRecordTransformer()
                : productHelper.getRecordTransformer();
        return basicOutputConfig.inputTransformer(inputTransformer).recordTransformer(recordTransformer);
    }
    
    protected abstract CommandSpec getMixee();
}
