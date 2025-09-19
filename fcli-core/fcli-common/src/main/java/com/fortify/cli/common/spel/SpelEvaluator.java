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
package com.fortify.cli.common.spel;

import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.integration.json.JsonNodeWrapperToJsonNodeConverter;
import org.springframework.integration.json.JsonPropertyAccessor;
import org.springframework.integration.json.JsonPropertyAccessor.JsonNodeWrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.fn.SpelFunctionsStandard;

import lombok.RequiredArgsConstructor;

public enum SpelEvaluator implements ISpelEvaluator {
    JSON_GENERIC(SpelEvaluator::createJsonGenericContext),
    JSON_QUERY(SpelEvaluator::createJsonQueryContext);
    
    private static final Logger LOG = LoggerFactory.getLogger(SpelEvaluator.class);
    private static final SpelExpressionParser SPEL_PARSER = new SpelExpressionParser();
    private EvaluationContext context;
    private final Supplier<SimpleEvaluationContext> contextSupplier;
    
    private SpelEvaluator(Supplier<SimpleEvaluationContext> contextSupplier) {
        this.context = contextSupplier.get();
        this.contextSupplier = contextSupplier;
    }

    public final <R> R evaluate(Expression expression, Object input, Class<R> returnClass) {
        try {
            return evaluate(context, expression, input, returnClass);
        } catch (RuntimeException e) {
            throw handleException(e);
        }
    }

    public final <R> R evaluate(String expression, Object input, Class<R> returnClass) {
        try {
            return evaluate(SPEL_PARSER.parseExpression(expression), input, returnClass);
        } catch (RuntimeException e) {
            throw handleException(e);
        }
    } 

    public final IConfigurableSpelEvaluator copy() {
        return new ConfigurableSpelEvaluator(contextSupplier.get());
    }
    
    @RequiredArgsConstructor
    private static final class ConfigurableSpelEvaluator implements IConfigurableSpelEvaluator {
        private final SimpleEvaluationContext context;
        
        public final <R> R evaluate(Expression expression, Object input, Class<R> returnClass) {
            return SpelEvaluator.evaluate(context, expression, input, returnClass);
        }

        public final <R> R evaluate(String expression, Object input, Class<R> returnClass) {
            return evaluate(SPEL_PARSER.parseExpression(expression), input, returnClass);
        }
        
        @Override
        public IConfigurableSpelEvaluator configure(Consumer<SimpleEvaluationContext> contextConfigurer) {
            contextConfigurer.accept(context);
            return this;
        }
    }
    
    private static final <R> R evaluate(EvaluationContext context, Expression expression, Object input, Class<R> returnClass) {
        try {
            return unwrapSpelExpressionResult(expression.getValue(context, input, returnClass), returnClass);
        } catch (RuntimeException e) {
            throw handleException(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static final <R> R unwrapSpelExpressionResult(R result, Class<R> returnClass) {
        if ( result instanceof JsonNodeWrapper<?> && returnClass.isAssignableFrom(JsonNode.class) ) {
            result = (R)((JsonNodeWrapper<?>)result).getRealNode();
        }
        return result;
    }
    
    private static final RuntimeException handleException(RuntimeException e) {
        return handleException(e, e.getCause());
    }

    private static final RuntimeException handleException(RuntimeException rootException, Throwable currentCause) {
        // TODO Do we need full exception stack trace, or can we throw FcliSimpleExpression
        //      that shows the failing expression and original message?
        if ( currentCause==null ) { return rootException; }
        // TODO Only handle FcliSimpleException, or all FcliExceptions?
        // TODO Should we still wrap this in another FcliSimpleException that shows the failing expression?
        if ( currentCause instanceof FcliSimpleException ) {
            LOG.debug("Throwing FcliSimpleException only; original exception: ", rootException);
            return (FcliSimpleException)currentCause;
        } else if ( currentCause instanceof InvocationTargetException ) {
            // TODO Should we also check InvocationTargetException::getCause? 
            return handleException(rootException, ((InvocationTargetException)currentCause).getTargetException());
        } else {
            return handleException(rootException, currentCause.getCause());
        }
    }
    
    private static final SimpleEvaluationContext createJsonGenericContext() {
        SimpleEvaluationContext context = SimpleEvaluationContext
            .forPropertyAccessors(new JsonPropertyAccessor())
            .withConversionService(createJsonConversionService())
            .withInstanceMethods()
            .build();
        SpelHelper.registerFunctions(context, StringUtils.class);
        SpelHelper.registerFunctions(context, SpelFunctionsStandard.class);
        return context;
    }
    
    private static final SimpleEvaluationContext createJsonQueryContext() {
        SimpleEvaluationContext context = SimpleEvaluationContext
            .forPropertyAccessors(new ExistingJsonPropertyAccessor())
            .withConversionService(createJsonConversionService())
            .withInstanceMethods()
            .build();
        SpelHelper.registerFunctions(context, StringUtils.class);
        SpelHelper.registerFunctions(context, SpelFunctionsStandard.class);
        return context;
    }
    
    private static final DefaultFormattingConversionService createJsonConversionService() {
        DefaultFormattingConversionService  conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new JsonNodeWrapperToJsonNodeConverter());
        conversionService.addConverter(new ListToArrayNodeConverter());
        conversionService.addConverter(new ObjectToJsonNodeConverter());
        DateTimeFormatterRegistrar dateTimeRegistrar = new DateTimeFormatterRegistrar();
        dateTimeRegistrar.setDateFormatter(DateTimeFormatter.ISO_DATE);
        dateTimeRegistrar.setDateTimeFormatter(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        dateTimeRegistrar.registerFormatters(conversionService);
        return conversionService;
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
    
    private static final class ExistingJsonPropertyAccessor extends JsonPropertyAccessor {
        /**
        * By default the JsonPropertyAccessor.canRead method always returns true if target is a valid JsonObject
        * This override exists to ensure we return false in case the target object does not have a property with the provided name
        */
        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
            return super.canRead(context, target, name) && (!(target instanceof ObjectNode) || ((ObjectNode)target).has(name));
        }
    }
}
