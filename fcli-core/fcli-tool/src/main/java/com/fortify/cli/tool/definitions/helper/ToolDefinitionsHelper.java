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
package com.fortify.cli.tool.definitions.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.FileUtils;

public final class ToolDefinitionsHelper {
    public static final Path DEFINITIONS_STATE_DIR = FcliDataHelper.getFcliStatePath().resolve("tool");
    public static final Path DEFINITIONS_STATE_ZIP = DEFINITIONS_STATE_DIR.resolve("tool-definitions.yaml.zip");
    private static final String DEFINITIONS_INTERNAL_ZIP = "com/fortify/cli/tool/config/tool-definitions.yaml.zip";
    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    
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
    
    private static final InputStream getToolDefinitionsInputStream() throws IOException {
        return Files.exists(DEFINITIONS_STATE_ZIP) 
                ? Files.newInputStream(DEFINITIONS_STATE_ZIP) 
                : FileUtils.getResourceInputStream(DEFINITIONS_INTERNAL_ZIP);
    }
}
