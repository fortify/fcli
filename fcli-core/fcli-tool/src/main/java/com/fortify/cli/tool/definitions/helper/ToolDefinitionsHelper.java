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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.FileUtils;

import lombok.SneakyThrows;

public final class ToolDefinitionsHelper {
    private static final String ZIP_FILE_NAME = "tool-definitions.yaml.zip";
    public static final Path DEFINITIONS_STATE_DIR = FcliDataHelper.getFcliStatePath().resolve("tool");
    public static final Path DEFINITIONS_STATE_ZIP = DEFINITIONS_STATE_DIR.resolve(ZIP_FILE_NAME);
    private static final String DEFINITIONS_INTERNAL_ZIP = "com/fortify/cli/tool/config/"+ZIP_FILE_NAME;
    private static final Path DESCRIPTOR_PATH = ToolDefinitionsHelper.DEFINITIONS_STATE_DIR.resolve("state.json");
    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    
    public static final List<ToolDefinitionsOutputDescriptor> getOutputDescriptors() {
        List<ToolDefinitionsOutputDescriptor> result = new ArrayList<>();
        addZipOutputDescriptor(result);
        addYamlOutputDescriptors(result);
        return result;
    }
    
    @SneakyThrows
    public static final List<ToolDefinitionsOutputDescriptor> updateToolDefinitions(String source) {
        createDefinitionsStateDir(ToolDefinitionsHelper.DEFINITIONS_STATE_DIR);
        var zip = ToolDefinitionsHelper.DEFINITIONS_STATE_ZIP;
        var descriptor = update(source, zip);
        FcliDataHelper.saveFile(DESCRIPTOR_PATH, descriptor, true);
        return getOutputDescriptors();
    }
    
    @SneakyThrows
    public static final List<ToolDefinitionsOutputDescriptor> reset() {
        if ( Files.exists(DEFINITIONS_STATE_ZIP) ) {
            Files.delete(DEFINITIONS_STATE_ZIP);
            FcliDataHelper.deleteFile(DESCRIPTOR_PATH, false);
        }
        return getOutputDescriptors();
    }
    
    private static final void createDefinitionsStateDir(Path dir) throws IOException {
        if( !Files.exists(dir) ) {
            Files.createDirectories(dir);
        }
    }

    private static FileTime getModifiedTime(Path path) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        return attr.lastModifiedTime();
    }
    
    private static final ToolDefinitionsStateDescriptor update(String source, Path dest) throws IOException {
        try {
            UnirestHelper.download("tool", new URL(source).toString(), dest.toFile());
        } catch ( MalformedURLException e ) {
            Files.copy(Path.of(source), dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return new ToolDefinitionsStateDescriptor(source, new Date(getModifiedTime(dest).toMillis()));
    }
    
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
    
    private static final void addZipOutputDescriptor(List<ToolDefinitionsOutputDescriptor> result) {
        var stateDescriptor = FcliDataHelper.readFile(DESCRIPTOR_PATH, ToolDefinitionsStateDescriptor.class, false);
        if ( stateDescriptor!=null ) {
            result.add(new ToolDefinitionsOutputDescriptor(ZIP_FILE_NAME, stateDescriptor));
        } else {
            result.add(new ToolDefinitionsOutputDescriptor(ZIP_FILE_NAME, "INTERNAL", FcliBuildPropertiesHelper.getFcliBuildDate()));
        }
    }
    
    private static final void addYamlOutputDescriptors(List<ToolDefinitionsOutputDescriptor> result) {
        try ( InputStream is = getToolDefinitionsInputStream(); ZipInputStream zis = new ZipInputStream(is) ) {
            ZipEntry entry;
            while ( (entry = zis.getNextEntry())!=null ) {
                var name = Path.of(entry.getName()).getFileName().toString(); // Should already be just a file name, but just in case
                var source = ZIP_FILE_NAME;
                var lastModified = new Date(entry.getLastModifiedTime().toMillis());
                result.add(new ToolDefinitionsOutputDescriptor(name, source, lastModified));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading tool definitions", e);
        }
    }
    
}
