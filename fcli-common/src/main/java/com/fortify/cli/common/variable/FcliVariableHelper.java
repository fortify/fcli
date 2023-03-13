/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.common.variable;

import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.JsonNodeHolder;
import com.fortify.cli.common.util.EncryptionHelper;
import com.fortify.cli.common.util.FcliHomeHelper;
import com.fortify.cli.common.util.StringUtils;

import io.micronaut.core.annotation.ReflectiveAccess;
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
    private static final Pattern variableReferencePattern = Pattern.compile("^::([a-zA-Z0-9_]+)::(.*)$");
    private FcliVariableHelper() {}
    
    @Data @EqualsAndHashCode(callSuper = true) @ReflectiveAccess @Builder @NoArgsConstructor @AllArgsConstructor
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
        return FcliHomeHelper.getFcliStatePath().resolve("vars");
    }
    
    @SneakyThrows // TODO Do we want to use SneakyThrows?
    public static final VariableDescriptor getVariableDescriptor(String variableName, boolean failIfUnavailable) {
        Path variablePath = getVariableDescriptorPathIfExists(variableName, failIfUnavailable);
        try {
            String variableDescriptor = FcliHomeHelper.readFile(variablePath, failIfUnavailable);
            JsonNode variableDescriptorJson = variableDescriptor==null ? null : objectMapper.readValue(variableDescriptor, JsonNode.class);
            return JsonHelper.treeToValue(variableDescriptorJson, VariableDescriptor.class);
        } catch ( Exception e ) {
            FcliHomeHelper.deleteDir(variablePath.getParent(), true);
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
            String variableContents = FcliHomeHelper.readFile(variablePath, failIfUnavailable);
            if ( descriptor.encrypted ) {
                variableContents = EncryptionHelper.decrypt(variableContents);
            }
            saveVariableDescriptor(descriptor);
            return variableContents==null ? null : objectMapper.readValue(variableContents, JsonNode.class);
        } catch ( Exception e ) {
            FcliHomeHelper.deleteDir(variablePath.getParent(), true);
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
        PrintWriter pw = new PrintWriter(Files.newOutputStream(getVariableContentsAbsolutePath(variableName), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
        return encrypt ? new EncryptionHelper.EncryptWriter(pw) : pw;
    }
    
    public static final String[] resolveVariables(String[] args) {
        return Stream.of(args).map(FcliVariableHelper::resolveVariable).toArray(String[]::new);
    }
    
    public static final String resolveVariable(String arg) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = variableReferencePattern.matcher(arg);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String propertyPath = getVariablePropertyPathOrDefault(variableName, matcher.group(2));
            JsonNode contents = getVariableContents(variableName, true);
            String value = JsonHelper.evaluateJsonPath(contents, propertyPath, String.class);
            if ( value==null ) {
                throw new IllegalArgumentException(String.format("Property path '%s' for variable '%s' resolves to null", propertyPath, variableName));
            }
            matcher.appendReplacement(sb, value);
        }
        matcher.appendTail(sb);
        return sb.toString();
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
        FcliHomeHelper.saveFile(getVariableDescriptorRelativePath(descriptor.getName()), variableDescriptorString, true);
        return descriptor;
    }
    
    @SneakyThrows // TODO Do we want to use SneakyThrows?
    private static void saveVariableContents(VariableDescriptor descriptor, JsonNode variableContents) {
        String variableContentsString = objectMapper.writeValueAsString(variableContents);
        if ( descriptor.encrypted ) {
            variableContentsString = EncryptionHelper.encrypt(variableContentsString);
        }
        FcliHomeHelper.saveFile(getVariableContentsRelativePath(descriptor.getName()), variableContentsString, true);
    }

    @SneakyThrows // TODO Do we want to use SneakyThrows?
    public static final void delete(String variableName) {
        Path variableDirPath = getVariablePathIfExists(variableName, getVariablePath(variableName), false);
        if ( variableDirPath!=null ) {
            FcliHomeHelper.deleteDir(variableDirPath, true);
        }
    }
    
    public static final void delete(JsonNode variableDescriptor) {
        delete(variableDescriptor.get("name").asText());
    }
    
    public static final boolean exists(String variableName) {
        return FcliHomeHelper.isReadable(getVariablePath(variableName));
    }
    
    public static final List<String> variableNames() {
        return variableNamesStream().collect(Collectors.toList());
    }
    
    @SneakyThrows // TODO Do we want to use SneakyThrows?
    public static final Stream<String> variableNamesStream() {
        Path path = getVariablesPath();
        if ( !FcliHomeHelper.exists(path) ) {
            return Stream.empty();
        }
        return FcliHomeHelper.listDirsInDir(path, true)
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
        if ( failIfUnavailable && !FcliHomeHelper.isReadable(path) ) {
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
        return FcliHomeHelper.getFcliHomePath().resolve(getVariableContentsRelativePath(variableName));
    }
}
