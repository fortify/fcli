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
package com.fortify.cli.tool._common.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.util.FcliDataHelper;

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
        FcliDataHelper.saveFile(getInstallDescriptorPath(toolName, downloadDescriptor.getVersion()), installDescriptor, true);
        return new ToolVersionCombinedDescriptor(toolName, downloadDescriptor, installDescriptor);
    }
    
    public static final ToolVersionInstallDescriptor loadToolVersionInstallDescriptor(String toolName, String version) {
        return FcliDataHelper.readFile(getInstallDescriptorPath(toolName, version), ToolVersionInstallDescriptor.class, false);
    }
    
    public static final ToolVersionCombinedDescriptor loadToolVersionCombinedDescriptor(String toolName, String version) {
        version = getToolDownloadDescriptor(toolName).getVersionOrDefault(version).getVersion();
        ToolVersionInstallDescriptor installDescriptor = loadToolVersionInstallDescriptor(toolName, version);
        return installDescriptor==null ? null : new ToolVersionCombinedDescriptor(toolName, getToolDownloadDescriptor(toolName).getVersion(version), installDescriptor);
    }
    
    public static final void deleteToolVersionInstallDescriptor(String toolName, String version) {
        FcliDataHelper.deleteFile(getInstallDescriptorPath(toolName, version), true);
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
        return FcliDataHelper.getFcliStatePath().resolve("tools").resolve(toolName).resolve(version);
    }
}
