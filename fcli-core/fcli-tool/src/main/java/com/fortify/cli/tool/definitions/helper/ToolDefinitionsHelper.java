/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.tool.definitions.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool._common.helper.ToolDependency;

import lombok.SneakyThrows;

public final class ToolDefinitionsHelper {
    private static final long DEFAULT_UPDATE_AGE_HOURS = 6L;
    private static final String ZIP_FILE_NAME = "tool-definitions.yaml.zip";
    public static final Path DEFINITIONS_STATE_DIR = FcliDataHelper.getFcliStatePath().resolve("tool");
    public static final Path DEFINITIONS_STATE_ZIP = DEFINITIONS_STATE_DIR.resolve(ZIP_FILE_NAME);
    public static final String DEFAULT_TOOL_DEFINITIONS_URL = "https://github.com/fortify/tool-definitions/releases/download/v1/tool-definitions.yaml.zip";
    private static final String DEFINITIONS_INTERNAL_ZIP = "com/fortify/cli/tool/config/" + ZIP_FILE_NAME;
    private static final Path DESCRIPTOR_PATH = ToolDefinitionsHelper.DEFINITIONS_STATE_DIR.resolve("state.json");
    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());

    /**
     * List current tool definitions.
     * @return List of tool definitions output descriptors
     */
    public static final List<ToolDefinitionsOutputDescriptor> listToolDefinitions() {
        List<ToolDefinitionsOutputDescriptor> result = new ArrayList<>();
        addZipOutputDescriptor(result);
        addYamlOutputDescriptors(result);
        return result;
    }
    
    /**
     * Update tool definitions from the specified source if needed based on forceUpdate and maxAge.
     * @param source Tool definitions source zip URL or file path; if null or blank, default URL is used
     * @param forceUpdate If true, always update regardless of age
     * @param maxAge Optional max age string (e.g., "4h", "1d"); if null, default max age of 6 hours is used
     * @return
     */
    @SneakyThrows
    public static final List<ToolDefinitionsOutputDescriptor> updateToolDefinitions(String source, boolean forceUpdate, String maxAge) {
        String normalizedSource = normalizeSource(source);
        boolean shouldUpdate = shouldUpdateToolDefinitions(forceUpdate, maxAge);
        if (shouldUpdate) {
            createDefinitionsStateDir(ToolDefinitionsHelper.DEFINITIONS_STATE_DIR);
            var zip = ToolDefinitionsHelper.DEFINITIONS_STATE_ZIP;
            var descriptor = update(normalizedSource, zip);
            FcliDataHelper.saveFile(DESCRIPTOR_PATH, descriptor, true);
        }
        return getOutputDescriptors(normalizedSource, shouldUpdate);
    }

    /**
     * Reset tool definitions to internal defaults by deleting state files.
     * @return List of tool definitions output descriptors after reset
     */
    @SneakyThrows
    public static final List<ToolDefinitionsOutputDescriptor> resetToolDefinitions() {
        if (Files.exists(DEFINITIONS_STATE_ZIP)) {
            Files.delete(DEFINITIONS_STATE_ZIP);
            FcliDataHelper.deleteFile(DESCRIPTOR_PATH, false);
        }
        return listToolDefinitions();
    }

    private static final String normalizeSource(String source) {
        if (StringUtils.isNotBlank(source)) {
            return source;
        }
        String envValue = com.fortify.cli.common.util.EnvHelper.env("TOOL_DEFINITIONS");
        return StringUtils.isNotBlank(envValue) ? envValue : DEFAULT_TOOL_DEFINITIONS_URL;
    }

    private static final void createDefinitionsStateDir(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    private static final FileTime getModifiedTime(Path path) throws IOException {
        if (!Files.exists(path)) return null;
        return Files.getLastModifiedTime(path);
    }

    private static List<ToolDefinitionsOutputDescriptor> getOutputDescriptors(String source, boolean shouldUpdate) {
        List<ToolDefinitionsOutputDescriptor> result = new ArrayList<>();
        addZipOutputDescriptor(result, shouldUpdate);
        addYamlOutputDescriptors(result, source, shouldUpdate);
        return result;
    }

    private static final ToolDefinitionsStateDescriptor update(String source, Path dest) throws IOException {
        try {
            UnirestHelper.download("tool", new URL(source).toString(), dest.toFile());
        } catch (MalformedURLException e) {
            if (!isValidZip(source)) {
                throw new FcliSimpleException("Invalid tool definitions file", e);
            }
            mergeDefinitionsZip(dest, source);
        }
        FileTime modifiedTime = getModifiedTime(dest);
        if (modifiedTime == null) {
            throw new FcliSimpleException("Could not determine last modified time for: " + dest);
        }
        return new ToolDefinitionsStateDescriptor(source, new Date(modifiedTime.toMillis()));
    }

    /**
     * Validates that a local file is a valid ZIP file containing at least one expected tool definition YAML.
     * <p>
     * The merge logic will handle missing required files by falling back to state directory
     * or internal resources, and will ignore any unknown files in the ZIP.
     * 
     * @param source the file path to validate
     * @return true if the file exists, is a valid ZIP, and contains at least one required YAML file
     * @throws FcliSimpleException if an I/O error occurs while reading the ZIP file
     */
    private static boolean isValidZip(String source) {
        Path zipPath = Path.of(source);
        if (!Files.exists(zipPath)) {
            return false;
        }
        Set<String> requiredYamlFiles = getRequiredYamlFileNames();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String name = Path.of(entry.getName()).getFileName().toString();
                    if (requiredYamlFiles.contains(name)) {
                        return true; // At least one required file found
                    }
                }
            }
        } catch (IOException e) {
            throw new FcliSimpleException("Error loading user ZIP file: " + zipPath, e);
        }
        return false; // No required files found
    }


    /**
     * Merges tool definition YAML files from multiple sources into a single destination ZIP file.
     * <p>
     * This method searches for required tool definition YAML files in the following priority order:
     * <ol>
     * <li>User-specified ZIP file (source parameter)</li>
     * <li>Existing state directory ZIP file</li>
     * <li>Internal resource ZIP file embedded in the fcli JAR</li>
     * </ol>
     * For each required YAML file, the first location where it's found is used. This allows users
     * to override specific tool definitions while falling back to previously downloaded or built-in
     * definitions for tools they haven't customized.
     * 
     * @param dest the destination ZIP file path where merged definitions will be written
     * @param source the user-specified source ZIP file path, or null to use only state/internal sources
     * @throws FcliSimpleException if the user-provided ZIP doesn't contain any required YAML files,
     *         or if I/O errors occur during processing
     */
    @SneakyThrows
    private static void mergeDefinitionsZip(Path dest, String source) {
        if (StringUtils.isNotBlank(source)) {
            validateUserZipContainsRequiredFiles(source);
        }
        
        createDefinitionsStateDir(DEFINITIONS_STATE_DIR);
        
        // If dest already exists and we're about to overwrite it, move it to temp location
        // so we can use it as a fallback source
        Path existingStateZip = null;
        if (Files.exists(dest) && dest.equals(DEFINITIONS_STATE_ZIP)) {
            existingStateZip = DEFINITIONS_STATE_DIR.resolve(".tool-definitions.yaml.zip.old");
            Files.move(dest, existingStateZip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        
        try {
            createMergedZipFile(dest, source, existingStateZip);
            Files.setLastModifiedTime(dest, FileTime.fromMillis(System.currentTimeMillis()));
            
            // Clean up temp file if it exists
            if (existingStateZip != null && Files.exists(existingStateZip)) {
                Files.delete(existingStateZip);
            }
        } catch (Exception e) {
            // On error, restore the old file if we moved it
            if (existingStateZip != null && Files.exists(existingStateZip)) {
                Files.move(existingStateZip, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            throw e;
        }
    }
    
    private static void validateUserZipContainsRequiredFiles(String source) throws IOException {
        Path sourcePath = Path.of(source);
        if (!Files.exists(sourcePath)) {
            throw new FcliSimpleException("ZIP file not found: " + sourcePath);
        }
        
        Set<String> requiredYamlFiles = getRequiredYamlFileNames();
        boolean foundAtLeastOne = false;
        
        try (ZipFile zipFile = new ZipFile(sourcePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String name = Path.of(entry.getName()).getFileName().toString();
                    if (requiredYamlFiles.contains(name)) {
                        foundAtLeastOne = true;
                        break; // Found at least one, that's enough
                    }
                }
            }
        } catch (IOException e) {
            throw new FcliSimpleException("Invalid or corrupted ZIP file: " + sourcePath, e);
        }
        
        if (!foundAtLeastOne) {
            throw new FcliSimpleException("ZIP file does not contain any expected tool definition files. Expected files: " 
                + String.join(", ", requiredYamlFiles));
        }
    }
    
    private static void createMergedZipFile(Path dest, String source, Path existingStateZip) throws IOException {
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(dest))) {
            for (String yamlFileName : getRequiredYamlFileNames()) {
                copyYamlFileFromFirstAvailableSource(yamlFileName, source, existingStateZip, zos);
            }
        }
    }
    
    private static void copyYamlFileFromFirstAvailableSource(String yamlFileName, String userSource, 
            Path existingStateZip, java.util.zip.ZipOutputStream zos) throws IOException {
        // Try user-provided source first
        if (StringUtils.isNotBlank(userSource) && copyYamlFromZipToZip(Path.of(userSource), yamlFileName, zos)) {
            return;
        }
        // Fall back to existing state ZIP (if provided)
        if (existingStateZip != null && Files.exists(existingStateZip) 
                && copyYamlFromZipToZip(existingStateZip, yamlFileName, zos)) {
            return;
        }
        // Fall back to internal resource
        copyYamlFromResourceZipToZip(DEFINITIONS_INTERNAL_ZIP, yamlFileName, zos);
    }
    /**
     * Copies a specific YAML file from a ZIP file to an output ZIP stream.
     * 
     * @param zipPath the source ZIP file path
     * @param yamlFileName the name of the YAML file to copy
     * @param zos the destination ZIP output stream
     * @return true if the file was found and copied, false if not found
     * @throws IOException if an I/O error occurs during reading or writing
     */
    private static boolean copyYamlFromZipToZip(Path zipPath, String yamlFileName, java.util.zip.ZipOutputStream zos) throws IOException {
        if (!Files.exists(zipPath)) {
            return false;
        }
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && Path.of(entry.getName()).getFileName().toString().equals(yamlFileName)) {
                    ZipEntry newEntry = new ZipEntry(yamlFileName);
                    if (entry.getLastModifiedTime() != null) {
                        newEntry.setLastModifiedTime(entry.getLastModifiedTime());
                    }
                    zos.putNextEntry(newEntry);
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        is.transferTo(zos);
                    }
                    zos.closeEntry();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Copies a specific YAML file from an internal resource ZIP to an output ZIP stream.
     * 
     * @param resourceZip the resource path of the internal ZIP file
     * @param yamlFileName the name of the YAML file to copy
     * @param zos the destination ZIP output stream
     * @return true if the file was found and copied, false if not found
     * @throws IOException if an I/O error occurs during reading or writing
     */
    private static boolean copyYamlFromResourceZipToZip(String resourceZip, String yamlFileName, java.util.zip.ZipOutputStream zos) throws IOException {
        try (InputStream is = FileUtils.getResourceInputStream(resourceZip); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && Path.of(entry.getName()).getFileName().toString().equals(yamlFileName)) {
                    ZipEntry newEntry = new ZipEntry(yamlFileName);
                    if (entry.getLastModifiedTime() != null) {
                        newEntry.setLastModifiedTime(entry.getLastModifiedTime());
                    }
                    zos.putNextEntry(newEntry);
                    zis.transferTo(zos);
                    zos.closeEntry();
                    return true;
                }
                }
            }
        return false;
    }

    public static final ToolDefinitionRootDescriptor getToolDefinitionRootDescriptor(String toolName) {
        String yamlFileName = toolName + ".yaml";
        try (InputStream is = getToolDefinitionsInputStream(); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (yamlFileName.equals(entry.getName())) {
                    return yamlObjectMapper.readValue(zis, ToolDefinitionRootDescriptor.class);
                }
            }
            throw new FcliSimpleException("No tool definitions found for " + toolName);
        } catch (IOException e) {
            throw new FcliSimpleException("Error loading tool definitions", e);
        }
    }

    private static final InputStream getToolDefinitionsInputStream() throws IOException {
        return Files.exists(DEFINITIONS_STATE_ZIP) ? Files.newInputStream(DEFINITIONS_STATE_ZIP)
                : FileUtils.getResourceInputStream(DEFINITIONS_INTERNAL_ZIP);
    }

    private static final void addZipOutputDescriptor(List<ToolDefinitionsOutputDescriptor> result) {
        addZipOutputDescriptor(result, true);
    }

    private static final void addZipOutputDescriptor(List<ToolDefinitionsOutputDescriptor> result, boolean shouldUpdate) {
        var stateDescriptor = FcliDataHelper.readFile(DESCRIPTOR_PATH, ToolDefinitionsStateDescriptor.class, false);
        String actionResult = determineActionResult(stateDescriptor, shouldUpdate);
        
        if (stateDescriptor != null) {
            result.add(new ToolDefinitionsOutputDescriptor(ZIP_FILE_NAME, stateDescriptor, actionResult));
        } else {
            result.add(new ToolDefinitionsOutputDescriptor(ZIP_FILE_NAME, "INTERNAL", 
                    FcliBuildProperties.INSTANCE.getFcliBuildDate(), actionResult));
        }
    }
    
    private static String determineActionResult(ToolDefinitionsStateDescriptor stateDescriptor, boolean shouldUpdate) {
        if (stateDescriptor == null) {
            return "RESET";
        }
        return shouldUpdate ? "UPDATED" : "SKIPPED_BY_AGE";
    }

    private static Set<String> getRequiredYamlFileNames() {
        var toolNames = Stream.concat(
            Arrays.stream(Tool.values()).map(Tool::getToolName),
            Arrays.stream(ToolDependency.values()).map(ToolDependency::getToolName)
        );
        return toolNames.map(s -> s + ".yaml").collect(Collectors.toSet());
    }

    private static final void addYamlOutputDescriptors(List<ToolDefinitionsOutputDescriptor> result) {
        try (InputStream is = getToolDefinitionsInputStream(); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = Path.of(entry.getName()).getFileName().toString();
                result.add(new ToolDefinitionsOutputDescriptor(name, ZIP_FILE_NAME, getEntryLastModified(entry), "RESET"));
            }
        } catch (IOException e) {
            throw new FcliSimpleException("Error loading tool definitions", e);
        }
    }

    private static final void addYamlOutputDescriptors(List<ToolDefinitionsOutputDescriptor> result, String source,
            boolean shouldUpdate) {
        Set<String> requiredYamlNames = getRequiredYamlFileNames();
        if (!shouldUpdate) {
            addYamlDescriptor(result, requiredYamlNames, "SKIPPED_BY_AGE");
        }
        else if (source != null && source.contains("https://")) {
            addYamlDescriptor(result, requiredYamlNames, "UPDATED");
        }
        else {
            Set<String> foundYamlNames = new HashSet<>();
            String zipPathOnly = source != null
                ? Path.of(source).getFileName().toString()
                : null;
            if (source != null) {
                updateActionResultForUserFile(result, requiredYamlNames, foundYamlNames, zipPathOnly, source);
            }

            updateActionResultForMissingFiles(result, requiredYamlNames, foundYamlNames);
        }
    }

    private static void updateActionResultForUserFile(List<ToolDefinitionsOutputDescriptor> result,
            Set<String> requiredYamlNames, Set<String> foundYamlNames, String zipPathOnly, String source) {
        Path zipPath = Path.of(source);

        if (!Files.exists(zipPath)) {
            throw new FcliSimpleException("ZIP file not found: " + zipPath);
        }
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    processUserZipEntry(entry, result, requiredYamlNames, foundYamlNames, zipPathOnly);
                }
            }
        } catch (IOException e) {
            throw new FcliSimpleException("Error loading files from user ZIP: " + zipPath, e);
        }
    }
    
    private static void processUserZipEntry(ZipEntry entry, List<ToolDefinitionsOutputDescriptor> result,
            Set<String> requiredYamlNames, Set<String> foundYamlNames, String zipPathOnly) {
        String name = Path.of(entry.getName()).getFileName().toString();
        Date lastModified = getEntryLastModified(entry);
        
        if (requiredYamlNames.contains(name)) {
            result.add(new ToolDefinitionsOutputDescriptor(name, zipPathOnly, lastModified, "UPDATED"));
            foundYamlNames.add(name);
        } else {
            result.add(new ToolDefinitionsOutputDescriptor(name, zipPathOnly, lastModified, "IGNORED"));
        }
    }
    
    private static Date getEntryLastModified(ZipEntry entry) {
        return entry.getLastModifiedTime() != null 
            ? new Date(entry.getLastModifiedTime().toMillis()) 
            : null;
    }

    private static void updateActionResultForMissingFiles(
            List<ToolDefinitionsOutputDescriptor> result, Set<String> requiredYamlNames, Set<String> foundYamlNames) {
        for (String required : requiredYamlNames) {
            if (!foundYamlNames.contains(required)) {
                addMissingFileDescriptor(result, required);
            }
        }
    }
    
    private static void addMissingFileDescriptor(List<ToolDefinitionsOutputDescriptor> result, String fileName) {
        Date lastModified = getFileOrResourceLastModified(fileName);
        result.add(new ToolDefinitionsOutputDescriptor(fileName, ZIP_FILE_NAME, lastModified, "NOT_PRESENT"));
    }
    
    private static Date getFileOrResourceLastModified(String fileName) {
        Path filePath = DEFINITIONS_STATE_DIR.resolve(fileName);
        try {
            if (Files.exists(filePath)) {
                return new Date(Files.getLastModifiedTime(filePath).toMillis());
            }
            return getInternalResourceZipEntryLastModified(fileName);
        } catch (IOException e) {
            throw new FcliSimpleException("Error getting last modified time for: " + filePath, e);
        }
    }

    private static void addYamlDescriptor(List<ToolDefinitionsOutputDescriptor> result,
            Set<String> requiredYamlNames, String action) {
        try (InputStream is = getToolDefinitionsInputStream(); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = Path.of(entry.getName()).getFileName().toString();
                if (requiredYamlNames.contains(name)) {
                    result.add(new ToolDefinitionsOutputDescriptor(name, ZIP_FILE_NAME, getEntryLastModified(entry), action));
                }
            }
        } catch (IOException e) {
            throw new FcliSimpleException("Error loading tool definitions", e);
        }
    }


    private static Date getInternalResourceZipEntryLastModified(String fileName) {
        try (InputStream is = FileUtils.getResourceInputStream(DEFINITIONS_INTERNAL_ZIP);
                ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(fileName)) {
                    return entry.getLastModifiedTime() != null ? new Date(entry.getLastModifiedTime().toMillis()) : null;
                }
            }
        } catch (IOException e) {
            throw new FcliSimpleException("Error reading internal resource zip entry for: " + fileName, e);
        }
        return null;
    }

    /**
     * Determines whether tool definitions should be updated based on force flag or age.
     * <p>
     * If force is true, always returns true. If maxAge is specified, checks if current
     * definitions are older than that age. Otherwise, uses default age of 6 hours.
     * 
     * @param forceUpdate if true, always update regardless of age
     * @param maxAge optional max age string (e.g., "4h", "1d"), or null to use default
     * @return true if definitions should be updated, false otherwise
     * @throws IOException if unable to determine file modification time
     */
    private static boolean shouldUpdateToolDefinitions(boolean forceUpdate, String maxAge) throws IOException {
        if (forceUpdate) {
            return true;
        }
        if (!Files.exists(DEFINITIONS_STATE_ZIP)) {
            return true;
        }
        
        FileTime modTime = getModifiedTime(DEFINITIONS_STATE_ZIP);
        if (modTime == null) {
            throw new FcliSimpleException("Could not determine last modified time for: " + DEFINITIONS_STATE_ZIP);
        }
        
        long ageThresholdMillis = StringUtils.isNotBlank(maxAge)
            ? parseDurationToMillis(maxAge)
            : DEFAULT_UPDATE_AGE_HOURS * 60 * 60 * 1000;
        
        long now = System.currentTimeMillis();
        long age = now - modTime.toMillis();
        return age > ageThresholdMillis;
    }

    /**
     * Parses a duration string to milliseconds using only days, hours, and minutes.
     * <p>
     * Supported format examples: "1d" (1 day), "4h" (4 hours), "30m" (30 minutes), "1d4h" (1 day 4 hours).
     * Seconds are explicitly not supported to avoid confusion with "6h" default.
     * 
     * @param duration the duration string to parse
     * @return the duration in milliseconds
     * @throws FcliSimpleException if the format is invalid or contains unsupported units
     */
    private static long parseDurationToMillis(String duration) {
        try {
            // Use restricted period helper that only allows days, hours, minutes
            var helper = DateTimePeriodHelper.byRange(Period.MINUTES, Period.DAYS);
            return helper.parsePeriodToMillis(duration);
        } catch (IllegalArgumentException e) {
            throw new FcliSimpleException("Invalid duration format: " + duration + ". Use only d (days), h (hours), m (minutes)", e);
        }
    }

}
