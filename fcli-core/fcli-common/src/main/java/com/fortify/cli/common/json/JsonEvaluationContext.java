/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.json;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.support.DataBindingMethodResolver;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.integration.json.JsonNodeWrapperToJsonNodeConverter;
import org.springframework.integration.json.JsonPropertyAccessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.spring.expression.SpelHelper;
import com.fortify.cli.common.spring.expression.StandardSpelFunctions;

/**
 * This {@link EvaluationContext} implementation uses a {@link SimpleEvaluationContext}
 * under the hoods, configured for data binding and condition evaluation that can resolve 
 * properties on {@link JsonNode} instances. We allow reflective method calls using 
 * {@link DataBindingMethodResolver}. Note that native binaries will only be able to access 
 * methods declared in reflect-config.json. We use the delegate pattern to allow for 
 * customizing the {@link #getOperatorOverloader()} method, as {@link SimpleEvaluationContext}
 * doesn't allow for customizing the default overloader.
 * 
 * @return
 */
public final class JsonEvaluationContext implements EvaluationContext {
    private final EvaluationContext delegate = createDelegate();
    
    private final EvaluationContext createDelegate() {
        DefaultFormattingConversionService  conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new JsonNodeWrapperToJsonNodeConverter());
        conversionService.addConverter(new ListToArrayNodeConverter());
        conversionService.addConverter(new ObjectToJsonNodeConverter());
        DateTimeFormatterRegistrar dateTimeRegistrar = new DateTimeFormatterRegistrar();
        dateTimeRegistrar.setDateFormatter(DateTimeFormatter.ISO_DATE);
        dateTimeRegistrar.setDateTimeFormatter(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        dateTimeRegistrar.registerFormatters(conversionService);
        SimpleEvaluationContext context = SimpleEvaluationContext
                .forPropertyAccessors(new JsonPropertyAccessor())
                .withConversionService(conversionService)
                .withInstanceMethods()
                .build();
        SpelHelper.registerFunctions(context, StandardSpelFunctions.class);
        return context;
    }
    
    /**
     * @return
     * @see org.springframework.expression.EvaluationContext#getRootObject()
     */
    public TypedValue getRootObject() {
        return delegate.getRootObject();
    }

    /**
     * @return
     * @see org.springframework.expression.EvaluationContext#getPropertyAccessors()
     */
    public List<PropertyAccessor> getPropertyAccessors() {
        return delegate.getPropertyAccessors();
    }

    /**
     * @return
     * @see org.springframework.expression.EvaluationContext#getConstructorResolvers()
     */
    public List<ConstructorResolver> getConstructorResolvers() {
        return delegate.getConstructorResolvers();
    }

    /**
     * @return
     * @see org.springframework.expression.EvaluationContext#getMethodResolvers()
     */
    public List<MethodResolver> getMethodResolvers() {
        return delegate.getMethodResolvers();
    }

    /**
     * @return
     * @see org.springframework.expression.EvaluationContext#getBeanResolver()
     */
    public BeanResolver getBeanResolver() {
        return delegate.getBeanResolver();
    }

    /**
     * @return
     * @see org.springframework.expression.EvaluationContext#getTypeLocator()
     */
    public TypeLocator getTypeLocator() {
        return delegate.getTypeLocator();
    }

    /**
     * @return
     * @see org.springframework.expression.EvaluationContext#getTypeConverter()
     */
    public TypeConverter getTypeConverter() {
        return delegate.getTypeConverter();
    }

    /**
     * @return
     * @see org.springframework.expression.EvaluationContext#getTypeComparator()
     */
    public TypeComparator getTypeComparator() {
        return delegate.getTypeComparator();
    }

    /**
     * @return
     * @see org.springframework.expression.EvaluationContext#getOperatorOverloader()
     */
    public OperatorOverloader getOperatorOverloader() {
        // TODO Return out custom overloader
        return delegate.getOperatorOverloader();
    }

    /**
     * @param name
     * @param valueSupplier
     * @return
     * @see org.springframework.expression.EvaluationContext#assignVariable(java.lang.String, java.util.function.Supplier)
     */
    public TypedValue assignVariable(String name, Supplier<TypedValue> valueSupplier) {
        return delegate.assignVariable(name, valueSupplier);
    }

    /**
     * @param name
     * @param value
     * @see org.springframework.expression.EvaluationContext#setVariable(java.lang.String, java.lang.Object)
     */
    public void setVariable(String name, Object value) {
        delegate.setVariable(name, value);
    }

    /**
     * @param name
     * @return
     * @see org.springframework.expression.EvaluationContext#lookupVariable(java.lang.String)
     */
    public Object lookupVariable(String name) {
        return delegate.lookupVariable(name);
    }

    private static final class ObjectToJsonNodeConverter implements Converter<Object, JsonNode> {
        @Override
        public JsonNode convert(Object source) {
            return JsonHelper.getObjectMapper().valueToTree(source);
        }
    }
    
    private static final class ListToArrayNodeConverter implements Converter<List<?>, ArrayNode> {
        @Override
        public ArrayNode convert(List<?> source) {
            return JsonHelper.getObjectMapper().valueToTree(source);
        }
    }
}
