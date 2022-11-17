package com.fortify.cli.tool.common.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.FcliHomeHelper;
import com.fortify.cli.common.util.StringUtils;

import io.micronaut.core.annotation.ReflectiveAccess;

@ReflectiveAccess
public final class ToolHelper {
    private static final ObjectMapper jsonObjectMapper = JsonHelper.getObjectMapper();
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
    
    public static final ToolVersionCombinedDescriptor saveToolVersionInstallDescriptor(String toolName, ToolVersionInstallDescriptor installDescriptor) throws IOException {
        ToolVersionDownloadDescriptor downloadDescriptor = installDescriptor.getOriginalDownloadDescriptor();
        String contents = jsonObjectMapper.writeValueAsString(installDescriptor);
        FcliHomeHelper.saveFile(getInstallDescriptorPath(toolName, downloadDescriptor.getVersion()), contents);
        return new ToolVersionCombinedDescriptor(toolName, downloadDescriptor, installDescriptor);
    }
    
    public static final ToolVersionInstallDescriptor loadToolVersionInstallDescriptor(String toolName, String version) {
        try {
            String contents = FcliHomeHelper.readFile(getInstallDescriptorPath(toolName, version), false);
            return StringUtils.isBlank(contents) ? null : jsonObjectMapper.readValue(contents, ToolVersionInstallDescriptor.class);
        } catch ( IOException e ) {
            throw new RuntimeException("Error reading installed tool data", e);
        }
    }
    
    public static final ToolVersionCombinedDescriptor loadToolVersionCombinedDescriptor(String toolName, String version) {
        ToolVersionInstallDescriptor installDescriptor = loadToolVersionInstallDescriptor(toolName, version);
        return installDescriptor==null ? null : new ToolVersionCombinedDescriptor(toolName, null, installDescriptor);
    }
    
    public static final void deleteToolVersionInstallDescriptor(String toolName, String version) {
        try {
            FcliHomeHelper.deleteFile(getInstallDescriptorPath(toolName, version));
        } catch ( IOException e ) {
            throw new RuntimeException("Error reading installed tool data", e);
        }
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
        return Paths.get("tools", toolName, version);
    }
}
