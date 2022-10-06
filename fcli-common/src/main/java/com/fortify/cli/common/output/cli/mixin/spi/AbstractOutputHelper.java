package com.fortify.cli.common.output.cli.mixin.spi;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.transform.fields.AddFieldsTransformer;
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
    @Getter private final IProductHelper productHelper;
    
    public AbstractOutputHelper() {
        Class<?> clazz = this.getClass();
        Class<?> enclosingClass = clazz.getEnclosingClass();
        ProductHelperClass productHelperClassAnnotation = clazz.getAnnotation(ProductHelperClass.class);
        if ( productHelperClassAnnotation==null && enclosingClass!=null ) {
            productHelperClassAnnotation = enclosingClass.getAnnotation(ProductHelperClass.class);
        }
        if ( productHelperClassAnnotation==null ) {
            throw new RuntimeException(this.getClass().getName()+" or its enclosing class must have the @ProductHelper annotation");
        }
        Class<? extends IProductHelper> productHelperClass = productHelperClassAnnotation.value();
        try {
            this.productHelper = productHelperClass.getDeclaredConstructor().newInstance();
            this.productHelper.setOutputHelper(this);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException("Error instantiating product helper class "+productHelperClass.getName(), e);
        }
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
        Object cmd = getCommandSpec().userObject();
        if ( cmd instanceof IBasicOutputConfigSupplier ) {
            return ((IBasicOutputConfigSupplier)cmd).getBasicOutputConfig();
        } else {
            throw new IllegalStateException(cmd.getClass().getName()+" must implement IBasicOutputConfigSupplier, or use an IOutputHelper implementation that provides a basic output configuration");
        }
    }
    
    @Override
    public IOutputWriterFactory getOutputWriterFactory() {
        Object cmd = getCommandSpec().userObject();
        if ( cmd instanceof IOutputWriterFactorySupplier ) {
            return ((IOutputWriterFactorySupplier)cmd).getOutputWriterFactory();
        } else {
            throw new IllegalStateException(cmd.getClass().getName()+" must implement IOutputWriterFactorySupplier, or use an IOutputHelper implementation that provides an output factory");
        }
    }

    protected IOutputWriter createOutputWriter() {
        return getOutputWriterFactory().createOutputWriter(getOutputConfig());
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
        Object cmd = getCommandSpec().userObject();
        OutputConfig outputConfig = getBasicOutputConfig(cmd);
        addCmdOrProductInputTransformer(outputConfig, cmd);
        addCmdOrProductRecordTransformer(outputConfig, cmd);
        addCommandActionResultTransformer(outputConfig, cmd);
        return outputConfig;
    }

    private OutputConfig getBasicOutputConfig(Object cmd) {
        return cmd instanceof IBasicOutputConfigSupplier
            ? ((IBasicOutputConfigSupplier)cmd).getBasicOutputConfig()
            : getBasicOutputConfig();
    }

    private void addCommandActionResultTransformer(OutputConfig outputConfig, Object cmd) {
        IActionCommandResultSupplier cmdSupplier = getCommandAs(IActionCommandResultSupplier.class);
        outputConfig.recordTransformer(
                cmdSupplier!=null
                    ? new AddFieldsTransformer("__action__", cmdSupplier.getActionCommandResult())::transform
                    : null);
    }

    private void addCmdOrProductInputTransformer(OutputConfig outputConfig, Object cmd) {
        IInputTransformerSupplier cmdSupplier = getCommandAs(IInputTransformerSupplier.class);
        outputConfig.inputTransformer(
                cmdSupplier!=null
                    ? cmdSupplier.getInputTransformer()
                    : productHelper.getInputTransformer());
    }
    
    private void addCmdOrProductRecordTransformer(OutputConfig outputConfig, Object cmd) {
        IRecordTransformerSupplier cmdSupplier = getCommandAs(IRecordTransformerSupplier.class);
        outputConfig.recordTransformer(
                cmdSupplier!=null
                    ? cmdSupplier.getRecordTransformer()
                    : productHelper.getRecordTransformer());
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCommandAs(Class<T> asType) {
        Object cmd = getCommandSpec().userObject();
        if ( asType.isAssignableFrom(cmd.getClass()) ) {
            return (T)cmd;
        }
        return null;
    }
    
    public CommandSpec getCommandSpec() {
        CommandSpec mixee = getMixee();
        return mixee.commandLine()==null ? mixee : mixee.commandLine().getCommandSpec();
    }
    
    protected abstract CommandSpec getMixee();
}
