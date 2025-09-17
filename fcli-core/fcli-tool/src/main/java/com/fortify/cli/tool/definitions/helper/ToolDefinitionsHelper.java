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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.tool._common.helper.ToolRegistry;

import lombok.SneakyThrows;

public final class ToolDefinitionsHelper {
    private static final String ZIP_FILE_NAME = "tool-definitions.yaml.zip";
    public static final Path DEFINITIONS_STATE_DIR = FcliDataHelper.getFcliStatePath().resolve("tool");
    public static final Path DEFINITIONS_STATE_ZIP = DEFINITIONS_STATE_DIR.resolve(ZIP_FILE_NAME);
    private static final String DEFINITIONS_INTERNAL_ZIP = "com/fortify/cli/tool/config/"+ZIP_FILE_NAME;
    private static final Path DESCRIPTOR_PATH = ToolDefinitionsHelper.DEFINITIONS_STATE_DIR.resolve("state.json");
    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
    private static String toolDefinitionCustomFilePath;
    private static final Map<String, Boolean> yamlUpdateMap = new HashMap<>();

    public static final List<ToolDefinitionsOutputDescriptor> getOutputDescriptors() {
        List<ToolDefinitionsOutputDescriptor> result = new ArrayList<>();
        addZipOutputDescriptor(result);
        addYamlOutputDescriptors(result);
        return result;
    }
    
    @SneakyThrows
    public static final List<ToolDefinitionsOutputDescriptor> updateToolDefinitions(String source) {
        toolDefinitionCustomFilePath=source;
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
            if(!source.toLowerCase().endsWith(".zip")) {
                throw new FcliSimpleException("Invalid Tools definitions file");
            }
            mergeDefinitionsZip(dest);
        }
        return new ToolDefinitionsStateDescriptor(source, new Date(getModifiedTime(dest).toMillis()));
    }

    @SneakyThrows
    private static void mergeDefinitionsZip(Path dest) {
        Set<String> registeredToolNames = ToolRegistry.getRegisteredToolNames();
        Set<String> requiredYamlFiles = registeredToolNames.stream().map(toolName -> toolName + ".yaml")
                .collect(Collectors.toSet());

        Map<String, byte[]> mergedFiles = new HashMap<>();
        Map<String, FileTime> fileTimeMap = new HashMap<>();

        Map<String, ZipEntry> userZipMap = new HashMap<>();
        if (toolDefinitionCustomFilePath != null && Files.exists(Paths.get(toolDefinitionCustomFilePath))) {
            updateToolDefinitionsFromUserZip(requiredYamlFiles, mergedFiles, fileTimeMap, userZipMap);
        }

        Map<String, ZipEntry> destZipMap = new HashMap<>();
        if (Files.exists(dest)) {
            updateToolDefinitionsDestZip(dest, requiredYamlFiles, mergedFiles, fileTimeMap, destZipMap);
        }

        updateToolDefinitionsFromDefault(requiredYamlFiles, mergedFiles, fileTimeMap);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(dest))) {
            for (Map.Entry<String, byte[]> file : mergedFiles.entrySet()) {
                ZipEntry outEntry = new ZipEntry(file.getKey());
                FileTime origTime = fileTimeMap.get(file.getKey());
                if (origTime != null) {
                    outEntry.setLastModifiedTime(origTime);
                }
                zos.putNextEntry(outEntry);
                zos.write(file.getValue());
                zos.closeEntry();
            }
        }
    }

    private static void updateToolDefinitionsFromDefault(Set<String> requiredYamlFiles, Map<String, byte[]> mergedFiles,
            Map<String, FileTime> fileTimeMap) throws IOException {
        try (InputStream is = FileUtils.getResourceInputStream(DEFINITIONS_INTERNAL_ZIP);
                ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry jarEntry;
            while ((jarEntry = zis.getNextEntry()) != null) {
                if (!jarEntry.isDirectory()) {
                    String fileName = Path.of(jarEntry.getName()).getFileName().toString();
                    if (requiredYamlFiles.contains(fileName) && !mergedFiles.containsKey(fileName)) {
                        mergedFiles.put(fileName, zis.readAllBytes());
                        // Use jarEntry time, but fallback to FileTime.fromMillis(0) if unavailable
                        FileTime ft = jarEntry.getLastModifiedTime();
                        fileTimeMap.put(fileName, (ft != null) ? ft : FileTime.fromMillis(0));
                        yamlUpdateMap.put(fileName, false); // from jar
                    }
                }
            }
        }
    }

    private static void updateToolDefinitionsDestZip(Path dest, Set<String> requiredYamlFiles,
            Map<String, byte[]> mergedFiles, Map<String, FileTime> fileTimeMap, Map<String, ZipEntry> destZipMap)
            throws IOException, ZipException {
        try (ZipFile destZip = new ZipFile(dest.toFile())) {
            Enumeration<? extends ZipEntry> entries = destZip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String name = Path.of(entry.getName()).getFileName().toString();
                    destZipMap.put(name, entry);
                }
            }
            for (String requiredFile : requiredYamlFiles) {
                if (!mergedFiles.containsKey(requiredFile)) {
                    ZipEntry destEntry = destZipMap.get(requiredFile);
                    if (destEntry != null) {
                        try (InputStream in = destZip.getInputStream(destEntry)) {
                            mergedFiles.put(requiredFile, in.readAllBytes());
                        }
                        fileTimeMap.put(requiredFile, destEntry.getLastModifiedTime());
                        yamlUpdateMap.put(requiredFile, false);
                    }
                }
            }
        }
    }

    private static void updateToolDefinitionsFromUserZip(Set<String> requiredYamlFiles, Map<String, byte[]> mergedFiles,
            Map<String, FileTime> fileTimeMap, Map<String, ZipEntry> userZipMap) throws IOException {
        try (ZipFile userZip = new ZipFile(toolDefinitionCustomFilePath)) {
            Enumeration<? extends ZipEntry> entries = userZip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String name = Path.of(entry.getName()).getFileName().toString();
                    userZipMap.put(name, entry);
                }
            }
            for (String requiredFile : requiredYamlFiles) {
                ZipEntry userEntry = userZipMap.get(requiredFile);
                if (userEntry != null) {
                    try (InputStream in = userZip.getInputStream(userEntry)) {
                        mergedFiles.put(requiredFile, in.readAllBytes());
                    }
                    fileTimeMap.put(requiredFile, userEntry.getLastModifiedTime());
                    yamlUpdateMap.put(requiredFile, true);
                }
            }
        }
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
            throw new FcliSimpleException("No tool definitions found for "+toolName);
        } catch (IOException e) {
            throw new FcliSimpleException("Error loading tool definitions", e);
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
            result.add(new ToolDefinitionsOutputDescriptor(ZIP_FILE_NAME, "INTERNAL", FcliBuildProperties.INSTANCE.getFcliBuildDate(), "UPDATED"));
        }
    }

    private static final void addYamlOutputDescriptors(List<ToolDefinitionsOutputDescriptor> result) {
        if (isUpdateDefault()) {
            try (InputStream is = getToolDefinitionsInputStream(); ZipInputStream zis = new ZipInputStream(is)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    var name = Path.of(entry.getName()).getFileName().toString();
                    var source = ZIP_FILE_NAME;
                    var lastModified = new Date(entry.getLastModifiedTime().toMillis());
                    result.add(new ToolDefinitionsOutputDescriptor(name, source, lastModified, "UPDATED"));
                }
            } catch (IOException e) {
                throw new FcliSimpleException("Error loading tool definitions", e);
            }
        } else {
            try (InputStream is = getToolDefinitionsInputStream(); ZipInputStream zis = new ZipInputStream(is)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = Path.of(entry.getName()).getFileName().toString();
                    if (name.endsWith(".yaml")) {
                        String source = ZIP_FILE_NAME;
                        Date lastModified = new Date(entry.getLastModifiedTime().toMillis());

                        String action = yamlUpdateMap.getOrDefault(name, false) ? "UPDATED" : "NOT_PRESENT";
                        result.add(new ToolDefinitionsOutputDescriptor(name, source, lastModified, action));
                    }
                }
            } catch (IOException e) {
                throw new FcliSimpleException("Error loading tool definitions", e);
            }

            if (toolDefinitionCustomFilePath != null) {
            try (ZipFile userZip = new ZipFile(toolDefinitionCustomFilePath)) {
                Enumeration<? extends ZipEntry> entries = userZip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        String name = Path.of(entry.getName()).getFileName().toString();
                        if (!name.endsWith(".yaml")) {
                            Date lastModified = new Date(entry.getLastModifiedTime().toMillis());
                            result.add(new ToolDefinitionsOutputDescriptor(name, toolDefinitionCustomFilePath, lastModified, "IGNORED"));
                        }
                    }
                }
            } catch (IOException e) {
                throw new FcliSimpleException("Error loading non-yaml files from user definitions", e);
            }
        }
    }
    }

    private static boolean isUpdateDefault() {
        return toolDefinitionCustomFilePath.contains("https://");
    }
}
