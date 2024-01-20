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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.FileUtils;

public final class ToolHelper {
    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    private static final String internalToolDefinitionsLocation = "com/fortify/cli/tool/config/tool-definitions.yaml.zip";
    private static final File configuredToolDefinitionsFile = FcliDataHelper.getFcliConfigPath().resolve("tool/tool-definitions.yaml.zip").toFile();
    
    public static final ToolDefinitionRootDescriptor getToolDefinitionRootDescriptor(String toolName) {
        String yamlFileName = toolName + ".yaml";
        try ( InputStream is = getToolDefinitionsInputStream(); ZipInputStream zis = new ZipInputStream(is) ) {
            ZipEntry entry;
            while ( (entry = zis.getNextEntry())!=null ) {
                if ( yamlFileName.equals(entry.getName()) ) {
                    return yamlObjectMapper.readValue(zis, ToolDefinitionRootDescriptor.class);
                }
            }
            throw new IllegalStateException("No tool definitions found for "+toolName);
        } catch (IOException e) {
            throw new RuntimeException("Error loading tool definitions", e);
        }
    }
    
    public static final void saveToolInstallationDescriptor(String toolName, ToolDefinitionVersionDescriptor versionDescriptor, ToolInstallationDescriptor installationDescriptor) {
        FcliDataHelper.saveFile(getInstallDescriptorPath(toolName, versionDescriptor.getVersion()), installationDescriptor, true);
    }
    
    public static final ToolInstallationDescriptor loadToolInstallationDescriptor(String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        return FcliDataHelper.readFile(getInstallDescriptorPath(toolName, versionDescriptor.getVersion()), ToolInstallationDescriptor.class, false);
    }
    
    public static final void deleteToolInstallDescriptor(String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        FcliDataHelper.deleteFile(getInstallDescriptorPath(toolName, versionDescriptor.getVersion()), true);
    }
    
    public static final String getResourceDir(String toolName) {
        return String.format("com/fortify/cli/tool/%s", toolName);
    }
    
    public static final String getResourceFile(String toolName, String fileName) {
        return String.format("%s/%s", getResourceDir(toolName.replace('-', '_')), fileName);
    }
    
    private static final Path getInstallDescriptorPath(String toolName, String version) {
        return FcliDataHelper.getFcliStatePath().resolve("tools").resolve(toolName).resolve(version);
    }
    
    private static final InputStream getToolDefinitionsInputStream() throws FileNotFoundException {
        return configuredToolDefinitionsFile.exists() 
                ? new FileInputStream(configuredToolDefinitionsFile) 
                : FileUtils.getResourceInputStream(internalToolDefinitionsLocation);
    }
    
    /*
    private static final void initializeZipBundle() throws IOException {
        if(!toolDefinitions.toFile().exists()) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String resourceFile = "com/fortify/cli/tool/config/tool-definitions.yaml.zip";
            try ( InputStream stream = classLoader.getResourceAsStream(resourceFile) ) { 
                if(!FcliDataHelper.getFcliConfigPath().resolve("tool").toFile().exists()) {
                    Files.createDirectories(FcliDataHelper.getFcliConfigPath().resolve("tool"));
                }
                Files.copy(stream, toolDefinitions, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw e;
            }
        }
    }
    
    private static final ToolDefinitionRootDescriptor loadDescriptorFromZipBundle(String toolName) throws IOException {
        ZipFile bundle = new ZipFile(toolDefinitions.toString());
        Enumeration<? extends ZipEntry> entries = bundle.entries();

        while(entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if(entry.getName().equals(String.format("%s.yaml", toolName))) {

                try (InputStream file = bundle.getInputStream(entry)) {
                    return yamlObjectMapper.readValue(file, ToolDefinitionRootDescriptor.class);
                } catch (IOException e) {
                    throw e;
                }
            }
        }
        throw new FileNotFoundException(String.format("%s.yaml", toolName));
    }
    */
}
