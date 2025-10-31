/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.common.action.schema;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fortify.cli.common.action.model.IActionElement;
import com.fortify.cli.common.json.JsonPropertyDescriptionAppend;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;
import com.fortify.cli.common.util.ReflectionHelper;

/**
 *
 * @author Ruud Senden
 */
public class ActionSchemaHelper {
    public static final boolean isActionModelClazz(Class<?> type) {
        return type!=null && IActionElement.class.isAssignableFrom(type);
    }
    
    public static final String getFullJsonPropertyDescription(Field field) {
        var description = ReflectionHelper.getAnnotationValue(field, JsonPropertyDescription.class, JsonPropertyDescription::value, ()->null);
        return getFullJsonPropertyDescription(description, field.getAnnotation(JsonPropertyDescriptionAppend.class));
    }
    public static final String getFullJsonPropertyDescription(String description, JsonPropertyDescriptionAppend appendAnnotation) {
        if ( description!=null && appendAnnotation!=null ) {
            var clazz = appendAnnotation.value();
            var values = new ArrayList<String>(); 
            for (var e : clazz.getEnumConstants()) {
                values.add(e.toString());
            }
            description += String.join(", ", values);
        }
        return description;
    }

    public static String getJsonType(Class<?> javaType, Class<?>[] javaGenericTypes) {
        if ( Map.class.isAssignableFrom(javaType) ) {
            return String.format("map<%s,%s>", getJsonType(javaGenericTypes[0]), getJsonType(javaGenericTypes[1]));
        } else if (Collection.class.isAssignableFrom(javaType) ) {
            return String.format("array<%s>", getJsonType(javaGenericTypes[0]));
        } else {
            return getJsonType(javaType);
        }
    }
    
    public static final String getJsonType(Class<?> clazz) {
        return getJsonType(clazz, c->"object");
    }
    
    public static final String getJsonType(Class<?> clazz, Function<Class<?>, String> orElse) {
        if ( String.class.isAssignableFrom(clazz) ) { return "string"; }
        if ( Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz) ) { return "boolean"; }
        if ( Number.class.isAssignableFrom(clazz) ) { return "number"; } // TODO Add support for primitives
        if ( Enum.class.isAssignableFrom(clazz)) { return "enum"; }
        if ( TemplateExpression.class.isAssignableFrom(clazz) ) { return "expression"; }
        if ( Map.class.isAssignableFrom(clazz)) { return "map"; }
        if ( Collection.class.isAssignableFrom(clazz) || clazz.isArray() ) { return "array"; }
        return orElse.apply(clazz);
    }
    
    public static final String[] getJsonTypes(Class<?>[] clazzes) {
        return getJsonTypes(clazzes, c->"object");
    }

    public static final String[] getJsonTypes(Class<?>[] clazzes, Function<Class<?>, String> orElse) {
        if ( clazzes==null ) { return new String[] {}; }
        return Stream.of(clazzes).map(c->getJsonType(c, orElse)).toArray(String[]::new);
    }
    
    public static final String[] getSampleYamlSnippets(AnnotatedElement annotatedElement) {
        var targets = addSampleYamlSnippetsCopyFrom(new ArrayList<AnnotatedElement>(), new AnnotatedElement[]{annotatedElement});
        return targets.stream()
            .map(target->ReflectionHelper.getAnnotationValue(target, SampleYamlSnippets.class, SampleYamlSnippets::value, ()->new String[] {}))
            .flatMap(Stream::of)
            .toArray(String[]::new);
    }

    /** Recursively collect all 'copyFrom' classes referenced from the given annotated elements */
    private static final List<AnnotatedElement> addSampleYamlSnippetsCopyFrom(ArrayList<AnnotatedElement> result, AnnotatedElement[] annotatedElements) {
        if ( annotatedElements!=null && annotatedElements.length>0 ) {
            var list = List.of(annotatedElements);
            result.addAll(list);
            list.forEach(e->addSampleYamlSnippetsCopyFrom(result, ReflectionHelper.getAnnotationValue(e, SampleYamlSnippets.class, SampleYamlSnippets::copyFrom, ()->new Class<?>[] {})));
        }
        return result;
    }

}
