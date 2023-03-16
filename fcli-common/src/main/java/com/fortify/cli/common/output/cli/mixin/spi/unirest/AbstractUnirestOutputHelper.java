package com.fortify.cli.common.output.cli.mixin.spi.unirest;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.output.cli.mixin.spi.AbstractOutputHelper;
import com.fortify.cli.common.output.spi.IBasicOutputConfigSupplier;
import com.fortify.cli.common.output.spi.product.IProductHelper;
import com.fortify.cli.common.output.spi.product.ProductHelperClass;
import com.fortify.cli.common.output.spi.request.IHttpRequestUpdater;
import com.fortify.cli.common.output.spi.request.INextPageUrlProducerSupplier;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public abstract class AbstractUnirestOutputHelper extends AbstractOutputHelper implements IUnirestOutputHelper {
    @Getter private final IProductHelper productHelper;
    
    /**
     * This constructor creates an {@link IProductHelper} instance based on
     * the {@link ProductHelperClass} annotation on either the concrete
     * subclass of {@link AbstractUnirestOutputHelper}, or its enclosing class. 
     * The {@link IProductHelper} instance is then configured with this
     * {@link IUnirestOutputHelper} instance.
     */
    public AbstractUnirestOutputHelper() {
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
     * This method adds record transformers to the given {@link StandardOutputConfig} by
     * calling the following methods, in this order:
     * <ul>
     * <li>{@link #addRecordTransformersFromObject(StandardOutputConfig, Object)} with the configured {@link IProductHelper}</li>
     * <li>{@link #addRecordTransformersFromObject(StandardOutputConfig, Object)} with the command being invoked</li>
     * <li>{@link #addCommandActionResultRecordTransformer(StandardOutputConfig, Object)} with the command being invoked</li>
     * <ul>
     * If a command needs to run any record transformations before the record transformations provided by
     * {@link IProductHelper}, the command should implement the {@link IBasicOutputConfigSupplier} interface
     * to add those record transformations to the basic output configuration.
     * @param standardOutputConfig
     * @param cmd
     */
    protected final void addRecordTransformersForCommand(StandardOutputConfig standardOutputConfig, Object cmd) {
        addRecordTransformersFromObject(standardOutputConfig, getProductHelper());
        addRecordTransformersFromObject(standardOutputConfig, cmd);
        addCommandActionResultRecordTransformer(standardOutputConfig, cmd);
    }

    /**
     * This method adds input transformers to the given {@link StandardOutputConfig} by
     * calling the following methods, in this order:
     * <ul>
     * <li>{@link #addInputTransformersFromObject(StandardOutputConfig, Object)} with the configured {@link IProductHelper}</li>
     * <li>{@link #addInputTransformersFromObject(StandardOutputConfig, Object)} with the command being invoked</li>
     * <ul>
     * If a command needs to run any input transformations before the input transformations provided by
     * {@link IProductHelper}, the command should implement the {@link IBasicOutputConfigSupplier} interface
     * to add those input transformations to the basic output configuration.
     * @param standardOutputConfig
     * @param cmd
     */
    protected final void addInputTransformersForCommand(StandardOutputConfig standardOutputConfig, Object cmd) {
        addInputTransformersFromObject(standardOutputConfig, getProductHelper());
        addInputTransformersFromObject(standardOutputConfig, cmd);
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
}
