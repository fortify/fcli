package com.fortify.cli.common.output.cli.mixin.spi.output;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.spi.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.cli.mixin.spi.output.transform.IInputTransformer;
import com.fortify.cli.common.output.cli.mixin.spi.output.transform.IInputTransformerSupplier;
import com.fortify.cli.common.output.cli.mixin.spi.output.transform.IRecordTransformer;
import com.fortify.cli.common.output.cli.mixin.spi.output.transform.IRecordTransformerSupplier;
import com.fortify.cli.common.output.cli.mixin.spi.product.IProductHelper;
import com.fortify.cli.common.output.cli.mixin.spi.product.ProductHelperClass;
import com.fortify.cli.common.output.cli.mixin.spi.request.IHttpRequestUpdater;
import com.fortify.cli.common.output.cli.mixin.spi.request.INextPageUrlProducerSupplier;
import com.fortify.cli.common.output.transform.fields.AddFieldsTransformer;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Model.CommandSpec;

@ReflectiveAccess
public abstract class AbstractOutputHelper implements IOutputHelper {
    @Getter private final IProductHelper productHelper;
    
    /**
     * This constructor creates an {@link IProductHelper} instance based on
     * the {@link ProductHelperClass} annotation on either the concrete
     * subclass of {@link AbstractOutputHelper}, or its enclosing class. 
     * The {@link IProductHelper} instance is then configured with this
     * {@link IOutputHelper} instance.
     */
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

    /**
     * Write output based on the given {@link UnirestInstance} and base {@link HttpRequest}.
     * This method updates the given base {@link HttpRequest} by calling the 
     * {@link #updateRequest(UnirestInstance, HttpRequest)} method, and retrieves a next page 
     * producer by calling the {@link #getNextPageUrlProducer(UnirestInstance, HttpRequest)} 
     * method. The (potentially) updated request and next page producer are then passed to
     * the {@link IOutputWriter} created by the {@link #createOutputWriter()} method, which
     * in turn will execute the request, handling paging if necessary, and write the response
     * data. 
     */
    @Override
    public final void write(UnirestInstance unirest, HttpRequest<?> baseRequest) {
        HttpRequest<?> request = updateRequest(unirest, baseRequest);
        INextPageUrlProducer nextPageUrlProducer = getNextPageUrlProducer(unirest, request);
        createOutputWriter().write(request, nextPageUrlProducer);
    }

    /**
     * Write the given {@link JsonNode} using the output writer created by the
     * {@link #createOutputWriter()} method. Obviously, this method will not 
     * provide any of the {@link HttpRequest}-based functionality as provided 
     * by the {@link #write(UnirestInstance, HttpRequest)} method, as there is
     * no {@link HttpRequest} to be updated or to apply paging on. Although not
     * currently used, this method accepts a {@link UnirestInstance} to be 
     * consistent with {@link #write(UnirestInstance, HttpRequest)}, and just in 
     * case we ever need it for future functionality. 
     */
    @Override
    public final void write(UnirestInstance unirest, JsonNode jsonNode) {
        createOutputWriter().write(jsonNode);
    }
    
    /**
     * This method simply gets a {@link JsonNode} instance from the given {@link JsonNodeHolder}, 
     * then calls the {@link #write(UnirestInstance, JsonNode)} method to write this
     * {@link JsonNode} instance to the output.
     */
    @Override
    public final void write(UnirestInstance unirest, JsonNodeHolder jsonNodeHolder) {
        write(unirest, jsonNodeHolder.asJsonNode());
    }
    
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
     * This default implementation of {@link IOutputHelper#getBasicOutputConfig()}
     * tries to retrieve a basic output configuration from the command being invoked,
     * if it implements the {@link IBasicOutputConfigSupplier} interface. If the
     * command doesn't implement this interface, or if the 
     * {@link IBasicOutputConfigSupplier#getBasicOutputConfig()} method returns null, 
     * this method throws an exception. Note that most concrete {@link IOutputHelper}
     * implementations will override this method; this default implementation is
     * mostly used for {@link OutputHelperMixins.Other}.
     */
    @Override
    public OutputConfig getBasicOutputConfig() {
        Object cmd = getCommand();
        return applyWithDefaultSupplier(cmd, 
                IBasicOutputConfigSupplier.class, IBasicOutputConfigSupplier::getBasicOutputConfig,
                ()->{throw new IllegalStateException(cmd.getClass().getName()+" must implement IBasicOutputConfigSupplier, or use an IOutputHelper implementation that provides a basic output configuration");});
    }
    
    /**
     * This default implementation of {@link IOutputHelper#getOutputWriterFactory()}
     * tries to retrieve an {@link IOutputWriterFactory} instance from the command 
     * being invoked, if it implements the {@link IOutputWriterFactorySupplier} interface. 
     * If the command doesn't implement this interface, or if the 
     * {@link IOutputWriterFactorySupplier#getOutputWriterFactory()} method returns null, 
     * this method throws an exception. Note that most concrete {@link IOutputHelper}
     * implementations will override this method; this default implementation is
     * mostly used for {@link OutputHelperMixins.Other}.
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
     * returned by the {@link IOutputWriterFactory#createOutputWriter(OutputConfig)} 
     * method.
     * @return {@link IOutputWriter} instance retrieved from an {@link IOutputWriterFactory} instance
     */
    protected final IOutputWriter createOutputWriter() {
        return getOutputWriterFactory().createOutputWriter(getOutputConfig());
    }
    
    /**
     * This method updates the given base {@link HttpRequest} by calling the
     * {@link IHttpRequestUpdater#updateRequest(UnirestInstance, HttpRequest)} method
     * on the configured {@link IProductHelper} and on the command currently being
     * invoked, in this order, if they implement the {@link IHttpRequestUpdater} interface. 
     * @param unirest
     * @param baseRequest
     * @return
     */
    protected final HttpRequest<?> updateRequest(UnirestInstance unirest, HttpRequest<?> request) {
        request = applyWithDefault(getProductHelper(), IHttpRequestUpdater.class, httpRequestUpdater(unirest, request), request);
        request = applyWithDefault(getCommand(), IHttpRequestUpdater.class, httpRequestUpdater(unirest, request), request);
        return request;
    }
    
    /**
     * This method returns a next page url producer retrieved from either the command
     * being invoked, or the configured {@link IProductHelper}, in this order, if
     * they implement the {@link INextPageUrlProducerSupplier} interface.
     * @param unirest
     * @param request
     * @return
     */
    protected final INextPageUrlProducer getNextPageUrlProducer(UnirestInstance unirest, HttpRequest<?> request) {
        return Stream.of(getCommand(), getProductHelper())
                .map(obj->getNextPageUrlProducerFromObject(obj, unirest, request))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }
    
    /**
     * Utility method for retrieving the command currently being invoked.
     * @return
     */
    protected final Object getCommand() {
        return getCommandSpec().userObject();
    }
    
    /** 
     * This method returns an {@link OutputConfig} instance that is
     * based on the basic output configuration returned by the
     * {@link #getBasicOutputConfig()} method, with input and record
     * transformers added by the {@link #addInputTransformersForCommand(OutputConfig, Object)}
     * and {@link #addRecordTransformersForCommand(OutputConfig, Object)} methods.
     * @return
     */
    protected final OutputConfig getOutputConfig() {
        Object cmd = getCommand();
        OutputConfig outputConfig = getBasicOutputConfig(cmd);
        addInputTransformersForCommand(outputConfig, cmd);
        addRecordTransformersForCommand(outputConfig, cmd);
        addCommandActionResultRecordTransformer(outputConfig, cmd);
        return outputConfig;
    }

    /**
     * This method adds record transformers to the given {@link OutputConfig} by
     * calling the following methods, in this order:
     * <ul>
     * <li>{@link #addRecordTransformersFromObject(OutputConfig, Object)} with the configured {@link IProductHelper}</li>
     * <li>{@link #addRecordTransformersFromObject(OutputConfig, Object)} with the command being invoked</li>
     * <li>{@link #addCommandActionResultRecordTransformer(OutputConfig, Object)} with the command being invoked</li>
     * <ul>
     * If a command needs to run any record transformations before the record transformations provided by
     * {@link IProductHelper}, the command should implement the {@link IBasicOutputConfigSupplier} interface
     * to add those record transformations to the basic output configuration.
     * @param outputConfig
     * @param cmd
     */
    protected final void addRecordTransformersForCommand(OutputConfig outputConfig, Object cmd) {
        addRecordTransformersFromObject(outputConfig, getProductHelper());
        addRecordTransformersFromObject(outputConfig, cmd);
        addCommandActionResultRecordTransformer(outputConfig, cmd);
    }

    /**
     * This method adds input transformers to the given {@link OutputConfig} by
     * calling the following methods, in this order:
     * <ul>
     * <li>{@link #addInputTransformersFromObject(OutputConfig, Object)} with the configured {@link IProductHelper}</li>
     * <li>{@link #addInputTransformersFromObject(OutputConfig, Object)} with the command being invoked</li>
     * <ul>
     * If a command needs to run any input transformations before the input transformations provided by
     * {@link IProductHelper}, the command should implement the {@link IBasicOutputConfigSupplier} interface
     * to add those input transformations to the basic output configuration.
     * @param outputConfig
     * @param cmd
     */
    protected final void addInputTransformersForCommand(OutputConfig outputConfig, Object cmd) {
        addInputTransformersFromObject(outputConfig, getProductHelper());
        addInputTransformersFromObject(outputConfig, cmd);
    }

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
    private final OutputConfig getBasicOutputConfig(Object cmd) {
        return applyWithDefaultSupplier(cmd, IBasicOutputConfigSupplier.class, IBasicOutputConfigSupplier::getBasicOutputConfig, this::getBasicOutputConfig);
    }
    
    protected abstract CommandSpec getMixee();
    
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
     * Utility method used by {@link #updateRequest(UnirestInstance, HttpRequest)}, returning a function
     * that takes an {@link IHttpRequestUpdater} instance and returning the result of 
     * {@link IHttpRequestUpdater#updateRequest(UnirestInstance, HttpRequest)}.
     * @param unirest
     * @param request
     * @return
     */
    private static final Function<IHttpRequestUpdater, HttpRequest<?>> httpRequestUpdater(UnirestInstance unirest, final HttpRequest<?> request) {
        return requestUpdater -> requestUpdater.updateRequest(unirest, request);
    }
    
    /**
     * Utility method used by {@link #getNextPageUrlProducer(UnirestInstance, HttpRequest)}, returning a
     * next page producer retrieved from the given object if that object implements {@link INextPageUrlProducerSupplier},
     * or null otherwise.
     * @param obj
     * @param unirest
     * @param request
     * @return
     */
    private static final INextPageUrlProducer getNextPageUrlProducerFromObject(Object obj, UnirestInstance unirest, final HttpRequest<?> request) {
        return apply(obj, INextPageUrlProducerSupplier.class, supplier->supplier.getNextPageUrlProducer(unirest, request));
    }
    
    /**
     * This method adds record transformers from the given object if it implements {@link IRecordTransformerSupplier}
     * or {@link IRecordTransformer}. Usually an object would implement only one of those interfaces,
     * but if both interfaces are implemented, {@link IRecordTransformerSupplier}-based transformations
     * will run before {@link IRecordTransformer}-based transformations.
     * @param outputConfig
     * @param obj
     */
    private static final void addRecordTransformersFromObject(OutputConfig outputConfig, Object obj) {
        apply(obj, IRecordTransformerSupplier.class, s->outputConfig.recordTransformer(s.getRecordTransformer()));
        apply(obj, IRecordTransformer.class, s->outputConfig.recordTransformer(s::transformRecord));
    }
    
    /**
     * This method adds input transformers from the given object if it implements {@link IInputTransformerSupplier}
     * or {@link IInputTransformer}. Usually an object would implement only one of those interfaces,
     * but if both interfaces are implemented, {@link IInputTransformerSupplier}-based transformations
     * will run before {@link IInputTransformer}-based transformations.
     * @param outputConfig
     * @param obj
     */
    private static final void addInputTransformersFromObject(OutputConfig outputConfig, Object obj) {
        apply(obj, IInputTransformerSupplier.class, s->outputConfig.inputTransformer(s.getInputTransformer()));
        apply(obj, IInputTransformer.class, s->outputConfig.inputTransformer(s::transformInput));
    }
    
    private static final void addCommandActionResultRecordTransformer(OutputConfig outputConfig, Object cmd) {
        apply(cmd, IActionCommandResultSupplier.class, s->outputConfig.recordTransformer(createCommandActionResultRecordTransformer(s)));
    }
    
    private static final UnaryOperator<JsonNode> createCommandActionResultRecordTransformer(IActionCommandResultSupplier supplier) {
        return new AddFieldsTransformer("__action__", supplier.getActionCommandResult())::transform;
    }
    
}
