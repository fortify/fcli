/*
 * Copyright 2021-2026 Open Text.
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.ArrayListWithAsJsonMethod;
import com.fortify.cli.common.spel.fn.descriptor.annotation.RenderSubFunctionsMode;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctions;
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
        // FoD & SSC classes are only available at runtime, so we need to specify them by name
        return getSpelFunctionsDescriptors(
                "com.fortify.cli.common.spel.fn.SpelFunctionsStandard",
                "com.fortify.cli.common.action.runner.ActionSpelFunctions",
                "com.fortify.cli.common.action.runner.ActionRunnerContextSpelFunctions",
                "com.fortify.cli.fod.action.helper.FoDActionSpelFunctions",
                "com.fortify.cli.ssc.action.helper.SSCActionSpelFunctions",
                "com.fortify.cli.common.action.helper.ci.ActionCiSpelFunctions",
                "com.fortify.cli.common.action.helper.ci.ado.ActionAdoSpelFunctions",
                "com.fortify.cli.common.action.helper.ci.github.ActionGitHubSpelFunctions",
                "com.fortify.cli.common.action.helper.ci.gitlab.ActionGitLabSpelFunctions",
                "com.fortify.cli.common.action.helper.ci.bitbucket.ActionBitbucketSpelFunctions",
                "com.fortify.cli.common.action.helper.fs.ActionFileSystemSpelFunctions"
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
            .flatMap(d->createNestedFunctionDescriptorsStream(d))
            .sorted((a, b) -> a.getCategoryAndName().compareTo(b.getCategoryAndName()))
            .collect(Collectors.toCollection(ArrayListWithAsJsonMethod::new));
    }
    
    private static final Stream<SpelFunctionDescriptor> createFunctionDescriptorsStream(Class<?> spelFunctionClazz) {
        return Stream.of(spelFunctionClazz.getDeclaredMethods())
                .filter(m->!Modifier.isPrivate(m.getModifiers()))
                .map(m->createSpelFunctionDescriptor(spelFunctionClazz, m));
    }
    
    /**
     * For each descriptor, check if its renderSubFunctions mode.
     * If so, generate additional descriptors for all public methods of that return type,
     * using the original function displayName as the prefix.
     */
    private static final Stream<SpelFunctionDescriptor> createNestedFunctionDescriptorsStream(SpelFunctionDescriptor parentDescriptor) {
        // First yield the parent descriptor itself
        Stream<SpelFunctionDescriptor> parentStream = Stream.of(parentDescriptor);
        
        // Check if we should render returned functions
        if (parentDescriptor.getRenderSubFunctionsMode() == RenderSubFunctionsMode.AUTO) {
            return parentStream;
        }
        
        // Get the return type from the parent descriptor's class
        Class<?> returnType = parentDescriptor.getClazz();
        try {
            // Try to resolve the actual return type from the method
            String methodName = parentDescriptor.getName().replaceAll(".*\\.", "");
            Method method = Stream.of(returnType.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .filter(m -> !Modifier.isPrivate(m.getModifiers()))
                .findFirst()
                .orElse(null);
            
            if (method != null) {
                Class<?> actualReturnType = method.getReturnType();
                if (actualReturnType.isAnnotationPresent(SpelFunctions.class)) {
                    // Process all public methods of the return type
                    // Use displayName with () for method calls as prefix
                    String prefix = parentDescriptor.getDisplayName() + (parentDescriptor.isProperty() ? "." : "().");
                    var renderSubFunctionsMode = parentDescriptor.getRenderSubFunctionsMode();
                    boolean renderInline = renderSubFunctionsMode == RenderSubFunctionsMode.INLINE;
                    Stream<SpelFunctionDescriptor> nestedStream = Stream.of(actualReturnType.getDeclaredMethods())
                        .filter(m -> Modifier.isPublic(m.getModifiers()))
                        .filter(m -> m.isAnnotationPresent(SpelFunction.class))
                        .map(m -> createNestedSpelFunctionDescriptor(actualReturnType, m, prefix, renderInline));
                    
                    return Stream.concat(parentStream, nestedStream);
                }
            }
        } catch (Exception e) {
            // If we can't process nested functions, just return the parent
        }
        
        return parentStream;
    }
    
    /**
     * Create a descriptor for a method from a class annotated with @SpelFunctions,
     * using the provided prefix instead of the class-level @SpelFunctionPrefix.
     */
    private static final SpelFunctionDescriptor createNestedSpelFunctionDescriptor(Class<?> spelFunctionClazz, Method spelFunctionMethod, String prefix, boolean renderInline) {
        var category = ReflectionHelper.getAnnotationValue(spelFunctionMethod, SpelFunction.class, SpelFunction::cat, ()->SpelFunctionCategory.util).name();
        var name = prefix + spelFunctionMethod.getName();
        var displayName = deriveDisplayName(prefix, spelFunctionMethod.getName());
        var isProperty = isPropertyGetter(spelFunctionMethod.getName());
        var desc = ReflectionHelper.getAnnotationValue(spelFunctionMethod, SpelFunction.class, SpelFunction::desc, ()->"");
        var params = createParamDescriptors(spelFunctionMethod);
        var returns = createReturnDescriptor(spelFunctionMethod);
        var signature = createSignature(displayName, params, returns, isProperty);
        var renderMode = ReflectionHelper.getAnnotationValue(spelFunctionMethod, SpelFunction.class, 
            SpelFunction::renderSubFunctions, 
            ()->RenderSubFunctionsMode.AUTO);
        var renderSubFunctionsMode = resolveRenderSubFunctionsMode(renderMode, spelFunctionMethod.getReturnType());
        
        return SpelFunctionDescriptor.builder()
                .clazz(spelFunctionClazz)
                .category(category)
                .name(name)
                .displayName(displayName)
                .description(desc)
                .params(params)
                .returns(returns)
                .signature(signature)
                .isProperty(isProperty)
                .renderSubFunctionsMode(renderSubFunctionsMode)
                .renderInline(renderInline)
                .build();
    }
    
    /**
     * Resolve the effective render mode for sub-functions.
     * @param mode The render mode from the annotation
     * @param returnType The return type of the method
     * @return the resolved render mode
     */
    private static final RenderSubFunctionsMode resolveRenderSubFunctionsMode(
            RenderSubFunctionsMode mode, Class<?> returnType) {
        return switch(mode) {
            case SECTION -> RenderSubFunctionsMode.SECTION;
            case INLINE -> RenderSubFunctionsMode.INLINE;
            case AUTO -> returnType.isAnnotationPresent(SpelFunctions.class) 
                ? RenderSubFunctionsMode.SECTION
                : RenderSubFunctionsMode.AUTO;
        };
    }
    
    private static final SpelFunctionDescriptor createSpelFunctionDescriptor(Class<?> spelFunctionClazz, Method spelFunctionMethod) {
        var prefix = ReflectionHelper.getAnnotationValue(spelFunctionClazz, SpelFunctionPrefix.class, SpelFunctionPrefix::value, ()->"");
        var category = ReflectionHelper.getAnnotationValue(spelFunctionMethod, SpelFunction.class, SpelFunction::cat, ()->SpelFunctionCategory.util).name();
        var name = prefix + spelFunctionMethod.getName();
        var displayName = deriveDisplayName(prefix, spelFunctionMethod.getName());
        var isProperty = isPropertyGetter(spelFunctionMethod.getName());
        var desc = ReflectionHelper.getAnnotationValue(spelFunctionMethod, SpelFunction.class, SpelFunction::desc, ()->"");
        var params = createParamDescriptors(spelFunctionMethod);
        var returns= createReturnDescriptor(spelFunctionMethod);
        var signature = createSignature(displayName, params, returns, isProperty);
        var renderMode = ReflectionHelper.getAnnotationValue(spelFunctionMethod, SpelFunction.class, 
            SpelFunction::renderSubFunctions, 
            ()->RenderSubFunctionsMode.AUTO);
        var renderSubFunctionsMode = resolveRenderSubFunctionsMode(renderMode, spelFunctionMethod.getReturnType());
        // All top-level functions should have section headers
        var renderInline = false;
        
        return SpelFunctionDescriptor.builder()
                .clazz(spelFunctionClazz)
                .category(category)
                .name(name)
                .displayName(displayName)
                .description(desc)
                .params(params)
                .returns(returns)
                .signature(signature)
                .isProperty(isProperty)
                .renderSubFunctionsMode(renderSubFunctionsMode)
                .renderInline(renderInline)
                .build();
    }
    
    /**
     * Derive display name from method name. For getter methods (getXxx), 
     * returns property-style name (xxx). Otherwise returns method name as-is.
     */
    private static final String deriveDisplayName(String prefix, String methodName) {
        if (isPropertyGetter(methodName)) {
            var propertyName = methodName.substring(3);
            propertyName = Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
            return prefix + propertyName;
        }
        return prefix + methodName;
    }
    
    /**
     * Check if method name follows getter pattern (getXxx with uppercase after 'get').
     */
    private static final boolean isPropertyGetter(String methodName) {
        return methodName.startsWith("get") && methodName.length() > 3 && Character.isUpperCase(methodName.charAt(3));
    }
    
    private static final String createSignature(String name, List<SpelFunctionParamDescriptor> params, SpelFunctionReturnDescriptor returns, boolean isProperty) {
        // For properties (getters), omit parentheses. For methods, include them with parameters.
        if (isProperty) {
            return String.format("%s #%s", returns.getType(), name);
        }
        var paramsStringBuilder = new StringBuilder();
        for ( var p : params ) {
            var paramString = String.format("%s%s %s", paramsStringBuilder.isEmpty()?"":", ", p.getType(), p.getName());
            if ( p.isOptional() ) { paramString = String.format("[%s]", paramString); }
            paramsStringBuilder.append(paramString);
        }
        return String.format("%s #%s(%s)", returns.getType(), name, paramsStringBuilder.toString());
    }

    private static final SpelFunctionReturnDescriptor createReturnDescriptor(Method spelFunctionMethod) {
        var methodReturnType = spelFunctionMethod.getReturnType();
        var type = getJsonType(methodReturnType);
        var desc = ReflectionHelper.getAnnotationValue(spelFunctionMethod, SpelFunction.class, SpelFunction::returns, ()->"N/A");
        var annotatedReturnType = ReflectionHelper.getAnnotationValue(spelFunctionMethod, SpelFunction.class, SpelFunction::returnType, ()->void.class);
        
        // Smart derivation: use annotated type if specified, else derive from method return type
        var effectiveReturnType = (annotatedReturnType != null && annotatedReturnType != void.class) 
            ? annotatedReturnType 
            : (methodReturnType.isRecord() || methodReturnType.isAnnotationPresent(SpelFunctions.class)) 
                ? methodReturnType 
                : null;
        
        var returnTypeClassName = (effectiveReturnType != null) ? effectiveReturnType.getName() : null;
        var returnTypeStructure = (effectiveReturnType != null) ? buildReturnTypeStructure(effectiveReturnType) : null;
        return SpelFunctionReturnDescriptor.builder()
                .type(type)
                .description(desc)
                .returnType(returnTypeClassName)
                .returnTypeStructure(returnTypeStructure)
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

    private static final String buildReturnTypeStructure(Class<?> clazz) {
        if (clazz == null || clazz == void.class || !clazz.isRecord()) {
            return null;
        }
        
        var mapper = com.fortify.cli.common.json.JsonHelper.getObjectMapper();
        var structure = buildReturnTypeStructureNode(clazz, mapper);
        
        if (structure == null || structure.isEmpty()) {
            return null;
        }
        
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(structure);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static final ObjectNode buildReturnTypeStructureNode(Class<?> clazz, com.fasterxml.jackson.databind.ObjectMapper mapper) {
        if (clazz == null || clazz == void.class || !clazz.isRecord()) {
            return null;
        }
        
        var structure = mapper.createObjectNode();
        
        // Sort components by name for deterministic output
        Stream.of(clazz.getRecordComponents())
            .sorted((a, b) -> a.getName().compareTo(b.getName()))
            .forEach(component -> {
            var componentType = component.getType();
            // Check for @JsonProperty annotation on the accessor method
            var componentName = component.getName();
            try {
                var accessor = component.getAccessor();
                var jsonProperty = accessor.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class);
                if (jsonProperty != null && !jsonProperty.value().isEmpty()) {
                    componentName = jsonProperty.value();
                }
            } catch (Exception e) {
                // If we can't get the annotation, just use the component name
            }
            
            if (componentType.isRecord()) {
                // Recursively build structure for nested records
                var nestedStructure = buildReturnTypeStructureNode(componentType, mapper);
                if (nestedStructure != null) {
                    structure.set(componentName, nestedStructure);
                }
            } else if (String.class.isAssignableFrom(componentType)) {
                structure.put(componentName, "string");
            } else if (Boolean.class.isAssignableFrom(componentType) || boolean.class.isAssignableFrom(componentType)) {
                structure.put(componentName, "boolean");
            } else if (Number.class.isAssignableFrom(componentType) || componentType.isPrimitive()) {
                structure.put(componentName, "number");
            } else if (componentType.isEnum()) {
                structure.put(componentName, "enum");
            } else if (Map.class.isAssignableFrom(componentType)) {
                structure.put(componentName, "map");
            } else if (Collection.class.isAssignableFrom(componentType) || componentType.isArray()) {
                structure.put(componentName, "array");
            } else {
                // For other complex types, recursively try to build structure
                var nestedStructure = buildReturnTypeStructureNode(componentType, mapper);
                if (nestedStructure != null) {
                    structure.set(componentName, nestedStructure);
                } else {
                    structure.put(componentName, "object");
                }
            }
        });
        
        return structure.isEmpty() ? null : structure;
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
        private final String displayName;
        private final String description;
        private final String signature;
        private final List<SpelFunctionParamDescriptor> params;
        private final SpelFunctionReturnDescriptor returns;
        @JsonIgnore private final boolean isProperty;
        @JsonIgnore private final RenderSubFunctionsMode renderSubFunctionsMode;
        private final boolean renderInline;
        
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
        private final String returnType;
        private final String returnTypeStructure;
    }
    
    public static void main(String[] args) {
        System.out.println(SpelFunctionDescriptorsFactory.getActionSpelFunctionsDescriptors().asJson().toPrettyString());
    }
}