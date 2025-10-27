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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.json.IWithAsJsonMethod;
import com.fortify.cli.common.util.ReflectionHelper;

import lombok.Builder;
import lombok.Data;

@Data
public class ActionSchemaDescriptorFactory {
    public static final ActionSchemaDescriptor getActionSchemaDescriptor() {
        return new ActionSchemaDescriptor();
    }
    
    @Data @Reflectable
    public static final class ActionSchemaDescriptor implements IWithAsJsonMethod {
        @JsonIgnore private final Map<Class<?>, ActionSchemaJsonTypeDescriptor> typesByJavaType = new LinkedHashMap<>();
        private final Map<String, ActionSchemaJsonTypeDescriptor> typesByName = new LinkedHashMap<>();
        private final List<ActionSchemaJsonTypeDescriptor> types = new ArrayList<>();
        private final Map<String, ActionSchemaJsonPropertyDescriptor> propertiesByQualifiedName = new LinkedHashMap<>();
        
        public ActionSchemaDescriptor() {
            collectActionElementTypes(Action.class, null);
            types.sort((a,b)->a.name.compareTo(b.name));
        }

        /**
         * This method recursively processes all action model classes that are referenced by JSON property fields
         * declared in the given class or any of its superclasses.
         */
        private void collectActionElementTypes(Class<?> clazz, ActionSchemaJsonPropertyDescriptor referencedFrom) {
            var existingTypeDescriptor = typesByJavaType.get(clazz);
            if ( existingTypeDescriptor!=null ) {
                existingTypeDescriptor.addReferencedFrom(referencedFrom);
            } else if ( ActionSchemaHelper.isActionModelClazz(clazz) ) {
                var descriptor = addActionElementType(clazz, referencedFrom);
                for (var p : descriptor.getPropertyDescriptors() ) {
                    propertiesByQualifiedName.put(p.getQualifiedName(), p);
                    for ( var fieldType : ReflectionHelper.getAllTypes(p.getField()) ) {
                        collectActionElementTypes(fieldType, p);
                    }
                }
            }
        }

        private ActionSchemaJsonTypeDescriptor addActionElementType(Class<?> clazz, ActionSchemaJsonPropertyDescriptor referencedFrom) {
            var typeDescriptor = ActionSchemaJsonTypeDescriptor.from(clazz, referencedFrom, this::getJsonType);
            typesByJavaType.put(clazz, typeDescriptor);
            typesByName.put(typeDescriptor.getName(), typeDescriptor);
            types.add(typeDescriptor);
            return typeDescriptor;
        }
        
        private final String getJsonType(Class<?> clazz) {
            var descriptor = typesByJavaType.get(clazz);
            var jsonType = descriptor==null ? "object" : descriptor.getName(); 
            return ActionSchemaHelper.getJsonType(clazz, c->jsonType); 
        }
    }
    
    @Data @Builder @Reflectable
    public static final class ActionSchemaJsonTypeDescriptor {
        private final Class<?> javaType;
        private final String name;
        @JsonIgnore private final List<ActionSchemaJsonPropertyDescriptor> propertyDescriptors;
        private final List<String> properties;
        private final String description;
        private final Set<String> referencedFromProperties;
        private final String[] sampleSnippets;
        
        public void addReferencedFrom(ActionSchemaJsonPropertyDescriptor referencedFrom) {
            if ( referencedFrom!=null ) {
                referencedFromProperties.add(referencedFrom.getQualifiedName());
            }
        }
        
        public static final ActionSchemaJsonTypeDescriptor from(Class<?> clazz, ActionSchemaJsonPropertyDescriptor referencedFrom, Function<Class<?>, String> javaToJsonTypeConverter) {
            var name = ReflectionHelper.getAnnotationValue(clazz, JsonTypeName.class, JsonTypeName::value, ()->clazz.getSimpleName());
            var result = ActionSchemaJsonTypeDescriptor.builder()
                .javaType(clazz)
                .name(name)
                .description(ReflectionHelper.getAnnotationValue(clazz, JsonClassDescription.class, JsonClassDescription::value, ()->""))
                .propertyDescriptors(new ArrayList<>())
                .properties(new ArrayList<>())
                .referencedFromProperties(new HashSet<String>())
                .sampleSnippets(ActionSchemaHelper.getSampleYamlSnippets(clazz))
                .build();
            for ( var field : getAllJsonPropertyFields(clazz) ) {
                var pd = ActionSchemaJsonPropertyDescriptor.from(name, field, javaToJsonTypeConverter);
                result.getPropertyDescriptors().add(pd);
                result.getProperties().add(pd.getQualifiedName());
            }
            result.addReferencedFrom(referencedFrom);
            return result;
        }
    }
    
    /** Get all fields with {@link JsonProperty} annotation from the given class and all of its superclasses */
    private static final List<Field> getAllJsonPropertyFields(Class<?> clazz) {
        var result = new ArrayList<Field>();
        var currentClazz = clazz;
        while ( ActionSchemaHelper.isActionModelClazz(currentClazz) ) {
            for ( var field : currentClazz.getDeclaredFields() ) {
                if ( field.isAnnotationPresent(JsonProperty.class) ) {
                    result.add(field);
                }
            }
            currentClazz = currentClazz.getSuperclass();
        }
        return result;
    }
    
    @Data @Builder(toBuilder = true) @Reflectable
    public static final class ActionSchemaJsonPropertyDescriptor {
        private final String name;
        private final String qualifiedName;
        private final String qualifiedParentName;
        private final String[] allQualifiedParentNames;
        private final String description;
        private final String[] sampleSnippets;
        final Class<?> javaType;
        private final Class<?>[] javaTypeArgs;
        // We lazily evaluate json types
        @JsonIgnore private final Function<Class<?>, String> javaToJsonTypeConverter; 
        @JsonIgnore private final Field field;
        
        public final String getJsonGenericType() {
            return javaToJsonTypeConverter.apply(javaType);
        }
        
        public final String[] getJsonGenericTypeArgs() {
            return Stream.of(javaTypeArgs).map(javaToJsonTypeConverter).toArray(String[]::new);
        }
        
        public final String getJsonType() {
            var genericType = getJsonGenericType();
            var genericTypeArgs = getJsonGenericTypeArgs();
            return genericTypeArgs==null || genericTypeArgs.length==0 
                    ? genericType
                    : String.format("%s<%s>", genericType, String.join(",", genericTypeArgs));
        }
        
        public static final ActionSchemaJsonPropertyDescriptor from(String qualifiedParentName, Field field, Function<Class<?>, String> javaToJsonTypeConverter) {
            var name = ReflectionHelper.getAnnotationValue(field, JsonProperty.class, JsonProperty::value, ()->field.getName());
            var qualifiedName = StringUtils.isBlank(qualifiedParentName) ? name : String.format("%s::%s", qualifiedParentName, name);
            var javaType = field.getType();
            var javaGenericTypes = ReflectionHelper.getGenericTypes(field);
        var fieldDescriptor = ActionSchemaJsonPropertyDescriptor.builder()
                    .name(name)
                    .qualifiedName(qualifiedName)
                    .qualifiedParentName(qualifiedParentName)
                    .allQualifiedParentNames(getAllQualifiedParentNames(qualifiedName))
                    .javaType(javaType)
                    .javaTypeArgs(javaGenericTypes)
                    .javaToJsonTypeConverter(javaToJsonTypeConverter)
                    .description(ActionSchemaHelper.getFullJsonPropertyDescription(field))
                    .sampleSnippets(ActionSchemaHelper.getSampleYamlSnippets(field))
                    .field(field)
                    .build();
            return fieldDescriptor;
        }
        
        private static final String getQualifiedParentName(String qualifiedName) {
            var idx = qualifiedName==null ? -1 : qualifiedName.lastIndexOf("::");
            return idx==-1 ? "" : qualifiedName.substring(0, idx);
        }
        
        private static final String[] getAllQualifiedParentNames(String qualifiedName) {
            var result = new ArrayList<String>();
            var currentParentName = getQualifiedParentName(qualifiedName);
            while ( !"".equals(currentParentName) ) {
                result.add(currentParentName);
                currentParentName = getQualifiedParentName(currentParentName);
            }
            return result.toArray(String[]::new);
        }
    }
    
    public static void main(String[] args) {
        System.out.println(ActionSchemaDescriptorFactory.getActionSchemaDescriptor().asJson().toPrettyString());
    }
}
