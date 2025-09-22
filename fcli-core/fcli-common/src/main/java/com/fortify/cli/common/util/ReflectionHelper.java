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
package com.fortify.cli.common.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import lombok.SneakyThrows;

/**
 *
 * @author Ruud Senden
 */
public final class ReflectionHelper {
    /**
     * This method returns the field type as the first element in the returned array, and any generic 
     * type arguments (if applicable) as subsequent array elements, for example [Map, String, Boolean]
     * or [List, Integer].
     */
    @SneakyThrows
    public static final Class<?>[] getAllTypes(Field field) {
        var result = new ArrayList<Class<?>>();
        result.add(field.getType());
        var genericType = field.getGenericType();
        if ( genericType!=null && genericType instanceof ParameterizedType ) {
            for ( var type : ((ParameterizedType)genericType).getActualTypeArguments() ) {
                result.add(Class.forName(type.getTypeName()));
            }
        }
        return result.toArray(Class<?>[]::new);
    }
    
    /**
     * If the given field is declared as a generic type, this method will return the generic type arguments,
     * otherwise an empty array will be returned.
     */
    @SneakyThrows
    public static final Class<?>[] getGenericTypes(Field field) {
        var allTypes = getAllTypes(field);
        return Arrays.copyOfRange(allTypes, 1, allTypes.length);
    }
    
    public static final <A extends Annotation,R> R getAnnotationValue(Object o, Class<A> annotationType, Function<A,R> valueRetriever, Supplier<R> defaultValueSupplier) {
        R annotationValue = JavaHelper.as(o,AnnotatedElement.class)
                .map(ae->ae.getAnnotation(annotationType))
                .map(a->valueRetriever.apply(a))
                .orElse(null);
        return annotationValue==null || (annotationValue instanceof String && StringUtils.isBlank((String)annotationValue) || AnnotationDefaultClassValue.class.equals(annotationValue)) 
                ? defaultValueSupplier==null ? null : defaultValueSupplier.get() 
                : annotationValue;
    }
    
    public static final boolean hasAnnotation(Object o, Class<? extends Annotation> annotation) {
        return JavaHelper.as(o, AnnotatedElement.class)
                .map(e->e.isAnnotationPresent(annotation))
                .orElse(false);
    }
    
    public static interface AnnotationDefaultClassValue {}
}
