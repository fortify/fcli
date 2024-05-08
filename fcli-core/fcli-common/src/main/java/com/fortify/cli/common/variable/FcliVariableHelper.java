/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.variable;

import java.io.Writer;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.crypto.helper.EncryptionHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

// TODO This class could probably use some cleanup
public final class FcliVariableHelper {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern variableNamePattern = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final Pattern variableReferencePattern = Pattern.compile("^(-{1,2}[\\-_a-zA-Z0-9]{1,}=){0,1}::([a-zA-Z0-9_]+)::(.*)$");
    private FcliVariableHelper() {}
    
    @Data @EqualsAndHashCode(callSuper = true) @Builder 
    @Reflectable @NoArgsConstructor @AllArgsConstructor
    public static class VariableDescriptor extends JsonNodeHolder {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd@HH:mm:ss.SSSZ")
        private Date created;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd@HH:mm:ss.SSSZ")
        private transient Date accessed;
        private String name;
        private String defaultPropertyName; 
        private boolean singular;
        private boolean encrypted;
    }
    
    public static final Path getVariablesPath() {
        return FcliDataHelper.getFcliStatePath().resolve("vars");
    }
    
    @SneakyThrows // TODO Do we want to use SneakyThrows?
    public static final VariableDescriptor getVariableDescriptor(String variableName, boolean failIfUnavailable) {
        Path variablePath = getVariableDescriptorPathIfExists(variableName, failIfUnavailable);
        try {
            String variableDescriptor = FcliDataHelper.readFile(variablePath, failIfUnavailable);
            JsonNode variableDescriptorJson = variableDescriptor==null ? null : objectMapper.readValue(variableDescriptor, JsonNode.class);
            return JsonHelper.treeToValue(variableDescriptorJson, VariableDescriptor.class);
        } catch ( Exception e ) {
            FcliDataHelper.deleteDir(variablePath.getParent(), true);
            conditionalThrow(failIfUnavailable, ()->new IllegalStateException("Error reading variable descriptor, data has been deleted", e));
            // TODO Log warning message
            return null;
        }
    }
    
    @SneakyThrows // TODO Do we want to use SneakyThrows?
    public static final JsonNode getVariableContents(String variableName, boolean failIfUnavailable) {
        VariableDescriptor descriptor = getVariableDescriptor(variableName, failIfUnavailable);
        if ( descriptor==null ) { return null; }
        descriptor.setAccessed(new Date());
        Path variablePath = getVariableContentsPathIfExists(variableName, failIfUnavailable);
        try {
            String variableContents = FcliDataHelper.readFile(variablePath, failIfUnavailable);
            if ( descriptor.encrypted ) {
                variableContents = EncryptionHelper.decrypt(variableContents);
            }
            saveVariableDescriptor(descriptor);
            return variableContents==null ? null : objectMapper.readValue(variableContents, JsonNode.class);
        } catch ( Exception e ) {
            FcliDataHelper.deleteDir(variablePath.getParent(), true);
            conditionalThrow(failIfUnavailable, ()->new IllegalStateException("Error reading variable descriptor or contents, data has been deleted", e));
            // TODO Log warning message
            return null;
        }
    }
    
    public static final VariableDescriptor save(String variableName, String defaultPropertyName, JsonNode variableContents, boolean singular, boolean encrypt) {
        checkVariableName(variableName);
        VariableDescriptor descriptor = createVariableDescriptor(variableName, defaultPropertyName, singular, encrypt);
        saveVariableContents(descriptor, variableContents);
        return saveVariableDescriptor(descriptor);
    }
    
    @SneakyThrows // TODO Do we want to use SneakyThrows?
    public static final Writer getVariableContentsWriter(String variableName, String defaultPropertyName, boolean singular, boolean encrypt) {
        checkVariableName(variableName);
        VariableDescriptor descriptor = createVariableDescriptor(variableName, defaultPropertyName, singular, encrypt);
        saveVariableDescriptor(descriptor);
        VariableContentsWriter vcw = new VariableContentsWriter(getVariableContentsAbsolutePath(variableName).toString());
        return encrypt ? new EncryptionHelper.EncryptWriter(vcw) : vcw;
    }
    
    public static final String[] resolveVariables(String[] args) {
        return Stream.of(args).map(FcliVariableHelper::resolveVariable).toArray(String[]::new);
    }
    
    public static final String resolveVariable(String arg) {
        Matcher matcher = variableReferencePattern.matcher(arg);
        if (matcher.matches()) {
            String variableName = matcher.group(2);
            String propertyPath = getVariablePropertyPathOrDefault(variableName, matcher.group(3));
            JsonNode contents = getVariableContents(variableName, true);
            String value = JsonHelper.evaluateSpelExpression(contents, propertyPath, String.class);
            if ( value==null ) {
                throw new IllegalArgumentException(String.format("Property path '%s' for variable '%s' resolves to null", propertyPath, variableName));
            }
            return matcher.group(1)!=null ? matcher.group(1)+value : value;
        }
        return arg;
    }
    
    private static final String getVariablePropertyPathOrDefault(String variableName, String propertyPath) {
        if ( StringUtils.isNotBlank(propertyPath) ) { return propertyPath; }
        String defaultPropertyName = getVariableDescriptor(variableName, true).getDefaultPropertyName();
        if ( StringUtils.isNotBlank(defaultPropertyName) ) { return defaultPropertyName; }
        throw new IllegalArgumentException("No property name specified for variable "+variableName+", and no default property name available");
    }
    
    private static final void checkVariableName(String variableName) {
        if ( !variableNamePattern.matcher(variableName).matches() ) {
            throw new IllegalArgumentException(String.format("Variable name '%s' doesn't match pattern '%s'", variableName, variableNamePattern.pattern()));
        }
    }
    
    private static final VariableDescriptor createVariableDescriptor(String variableName, String defaultPropertyName, boolean singular, boolean encrypt) {
        Date currentDateTime = new Date();
		return VariableDescriptor.builder()
                .created(currentDateTime)
                .accessed(currentDateTime)
                .defaultPropertyName(defaultPropertyName)
                .name(variableName)
                .singular(singular)
                .encrypted(encrypt)
                .build();
    }
    
    @SneakyThrows // TODO Do we want to use SneakyThrows? 
    private static final VariableDescriptor saveVariableDescriptor(VariableDescriptor descriptor) {
        String variableDescriptorString = objectMapper.writeValueAsString(descriptor);
        FcliDataHelper.saveFile(getVariableDescriptorRelativePath(descriptor.getName()), variableDescriptorString, true);
        return descriptor;
    }
    
    @SneakyThrows // TODO Do we want to use SneakyThrows?
    private static void saveVariableContents(VariableDescriptor descriptor, JsonNode variableContents) {
        String variableContentsString = objectMapper.writeValueAsString(variableContents);
        if ( descriptor.encrypted ) {
            variableContentsString = EncryptionHelper.encrypt(variableContentsString);
        }
        FcliDataHelper.saveFile(getVariableContentsRelativePath(descriptor.getName()), variableContentsString, true);
    }

    @SneakyThrows // TODO Do we want to use SneakyThrows?
    public static final void delete(String variableName) {
        Path variableDirPath = getVariablePathIfExists(variableName, getVariablePath(variableName), false);
        if ( variableDirPath!=null ) {
            FcliDataHelper.deleteDir(variableDirPath, true);
        }
    }
    
    public static final void delete(JsonNode variableDescriptor) {
        delete(variableDescriptor.get("name").asText());
    }
    
    public static final boolean exists(String variableName) {
        return FcliDataHelper.isReadable(getVariablePath(variableName));
    }
    
    public static final List<String> variableNames() {
        return variableNamesStream().collect(Collectors.toList());
    }
    
    @SneakyThrows // TODO Do we want to use SneakyThrows?
    public static final Stream<String> variableNamesStream() {
        Path path = getVariablesPath();
        if ( !FcliDataHelper.exists(path) ) {
            return Stream.empty();
        }
        return FcliDataHelper.listDirsInDir(path, true)
                .map(Path::getFileName)
                .map(Path::toString);
    }
    
    public static final ArrayNode listDescriptors() {
        return variableNames().stream()
                .map(FcliVariableHelper::getVariableDescriptorAsJson)
                .collect(JsonHelper.arrayNodeCollector());
    }
    
    private static final JsonNode getVariableDescriptorAsJson(String variableName) {
        return getVariableDescriptor(variableName, true).asJsonNode();
    }
    
    private static final void conditionalThrow(boolean throwException, Supplier<RuntimeException> exceptionSupplier) {
        if ( throwException ) { throw exceptionSupplier.get(); }
    }

    private static final Path getVariableDescriptorPathIfExists(String variableName, boolean failIfUnavailable) {
        return getVariablePathIfExists(variableName, getVariableDescriptorRelativePath(variableName), failIfUnavailable);
    }
    
    private static final Path getVariableContentsPathIfExists(String variableName, boolean failIfUnavailable) {
        return getVariablePathIfExists(variableName, getVariableContentsRelativePath(variableName), failIfUnavailable);
    }
    
    private static final Path getVariablePathIfExists(String variableName, Path path, boolean failIfUnavailable) {
        if ( failIfUnavailable && !FcliDataHelper.isReadable(path) ) {
            throw new IllegalStateException("Variable "+variableName+" not found");
        }
        return path;
    }
    
    private static final Path getVariablePath(String variableName) {
        return getVariablesPath().resolve(variableName);
    }
    
    private static final Path getVariableDescriptorRelativePath(String variableName) {
        return getVariablePath(variableName).resolve("descriptor.json");
    }
    
    private static final Path getVariableContentsRelativePath(String variableName) {
        return getVariablePath(variableName).resolve("contents.json");
    }
    
    private static final Path getVariableContentsAbsolutePath(String variableName) {
        return FcliDataHelper.getFcliHomePath().resolve(getVariableContentsRelativePath(variableName));
    }
}
