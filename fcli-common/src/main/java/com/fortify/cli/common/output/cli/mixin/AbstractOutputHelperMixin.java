/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.output.cli.mixin;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.common.output.product.NoOpProductHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.output.transform.fields.AddFieldsTransformer;
import com.fortify.cli.common.output.writer.IBasicOutputConfigSupplier;
import com.fortify.cli.common.output.writer.IOutputWriterFactorySupplier;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;
import com.fortify.cli.common.util.JavaHelper;

import kong.unirest.HttpRequest;
import picocli.CommandLine.Mixin;

public abstract class AbstractOutputHelperMixin implements IOutputHelper {
    @Mixin private CommandHelperMixin commandHelper;
    
    public IProductHelper getProductHelper() {
        return commandHelper.getCommandAs(IProductHelperSupplier.class)
            .map(IProductHelperSupplier::getProductHelper)
            .orElse(NoOpProductHelper.instance());
    }

    /**
     * Write output based on the given {@link HttpRequest}.
     * This method updates the given base {@link HttpRequest} by calling the 
     * {@link #updateRequest(HttpRequest)} method, and retrieves a next page 
     * producer by calling the {@link #getNextPageUrlProducer(HttpRequest)} 
     * method. The (potentially) updated request and next page producer are then passed to
     * the {@link IOutputWriter} created by the {@link #createOutputWriter()} method, which
     * in turn will execute the request, handling paging if necessary, and write the response
     * data. 
     */
    @Override
    public final void write(HttpRequest<?> baseRequest) {
        HttpRequest<?> request = updateRequest(baseRequest);
        INextPageUrlProducer nextPageUrlProducer = getNextPageUrlProducer(request);
        createOutputWriter().write(request, nextPageUrlProducer);
    }

    /**
     * Write the given {@link JsonNode} using the output writer created by the
     * {@link #createOutputWriter()} method. Obviously, this method will not 
     * provide any of the {@link HttpRequest}-based functionality as provided 
     * by the {@link #write(HttpRequest)} method, as there is no 
     * {@link HttpRequest} to be updated or to apply paging on.
     */
    @Override
    public final void write(JsonNode jsonNode) {
        createOutputWriter().write(jsonNode);
    }
    
    /**
     * This method simply gets a {@link JsonNode} instance from the given 
     * {@link JsonNodeHolder}, then calls the {@link #write(JsonNode)} method 
     * to write this {@link JsonNode} instance to the output.
     */
    @Override
    public final void write(JsonNodeHolder jsonNodeHolder) {
        write(jsonNodeHolder.asJsonNode());
    }
    
    /**
     * This method updates the given base {@link HttpRequest} by calling the
     * {@link IHttpRequestUpdater#updateRequest(HttpRequest)} method
     * on the configured {@link IProductHelper}, any mixins on the command
     * currently being invoked, and the command itself, in this order, if 
     * they implement the {@link IHttpRequestUpdater} interface. 
     * @param baseRequest
     * @return
     */
    protected final HttpRequest<?> updateRequest(HttpRequest<?> request) {
        request = applyWithDefault(getProductHelper(), IHttpRequestUpdater.class, httpRequestUpdater(request), request);
        for ( var mixin : commandHelper.getCommandSpec().mixins().values() ) {
            request = applyWithDefault(mixin.userObject(), IHttpRequestUpdater.class, httpRequestUpdater(request), request);
        }
        request = applyWithDefault(commandHelper.getCommand(), IHttpRequestUpdater.class, httpRequestUpdater(request), request);
        return request;
    }
    
    /**
     * This method returns a next page url producer retrieved from either the command
     * being invoked, or the configured {@link IProductHelper}, in this order, if
     * they implement the {@link INextPageUrlProducerSupplier} interface.
     * @param request
     * @return
     */
    protected final INextPageUrlProducer getNextPageUrlProducer(HttpRequest<?> request) {
        return Stream.of(commandHelper.getCommand(), getProductHelper())
                .map(obj->getNextPageUrlProducerFromObject(obj, request))
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
     * <li>{@link #addInputTransformersFromObject(StandardOutputConfig, Object)} with every mixin 
     *     contained in the command being invoked, which includes any {@link IProductHelper}
     *     mixin that implements {@link IInputTransformer}</li>
     * <li>{@link #addInputTransformersFromObject(StandardOutputConfig, Object)} with the command 
     *     being invoked</li>
     * <ul>
     * If a command needs to run any input transformations before the input transformations provided by
     * {@link IProductHelper}, the command should implement the {@link IBasicOutputConfigSupplier} interface
     * to add those input transformations to the basic output configuration.
     * @param standardOutputConfig
     * @param cmd
     */
    protected final void addInputTransformersForCommand(StandardOutputConfig standardOutputConfig, Object cmd) {
        for ( var mixin : commandHelper.getCommandSpec().mixins().values() ) {
            addInputTransformersFromObject(standardOutputConfig, mixin.userObject());
        }
        addInputTransformersFromObject(standardOutputConfig, cmd);
    }
    
    /**
     * Utility method used by {@link #updateRequest(HttpRequest)}, returning a function
     * that takes an {@link IHttpRequestUpdater} instance and returning the result of 
     * {@link IHttpRequestUpdater#updateRequest(HttpRequest)}.
     * @param request
     * @return
     */
    private static final Function<IHttpRequestUpdater, HttpRequest<?>> httpRequestUpdater(final HttpRequest<?> request) {
        return requestUpdater -> requestUpdater.updateRequest(request);
    }
    
    /**
     * Utility method used by {@link #getNextPageUrlProducer(HttpRequest)}, returning a
     * next page producer retrieved from the given object if that object implements 
     * {@link INextPageUrlProducerSupplier}, or null otherwise.
     * @param obj
     * @param request
     * @return
     */
    private static final INextPageUrlProducer getNextPageUrlProducerFromObject(Object obj, final HttpRequest<?> request) {
        return apply(obj, INextPageUrlProducerSupplier.class, supplier->supplier.getNextPageUrlProducer(request));
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
    public StandardOutputConfig getBasicOutputConfig() {
        Object cmd = commandHelper.getCommand();
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
        Object cmd = commandHelper.getCommand();
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
    private final IOutputWriter createOutputWriter() {
        return getOutputWriterFactory().createOutputWriter(getOutputConfig());
    }
    
    /** 
     * This method returns an {@link StandardOutputConfig} instance that is
     * based on the basic output configuration returned by the
     * {@link #getBasicOutputConfig()} method, with input and record
     * transformers added by the {@link #addInputTransformersForCommand(StandardOutputConfig, Object)}
     * and {@link #addRecordTransformersForCommand(StandardOutputConfig, Object)} methods.
     * @return
     */
    private final StandardOutputConfig getOutputConfig() {
        Object cmd = commandHelper.getCommand();
        StandardOutputConfig standardOutputConfig = getBasicOutputConfig(cmd);
        addInputTransformersForCommand(standardOutputConfig, cmd);
        addRecordTransformersForCommand(standardOutputConfig, cmd);
        addCommandActionResultRecordTransformer(standardOutputConfig, cmd);
        return standardOutputConfig;
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
    private final StandardOutputConfig getBasicOutputConfig(Object cmd) {
        return applyWithDefaultSupplier(cmd, IBasicOutputConfigSupplier.class, IBasicOutputConfigSupplier::getBasicOutputConfig, this::getBasicOutputConfig);
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
    private static final <T,R> R applyWithDefaultSupplier(Object obj, Class<T> type, Function<T,R> function, Supplier<R> defaultValueSupplier) {
        var result = JavaHelper.as(obj, type).map(function);
        if ( defaultValueSupplier!=null ) {
            result = result.or(()->Optional.of(defaultValueSupplier.get()));
        }
        return result.orElse(null);
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
    private static final <T,R> R applyWithDefault(Object obj, Class<T> type, Function<T,R> function, R defaultValue) {
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
    private static final <T,R> R apply(Object obj, Class<T> type, Function<T,R> function) {
        return applyWithDefaultSupplier(obj, type, function, null);
    }
    
    /**
     * This method adds record transformers from the given object if it 
     * implements {@link IRecordTransformer}. 
     * @param standardOutputConfig
     * @param obj
     */
    private static final void addRecordTransformersFromObject(StandardOutputConfig standardOutputConfig, Object obj) {
        apply(obj, IRecordTransformer.class, s->standardOutputConfig.recordTransformer(s::transformRecord));
    }
    
    /**
     * This method adds input transformers from the given object if it 
     * implements {@link IInputTransformer}.
     * @param standardOutputConfig
     * @param obj
     */
    private static final void addInputTransformersFromObject(StandardOutputConfig standardOutputConfig, Object obj) {
        apply(obj, IInputTransformer.class, s->standardOutputConfig.inputTransformer(s::transformInput));
    }
    
    private static final void addCommandActionResultRecordTransformer(StandardOutputConfig standardOutputConfig, Object cmd) {
        apply(cmd, IActionCommandResultSupplier.class, s->standardOutputConfig.recordTransformer(createCommandActionResultRecordTransformer(s)));
    }
    
    private static final UnaryOperator<JsonNode> createCommandActionResultRecordTransformer(IActionCommandResultSupplier supplier) {
        return new AddFieldsTransformer(IActionCommandResultSupplier.actionFieldName, supplier.getActionCommandResult()).overwiteExisting(false)::transform;
    }
    
}
