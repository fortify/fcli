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
package com.fortify.cli.common.spel.fn.descriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.ArrayListWithAsJsonMethod;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;
import com.fortify.cli.common.util.ReflectionHelper;

import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;

@Data
public final class SpelFunctionDescriptorsFactory {
    public static final ArrayListWithAsJsonMethod<SpelFunctionDescriptor> getStandardSpelFunctionsDescriptors() {
        return getSpelFunctionsDescriptors("com.fortify.cli.common.spring.expression.fn.SpelFunctionsStandard");
    }
    
    public static final ArrayListWithAsJsonMethod<SpelFunctionDescriptor> getActionSpelFunctionsDescriptors() {
        return getSpelFunctionsDescriptors(
                "com.fortify.cli.common.spel.fn.SpelFunctionsStandard",
                "com.fortify.cli.common.action.runner.ActionSpelFunctions",
                "com.fortify.cli.common.action.runner.ActionRunnerContextSpelFunctions",
                "com.fortify.cli.fod.action.helper.FoDActionSpelFunctions",
                "com.fortify.cli.ssc.action.helper.SSCActionSpelFunctions"
        );
    }
    
    public static final ArrayListWithAsJsonMethod<SpelFunctionDescriptor> getSpelFunctionsDescriptors(String... spelFunctionClazzNames) {
    Collection<Class<?>> spelFunctionClazzes = Stream.of(spelFunctionClazzNames)
        .map(c->toClass(c))
        .collect(Collectors.toList());
        return collectSpelFunctions(spelFunctionClazzes);
    }

    @SneakyThrows
    private static final Class<?> toClass(String c) {
        return Class.forName(c);
    }

    private static final ArrayListWithAsJsonMethod<SpelFunctionDescriptor> collectSpelFunctions(Collection<Class<?>> spelFunctionClazzes) {
        return spelFunctionClazzes.stream()
            .flatMap(c->createFunctionDescriptorsStream(c))
            .sorted((a, b) -> a.getCategoryAndName().compareTo(b.getCategoryAndName()))
            .collect(Collectors.toCollection(ArrayListWithAsJsonMethod::new));
    }
    
    private static final Stream<SpelFunctionDescriptor> createFunctionDescriptorsStream(Class<?> spelFunctionClazz) {
        return Stream.of(spelFunctionClazz.getDeclaredMethods())
                .filter(m->!Modifier.isPrivate(m.getModifiers()))
                .map(m->createSpelFunctionDescriptor(spelFunctionClazz, m));
    }
    
    private static final SpelFunctionDescriptor createSpelFunctionDescriptor(Class<?> spelFunctionClazz, Method spelFunctionMethod) {
        var prefix = ReflectionHelper.getAnnotationValue(spelFunctionClazz, SpelFunctionPrefix.class, SpelFunctionPrefix::value, ()->"");
        var category = ReflectionHelper.getAnnotationValue(spelFunctionMethod, SpelFunction.class, SpelFunction::cat, ()->com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.util).name();
        var name = prefix + spelFunctionMethod.getName();
        var desc = ReflectionHelper.getAnnotationValue(spelFunctionMethod, SpelFunction.class, SpelFunction::desc, ()->"");
        var params = createParamDescriptors(spelFunctionMethod);
        var returns= createReturnDescriptor(spelFunctionMethod);
        var signature = createSignature(name, params, returns);
        return SpelFunctionDescriptor.builder()
                .clazz(spelFunctionClazz)
                .category(category)
                .name(name)
                .description(desc)
                .params(params)
                .returns(returns)
                .signature(signature)
                .build();
    }
    
    private static final String createSignature(String name, List<SpelFunctionParamDescriptor> params, SpelFunctionReturnDescriptor returns) {
        var paramsStringBuilder = new StringBuilder();
        for ( var p : params ) {
            var paramString = String.format("%s%s %s", paramsStringBuilder.isEmpty()?"":", ", p.getType(), p.getName());
            if ( p.isOptional() ) { paramString = String.format("[%s]", paramString); }
            paramsStringBuilder.append(paramString);
        }
        return String.format("%s #%s(%s)", returns.getType(), name, paramsStringBuilder.toString());
    }

    private static final SpelFunctionReturnDescriptor createReturnDescriptor(Method spelFunctionMethod) {
        var type = getJsonType(spelFunctionMethod.getReturnType());
        var desc = ReflectionHelper.getAnnotationValue(spelFunctionMethod, SpelFunction.class, SpelFunction::returns, ()->"N/A");
        return SpelFunctionReturnDescriptor.builder()
                .type(type)
                .description(desc)
                .build();
    }
    
    private static final List<SpelFunctionParamDescriptor> createParamDescriptors(Method spelFunctionMethod) {
    return Stream.of(spelFunctionMethod.getParameters())
            .map(p->createParamDescriptor(p))
            .collect(Collectors.toList());
    }

    private static final SpelFunctionParamDescriptor createParamDescriptor(Parameter param) {
        var name = getParamAnnotationValue(param, SpelFunctionParam::name, ()->param.getName());
        var type = getParamAnnotationValue(param, SpelFunctionParam::type, ()->getJsonType(param.getType()));
        var desc = getParamAnnotationValue(param, SpelFunctionParam::desc, ()->"no description available");
        var optional = getParamAnnotationValue(param, SpelFunctionParam::optional, ()->false);
        return SpelFunctionParamDescriptor.builder()
                .name(name)
                .description(desc)
                .type(type)
                .optional(optional)
                .build();
    }
    
    private static final <T> T getParamAnnotationValue(Parameter parameter, Function<SpelFunctionParam,T> valueRetriever, Supplier<T> defaultValueSupplier) {
        return ReflectionHelper.getAnnotationValue(parameter, SpelFunctionParam.class, valueRetriever, defaultValueSupplier);
    }

    // TODO Remove duplication with ActionSchemaHelper::getJsonType
    private static final String getJsonType(Class<?> clazz) {
        if (String.class.isAssignableFrom(clazz)) {
            return "string";
        }
        if (Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz)) {
            return "boolean";
        }
        if (Number.class.isAssignableFrom(clazz)) {
            return "number";
        } 
        if (Enum.class.isAssignableFrom(clazz)) {
            return "enum";
        }
        if (TemplateExpression.class.isAssignableFrom(clazz)) {
            return "expression";
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return "map";
        }
        if (Collection.class.isAssignableFrom(clazz) || clazz.isArray()) {
            return "array";
        } else {
            return "object";
        }
    }

    @Data @Builder
    @Reflectable
    public static final class SpelFunctionDescriptor {
        @JsonIgnore private final Class<?> clazz;
        private final String category;
        private final String name;
        private final String description;
        private final String signature;
        private final List<SpelFunctionParamDescriptor> params;
        private final SpelFunctionReturnDescriptor returns;
        
        @JsonIgnore private final String getCategoryAndName() {
            return String.format("[%s] %s", category, name);
        }
    }

    @Data @Builder
    @Reflectable
    public static final class SpelFunctionParamDescriptor {
        private final String name;
        private final String type;
        private final String description;
        private final boolean optional;
    }

    @Data @Builder
    @Reflectable
    public static final class SpelFunctionReturnDescriptor {
        private final String type;
        private final String description;

    }
    
    public static void main(String[] args) {
        System.out.println(SpelFunctionDescriptorsFactory.getActionSpelFunctionsDescriptors().asJson().toPrettyString());
    }
}