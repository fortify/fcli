package com.fortify.cli.tool.common.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.util.FcliHomeHelper;

import io.micronaut.core.annotation.ReflectiveAccess;

@ReflectiveAccess
public final class ToolHelper {
    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    
    public static final ToolDownloadDescriptor getToolDownloadDescriptor(String toolName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String resourceFile = getResourceFile(toolName, String.format("%s.yaml", toolName));
        try ( InputStream file = classLoader.getResourceAsStream(resourceFile) ) { 
            return yamlObjectMapper.readValue(file, ToolDownloadDescriptor.class);
        } catch (IOException e) {
            throw new RuntimeException("Error loading resource file: "+resourceFile, e);
        }
    }
    
    public static final ToolVersionCombinedDescriptor saveToolVersionInstallDescriptor(String toolName, ToolVersionInstallDescriptor installDescriptor) {
        ToolVersionDownloadDescriptor downloadDescriptor = installDescriptor.getOriginalDownloadDescriptor();
        FcliHomeHelper.saveFile(getInstallDescriptorPath(toolName, downloadDescriptor.getVersion()), installDescriptor, true);
        return new ToolVersionCombinedDescriptor(toolName, downloadDescriptor, installDescriptor);
    }
    
    public static final ToolVersionInstallDescriptor loadToolVersionInstallDescriptor(String toolName, String version) {
        return FcliHomeHelper.readFile(getInstallDescriptorPath(toolName, version), ToolVersionInstallDescriptor.class, false);
    }
    
    public static final ToolVersionCombinedDescriptor loadToolVersionCombinedDescriptor(String toolName, String version) {
        ToolVersionInstallDescriptor installDescriptor = loadToolVersionInstallDescriptor(toolName, version);
        return installDescriptor==null ? null : new ToolVersionCombinedDescriptor(toolName, null, installDescriptor);
    }
    
    public static final void deleteToolVersionInstallDescriptor(String toolName, String version) {
        FcliHomeHelper.deleteFile(getInstallDescriptorPath(toolName, version), true);
    }
    
    public static final ToolVersionCombinedDescriptor[] getToolVersionCombinedDescriptors(String toolName) {
        return getToolVersionCombinedDescriptorsStream(toolName)
            .toArray(ToolVersionCombinedDescriptor[]::new);
    }

    public static final Stream<ToolVersionCombinedDescriptor> getToolVersionCombinedDescriptorsStream(String toolName) {
        return getToolDownloadDescriptor(toolName).getVersionsStream()
            .map(d->new ToolVersionCombinedDescriptor(toolName, d, loadToolVersionInstallDescriptor(toolName, d.getVersion())));
    }
    
    public static final String getResourceDir(String toolName) {
        return String.format("com/fortify/cli/tool/%s", toolName);
    }
    
    public static final String getResourceFile(String toolName, String fileName) {
        return String.format("%s/%s", getResourceDir(toolName), fileName);
    }
    
    private static final Path getInstallDescriptorPath(String toolName, String version) {
        return FcliHomeHelper.getFcliStatePath().resolve("tools").resolve(toolName).resolve(version);
    }
}
