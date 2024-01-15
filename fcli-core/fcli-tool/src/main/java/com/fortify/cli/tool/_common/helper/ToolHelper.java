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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.util.FcliDataHelper;

public final class ToolHelper {
    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    private static final Path toolversionsBundle = FcliDataHelper.getFcliConfigPath().resolve("tool/tool-definitions.yaml.zip");
    
    public static final ToolDefinitionsDescriptor getToolDownloadDescriptor(String toolName) {
        try {
            initializeZipBundle();
            return loadDescriptorFromZipBundle(toolName);
        } catch (IOException e) {
            throw new RuntimeException("Error loading tool definitions", e);
        }
    }
    
    public static final ToolVersionCombinedDescriptor saveToolVersionInstallDescriptor(String toolName, ToolVersionInstallDescriptor installDescriptor) {
        ToolVersionDescriptor downloadDescriptor = installDescriptor.getOriginalDownloadDescriptor();
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
        return String.format("%s/%s", getResourceDir(toolName.replace('-', '_')), fileName);
    }
    
    private static final void initializeZipBundle() throws IOException {
        if(!toolversionsBundle.toFile().exists()) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String resourceFile = "com/fortify/cli/tool/tool-definitions.yaml.zip";
            try ( InputStream stream = classLoader.getResourceAsStream(resourceFile) ) { 
                if(!FcliDataHelper.getFcliConfigPath().resolve("tool").toFile().exists()) {
                    Files.createDirectories(FcliDataHelper.getFcliConfigPath().resolve("tool"));
                }
                Files.copy(stream, toolversionsBundle, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw e;
            }
        }
    }
    
    private static final Path getInstallDescriptorPath(String toolName, String version) {
        return FcliDataHelper.getFcliStatePath().resolve("tools").resolve(toolName).resolve(version);
    }
    
    private static final ToolDefinitionsDescriptor loadDescriptorFromZipBundle(String toolName) throws IOException {
        ZipFile bundle = new ZipFile(toolversionsBundle.toString());
        Enumeration<? extends ZipEntry> entries = bundle.entries();

        while(entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if(entry.getName().equals(String.format("%s.yaml", toolName))) {

                try (InputStream file = bundle.getInputStream(entry)) {
                    return yamlObjectMapper.readValue(file, ToolDefinitionsDescriptor.class);
                } catch (IOException e) {
                    throw e;
                }
            }
        }
        throw new FileNotFoundException(String.format("%s.yaml", toolName));
    }
}
