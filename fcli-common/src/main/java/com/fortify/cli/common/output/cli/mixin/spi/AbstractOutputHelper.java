package com.fortify.cli.common.output.cli.mixin.spi;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.UnirestOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.unirest.IUnirestOutputHelper;
import com.fortify.cli.common.output.spi.IBasicOutputConfigSupplier;
import com.fortify.cli.common.output.spi.IOutputWriterFactorySupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IInputTransformer;
import com.fortify.cli.common.output.spi.transform.IInputTransformerSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.common.output.spi.transform.IRecordTransformerSupplier;
import com.fortify.cli.common.output.transform.fields.AddFieldsTransformer;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

import picocli.CommandLine.Model.CommandSpec;

public abstract class AbstractOutputHelper implements IOutputHelperBase {
    
    /**
     * Utility method for retrieving the command being invoked as the given 
     * type, returning null if the command is not an instance of the given 
     * type.
     */
    @Override
    public final <T> T getCommandAs(Class<T> asType) {
        return getAs(getCommand(), asType);
    }
    
    /**
     * Get the {@link CommandSpec} for the command being invoked.
     */
    @Override
    public final CommandSpec getCommandSpec() {
        CommandSpec mixee = getMixee();
        // mixee may represent an intermediate mixin rather than representing the actual command
        // so we use mixee.commandLine() if available to retrieve the CommandSpec for the actual
        // command.
        return mixee.commandLine()==null ? mixee : mixee.commandLine().getCommandSpec();
    }
    
    /**
     * This default implementation of {@link IUnirestOutputHelper#getBasicOutputConfig()}
     * tries to retrieve a basic output configuration from the command being invoked,
     * if it implements the {@link IBasicOutputConfigSupplier} interface. If the
     * command doesn't implement this interface, or if the 
     * {@link IBasicOutputConfigSupplier#getBasicOutputConfig()} method returns null, 
     * this method throws an exception. Note that most concrete {@link IUnirestOutputHelper}
     * implementations will override this method; this default implementation is
     * mostly used for {@link UnirestOutputHelperMixins.Other}.
     */
    @Override
    public StandardOutputConfig getBasicOutputConfig() {
        Object cmd = getCommand();
        return applyWithDefaultSupplier(cmd, 
                IBasicOutputConfigSupplier.class, IBasicOutputConfigSupplier::getBasicOutputConfig,
                ()->{throw new IllegalStateException(cmd.getClass().getName()+" must implement IBasicOutputConfigSupplier, or use an IOutputHelper implementation that provides a basic output configuration");});
    }
    
    /**
     * This default implementation of {@link IUnirestOutputHelper#getOutputWriterFactory()}
     * tries to retrieve an {@link IOutputWriterFactory} instance from the command 
     * being invoked, if it implements the {@link IOutputWriterFactorySupplier} interface. 
     * If the command doesn't implement this interface, or if the 
     * {@link IOutputWriterFactorySupplier#getOutputWriterFactory()} method returns null, 
     * this method throws an exception. Note that most concrete {@link IUnirestOutputHelper}
     * implementations will override this method; this default implementation is
     * mostly used for {@link UnirestOutputHelperMixins.Other}.
     */
    @Override
    public IOutputWriterFactory getOutputWriterFactory() {
        Object cmd = getCommand();
        return applyWithDefaultSupplier(cmd, 
                IOutputWriterFactorySupplier.class, IOutputWriterFactorySupplier::getOutputWriterFactory,
                ()->{throw new IllegalStateException(cmd.getClass().getName()+" must implement IOutputWriterFactorySupplier, or use an IOutputHelper implementation that provides an output factory");});
    }

    /**
     * This method retrieves an {@link IOutputWriterFactory} by calling the
     * {@link #getOutputWriterFactory()} method, then returns the {@link IOutputWriter} 
     * returned by the {@link IOutputWriterFactory#createOutputWriter(StandardOutputConfig)} 
     * method.
     * @return {@link IOutputWriter} instance retrieved from an {@link IOutputWriterFactory} instance
     */
    protected final IOutputWriter createOutputWriter() {
        return getOutputWriterFactory().createOutputWriter(getOutputConfig());
    }
    
    /**
     * Utility method for retrieving the command currently being invoked.
     * @return
     */
    protected final Object getCommand() {
        return getCommandSpec().userObject();
    }
    
    /** 
     * This method returns an {@link StandardOutputConfig} instance that is
     * based on the basic output configuration returned by the
     * {@link #getBasicOutputConfig()} method, with input and record
     * transformers added by the {@link #addInputTransformersForCommand(StandardOutputConfig, Object)}
     * and {@link #addRecordTransformersForCommand(StandardOutputConfig, Object)} methods.
     * @return
     */
    protected final StandardOutputConfig getOutputConfig() {
        Object cmd = getCommand();
        StandardOutputConfig standardOutputConfig = getBasicOutputConfig(cmd);
        addInputTransformersForCommand(standardOutputConfig, cmd);
        addRecordTransformersForCommand(standardOutputConfig, cmd);
        addCommandActionResultRecordTransformer(standardOutputConfig, cmd);
        return standardOutputConfig;
    }

    protected abstract void addRecordTransformersForCommand(StandardOutputConfig standardOutputConfig, Object cmd);

    protected abstract void addInputTransformersForCommand(StandardOutputConfig standardOutputConfig, Object cmd);
    
    protected abstract CommandSpec getMixee();

    /**
     * If the command being invoked implements {@link IBasicOutputConfigSupplier}, the
     * {@link IBasicOutputConfigSupplier#getBasicOutputConfig()} method is called on the
     * command to retrieve the command-specific basic output configuration. If the
     * command doesn't implement {@link IBasicOutputConfigSupplier}, or if the 
     * {@link IBasicOutputConfigSupplier#getBasicOutputConfig()} method provided by the
     * command returns null, the {@link #getBasicOutputConfig()} method in this class
     * will be called to retrieve the basic output configuration.
     * @param cmd
     * @return
     */
    private final StandardOutputConfig getBasicOutputConfig(Object cmd) {
        return applyWithDefaultSupplier(cmd, IBasicOutputConfigSupplier.class, IBasicOutputConfigSupplier::getBasicOutputConfig, this::getBasicOutputConfig);
    }
    
    /**
     * Utility method for getting the given object as the given type,
     * returning null if the given object is not an instance of the
     * given type.
     * @param <T>
     * @param obj
     * @param asType
     * @return
     */
    @SuppressWarnings("unchecked")
    protected static final <T> T getAs(Object obj, Class<T> asType) {
        if ( obj!=null && asType.isAssignableFrom(obj.getClass()) ) {
            return (T)obj;
        }
        return null;
    }
    
    /**
     * Utility method for calling the given consumer if the given object is 
     * an instance of the given type.
     * @param <T>
     * @param obj
     * @param type
     * @param consumer
     */
    protected static final <T> void accept(Object obj, Class<T> type, Consumer<T> consumer) {
        T target = getAs(obj, type);
        if ( target!=null ) { consumer.accept(target); }
    }
    
    /**
     * Utility method for applying the given function on the given object and
     * returning the result, if the given object is an instance of the given 
     * type. If the given object is not of the given type, or if the provided 
     * function returns null, this method returns the value returned by the
     * given defaultValueSupplier if it is not null. Otherwise, this method 
     * returns null.   
     * @param <T>
     * @param <R>
     * @param obj
     * @param type
     * @param function
     * @param defaultValueSupplier
     * @return
     */
    protected static final <T,R> R applyWithDefaultSupplier(Object obj, Class<T> type, Function<T,R> function, Supplier<R> defaultValueSupplier) {
        T target = getAs(obj, type);
        R result = target==null ? null : function.apply(target);
        if ( result==null && defaultValueSupplier!=null ) {
            result = defaultValueSupplier.get();
        }
        return result;
    }
    
    /**
     * Utility method for applying the given function on the given object and
     * returning the result, if the given object is an instance of the given 
     * type. If the given object is not of the given type, or if the provided 
     * function returns null, this method returns the provided default value.
     * @param <T>
     * @param <R>
     * @param obj
     * @param type
     * @param function
     * @param defaultValue
     * @return
     */
    protected static final <T,R> R applyWithDefault(Object obj, Class<T> type, Function<T,R> function, R defaultValue) {
        return applyWithDefaultSupplier(obj, type, function, ()->defaultValue);
    }
    
    /**
     * Utility method for applying the given function on the given object and
     * returning the result, if the given object is an instance of the given 
     * type. If the given object is not of the given type, or if the provided 
     * function returns null, this method returns null.
     * @param <T>
     * @param <R>
     * @param obj
     * @param type
     * @param function
     * @return
     */
    protected static final <T,R> R apply(Object obj, Class<T> type, Function<T,R> function) {
        return applyWithDefaultSupplier(obj, type, function, null);
    }
    
    /**
     * This method adds record transformers from the given object if it implements {@link IRecordTransformerSupplier}
     * or {@link IRecordTransformer}. Usually an object would implement only one of those interfaces,
     * but if both interfaces are implemented, {@link IRecordTransformerSupplier}-based transformations
     * will run before {@link IRecordTransformer}-based transformations.
     * @param standardOutputConfig
     * @param obj
     */
    protected static final void addRecordTransformersFromObject(StandardOutputConfig standardOutputConfig, Object obj) {
        apply(obj, IRecordTransformerSupplier.class, s->standardOutputConfig.recordTransformer(s.getRecordTransformer()));
        apply(obj, IRecordTransformer.class, s->standardOutputConfig.recordTransformer(s::transformRecord));
    }
    
    /**
     * This method adds input transformers from the given object if it implements {@link IInputTransformerSupplier}
     * or {@link IInputTransformer}. Usually an object would implement only one of those interfaces,
     * but if both interfaces are implemented, {@link IInputTransformerSupplier}-based transformations
     * will run before {@link IInputTransformer}-based transformations.
     * @param standardOutputConfig
     * @param obj
     */
    protected static final void addInputTransformersFromObject(StandardOutputConfig standardOutputConfig, Object obj) {
        apply(obj, IInputTransformerSupplier.class, s->standardOutputConfig.inputTransformer(s.getInputTransformer()));
        apply(obj, IInputTransformer.class, s->standardOutputConfig.inputTransformer(s::transformInput));
    }
    
    protected static final void addCommandActionResultRecordTransformer(StandardOutputConfig standardOutputConfig, Object cmd) {
        apply(cmd, IActionCommandResultSupplier.class, s->standardOutputConfig.recordTransformer(createCommandActionResultRecordTransformer(s)));
    }
    
    private static final UnaryOperator<JsonNode> createCommandActionResultRecordTransformer(IActionCommandResultSupplier supplier) {
        return new AddFieldsTransformer(IActionCommandResultSupplier.actionFieldName, supplier.getActionCommandResult()).overwiteExisting(false)::transform;
    }
    
}
