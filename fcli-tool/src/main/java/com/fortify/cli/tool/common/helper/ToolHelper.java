package com.fortify.cli.tool.common.helper;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.micronaut.core.annotation.ReflectiveAccess;

@ReflectiveAccess
public final class ToolHelper {
    public static final ToolInstallDescriptor getToolInstallDescriptor(String toolName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String resourceFile = getResourceFile(toolName, String.format("%s.yaml", toolName));
        try ( InputStream file = classLoader.getResourceAsStream(resourceFile) ) { 
            ObjectMapper om = new ObjectMapper(new YAMLFactory());
            return om.readValue(file, ToolInstallDescriptor.class);
        } catch (IOException e) {
            throw new RuntimeException("Error loading resource file: "+resourceFile, e);
        }
    }
    
    public static final String getResourceDir(String toolName) {
        return String.format("com/fortify/cli/tool/%s", toolName);
    }
    
    public static final String getResourceFile(String toolName, String fileName) {
        return String.format("%s/%s", getResourceDir(toolName), fileName);
    }
}
