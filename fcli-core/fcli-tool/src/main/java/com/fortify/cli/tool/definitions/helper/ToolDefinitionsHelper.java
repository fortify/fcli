/*
 * Copyright 2021-2026 Open Text.
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
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.FileUtils;

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
     * 
     * @return List of tool definitions output descriptors
     */
    public static final List<ToolDefinitionsOutputDescriptor> listToolDefinitions() {
        List<ToolDefinitionsOutputDescriptor> result = new ArrayList<>();
        addZipOutputDescriptor(result);
        addYamlOutputDescriptors(result);
        return result;
    }

    /**
     * Update tool definitions from the specified source if needed based on
     * forceUpdate and maxAge.
     * 
     * @param source      Tool definitions source zip URL or file path; if null or
     *                    blank, default URL is used
     * @param forceUpdate If true, always update regardless of age
     * @param maxAge      Optional max age string (e.g., "4h", "1d"); if null,
     *                    default max age of 6 hours is used
     * @return
     */
    @SneakyThrows
    public static final List<ToolDefinitionsOutputDescriptor> updateToolDefinitions(String source, boolean forceUpdate,
            String maxAge) {
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
     * 
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
        String envValue = EnvHelper.env("TOOL_DEFINITIONS");
        return StringUtils.isNotBlank(envValue) ? envValue : DEFAULT_TOOL_DEFINITIONS_URL;
    }

    private static final void createDefinitionsStateDir(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    private static final FileTime getModifiedTime(Path path) throws IOException {
        if (!Files.exists(path))
            return null;
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
            var url = new URL(source);
            Path tempFile = Files.createTempFile("tool-definitions-", ".zip");
            try {
                UnirestHelper.download("tool", url.toString(), tempFile.toFile());
                mergeDefinitionsZip(dest, tempFile.toString());
            } finally {
                Files.deleteIfExists(tempFile);
            }
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
     * Validates that a local file is a valid ZIP file containing at least one
     * expected tool definition YAML.
     * <p>
     * The merge logic will handle missing required files by falling back to state
     * directory
     * or internal resources, and will ignore any unknown files in the ZIP.
     * 
     * @param source the file path to validate
     * @return true if the file exists, is a valid ZIP, and contains at least one
     *         required YAML file
     * @throws FcliSimpleException if an I/O error occurs while reading the ZIP file
     */
    private static boolean isValidZip(String source) {
        Path zipPath = Path.of(source);
        if (!Files.exists(zipPath)) {
            return false;
        }
        Set<String> requiredFiles = getRequiredFileNames();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String name = Path.of(entry.getName()).getFileName().toString();
                    if (requiredFiles.contains(name)) {
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
     * Merges tool definition YAML files from multiple sources into a single
     * destination ZIP file.
     * <p>
     * This method searches for required tool definition YAML files in the following
     * priority order:
     * <ol>
     * <li>User-specified ZIP file (source parameter)</li>
     * <li>Existing state directory ZIP file</li>
     * <li>Internal resource ZIP file embedded in the fcli JAR</li>
     * </ol>
     * For each required YAML file, the first location where it's found is used.
     * This allows users
     * to override specific tool definitions while falling back to previously
     * downloaded or built-in
     * definitions for tools they haven't customized.
     * 
     * @param dest   the destination ZIP file path where merged definitions will be
     *               written
     * @param source the user-specified source ZIP file path, or null to use only
     *               state/internal sources
     * @throws FcliSimpleException if the user-provided ZIP doesn't contain any
     *                             required YAML files,
     *                             or if I/O errors occur during processing
     */
    @SneakyThrows
    private static void mergeDefinitionsZip(Path dest, String source) {
        if (StringUtils.isNotBlank(source)) {
            validateUserZipContainsRequiredFiles(source);
        }

        createDefinitionsStateDir(DEFINITIONS_STATE_DIR);

        // If dest already exists and we're about to overwrite it, move it to temp
        // location
        // so we can use it as a fallback source
        Path existingStateZip = null;
        if (Files.exists(dest) && dest.equals(DEFINITIONS_STATE_ZIP)) {
            existingStateZip = DEFINITIONS_STATE_DIR.resolve(".tool-definitions.yaml.zip.old");
            Files.move(dest, existingStateZip, StandardCopyOption.REPLACE_EXISTING);
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
                Files.move(existingStateZip, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            throw e;
        }
    }

    private static void validateUserZipContainsRequiredFiles(String source) throws IOException {
        Path sourcePath = Path.of(source);
        if (!Files.exists(sourcePath)) {
            throw new FcliSimpleException("ZIP file not found: " + sourcePath);
        }

        Set<String> requiredFiles = getRequiredFileNames();
        boolean foundAtLeastOne = false;

        try (ZipFile zipFile = new ZipFile(sourcePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String name = Path.of(entry.getName()).getFileName().toString();
                    if (requiredFiles.contains(name)) {
                        foundAtLeastOne = true;
                        break; // Found at least one, that's enough
                    }
                }
            }
        } catch (IOException e) {
            throw new FcliSimpleException("Invalid or corrupted ZIP file: " + sourcePath, e);
        }

        if (!foundAtLeastOne) {
            throw new FcliSimpleException(
                    "ZIP file does not contain any expected tool definition files. Expected files: "
                            + String.join(", ", requiredFiles));
        }
    }

    private static void createMergedZipFile(Path dest, String source, Path existingStateZip) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(dest))) {
            for (String fileName : getRequiredFileNames()) {
                copyFileFromFirstAvailableSource(fileName, source, existingStateZip, zos);
            }
        }
    }

    private static void copyFileFromFirstAvailableSource(String fileName, String userSource,
            Path existingStateZip, ZipOutputStream zos) throws IOException {
        // Try user-provided source first
        if (StringUtils.isNotBlank(userSource) && copyEntryFromZipToZip(Path.of(userSource), fileName, zos)) {
            return;
        }
        // Fall back to existing state ZIP (if provided)
        if (existingStateZip != null && Files.exists(existingStateZip)
                && copyEntryFromZipToZip(existingStateZip, fileName, zos)) {
            return;
        }
        // Fall back to internal resource
        copyEntryFromResourceZipToZip(DEFINITIONS_INTERNAL_ZIP, fileName, zos);
    }

    private static boolean copyEntryFromZipToZip(Path zipPath, String fileName, ZipOutputStream zos)
            throws IOException {
        if (!Files.exists(zipPath)) {
            return false;
        }
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && Path.of(entry.getName()).getFileName().toString().equals(fileName)) {
                    ZipEntry newEntry = new ZipEntry(fileName);
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

    private static boolean copyEntryFromResourceZipToZip(String resourceZip, String fileName,
            ZipOutputStream zos) throws IOException {
        try (InputStream is = FileUtils.openResourceInputStream(resourceZip);
                ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && Path.of(entry.getName()).getFileName().toString().equals(fileName)) {
                    ZipEntry newEntry = new ZipEntry(fileName);
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

    public static final Optional<ToolDefinitionRootDescriptor> tryGetToolDefinitionRootDescriptor(String toolName) {
        String yamlFileName = toolName + ".yaml";
        try (InputStream is = getToolDefinitionsInputStream(); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (yamlFileName.equals(entry.getName())) {
                    ToolDefinitionRootDescriptor descriptor = yamlObjectMapper.readValue(zis,
                            ToolDefinitionRootDescriptor.class);
                    return Optional.of(descriptor);
                }
            }
            // No matching YAML entry → no definitions for this tool
            return Optional.empty();
        } catch (IOException e) {
            throw new FcliSimpleException("Error loading tool definitions", e);
        }
    }

    /**
     * Read a non-YAML extra file from the tool-definitions zip as a string.
     * These are files placed in the extra-files/ directory of the tool-definitions
     * repo and included in the published zip alongside tool definition YAMLs.
     */
    public static final String readExtraFile(String fileName) {
        try (InputStream is = getToolDefinitionsInputStream(); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (fileName.equals(entry.getName())) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            throw new FcliSimpleException("Extra file not found in tool definitions: " + fileName);
        } catch (IOException e) {
            throw new FcliSimpleException("Error reading extra file from tool definitions: " + fileName, e);
        }
    }

    /**
     * Open an embedded zip file from tool-definitions.yaml.zip as a {@link FileSystem},
     * allowing its contents to be read using standard {@link Files} APIs. The returned
     * {@link CloseableZipFileSystem} is auto-closeable and handles all cleanup.
     * <p>
     * Uses nested zip {@link FileSystem} instances directly on the state zip —
     * no temp files needed. If the state zip doesn't exist yet, the internal
     * resource is copied to the state directory first.
     */
    public static final CloseableZipFileSystem openEmbeddedZipFileSystem(String zipFileName) {
        var stateZip = ensureStateZipExists();
        try {
            var outerFs = FileSystems.newFileSystem(stateZip);
            try {
                var innerZipPath = outerFs.getPath(zipFileName);
                if (!Files.exists(innerZipPath)) {
                    outerFs.close();
                    throw new FcliSimpleException(
                        "Embedded zip not found in tool definitions: " + zipFileName);
                }
                var innerFs = FileSystems.newFileSystem(innerZipPath);
                return new CloseableZipFileSystem(innerFs, outerFs);
            } catch (FcliSimpleException e) {
                throw e;
            } catch (Exception e) {
                try { outerFs.close(); } catch (IOException ignored) {}
                throw e;
            }
        } catch (IOException e) {
            throw new FcliSimpleException(
                "Error opening embedded zip from tool definitions: " + zipFileName, e);
        }
    }

    /**
     * An auto-closeable wrapper around a nested zip {@link FileSystem}.
     * Closing this instance closes the inner filesystem, then the outer filesystem.
     */
    public static final class CloseableZipFileSystem implements AutoCloseable {
        private final FileSystem innerFs;
        private final FileSystem outerFs;

        CloseableZipFileSystem(FileSystem innerFs, FileSystem outerFs) {
            this.innerFs = innerFs;
            this.outerFs = outerFs;
        }

        /** Get the root path of the zip file system. */
        public Path getRoot() {
            return innerFs.getPath("/");
        }

        /** Resolve a path within the zip file system. */
        public Path getPath(String path) {
            return innerFs.getPath(path);
        }

        @Override
        public void close() {
            try { innerFs.close(); } catch (Exception e) { /* ignore */ }
            try { outerFs.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    /**
     * Ensure the state zip exists on disk. If it doesn't, copy the internal
     * resource to the state directory so all downstream code can work with
     * a file on disk.
     */
    @SneakyThrows
    private static Path ensureStateZipExists() {
        if (!Files.exists(DEFINITIONS_STATE_ZIP)) {
            createDefinitionsStateDir(DEFINITIONS_STATE_DIR);
            try (InputStream is = FileUtils.openResourceInputStream(DEFINITIONS_INTERNAL_ZIP)) {
                Files.copy(is, DEFINITIONS_STATE_ZIP);
            }
            // Set epoch timestamp so the age check treats this as stale and triggers
            // a real update on the next 'tool definitions update' invocation.
            Files.setLastModifiedTime(DEFINITIONS_STATE_ZIP, FileTime.fromMillis(0));
        }
        return DEFINITIONS_STATE_ZIP;
    }

    private static final InputStream getToolDefinitionsInputStream() throws IOException {
        ensureStateZipExists();
        return Files.newInputStream(DEFINITIONS_STATE_ZIP);
    }

    private static final void addZipOutputDescriptor(List<ToolDefinitionsOutputDescriptor> result) {
        addZipOutputDescriptor(result, true);
    }

    private static final void addZipOutputDescriptor(List<ToolDefinitionsOutputDescriptor> result,
            boolean shouldUpdate) {
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

    private static Set<String> getRequiredFileNames() {
        Set<String> names = new HashSet<>();
        try (InputStream is = FileUtils.openResourceInputStream(DEFINITIONS_INTERNAL_ZIP);
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    names.add(Path.of(entry.getName()).getFileName().toString());
                }
            }
        } catch (IOException e) {
            throw new FcliSimpleException("Error reading internal tool definitions", e);
        }
        return names;
    }

    private static final void addYamlOutputDescriptors(List<ToolDefinitionsOutputDescriptor> result) {
        try (InputStream is = getToolDefinitionsInputStream(); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = Path.of(entry.getName()).getFileName().toString();
                result.add(
                        new ToolDefinitionsOutputDescriptor(name, ZIP_FILE_NAME, getEntryLastModified(entry), "RESET"));
            }
        } catch (IOException e) {
            throw new FcliSimpleException("Error loading tool definitions", e);
        }
    }

    private static final void addYamlOutputDescriptors(List<ToolDefinitionsOutputDescriptor> result, String source,
            boolean shouldUpdate) {
        Set<String> requiredNames = getRequiredFileNames();
        if (!shouldUpdate) {
            addYamlDescriptor(result, requiredNames, "SKIPPED_BY_AGE");
        } else if (source != null && source.contains("https://")) {
            addYamlDescriptor(result, requiredNames, "UPDATED");
        } else {
            Set<String> foundNames = new HashSet<>();
            String zipPathOnly = source != null
                    ? Path.of(source).getFileName().toString()
                    : null;
            if (source != null) {
                updateActionResultForUserFile(result, requiredNames, foundNames, zipPathOnly, source);
            }

            updateActionResultForMissingFiles(result, requiredNames, foundNames);
        }
    }

    private static void updateActionResultForUserFile(List<ToolDefinitionsOutputDescriptor> result,
            Set<String> requiredNames, Set<String> foundNames, String zipPathOnly, String source) {
        Path zipPath = Path.of(source);

        if (!Files.exists(zipPath)) {
            throw new FcliSimpleException("ZIP file not found: " + zipPath);
        }
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    processUserZipEntry(entry, result, requiredNames, foundNames, zipPathOnly);
                }
            }
        } catch (IOException e) {
            throw new FcliSimpleException("Error loading files from user ZIP: " + zipPath, e);
        }
    }

    private static void processUserZipEntry(ZipEntry entry, List<ToolDefinitionsOutputDescriptor> result,
            Set<String> requiredNames, Set<String> foundNames, String zipPathOnly) {
        String name = Path.of(entry.getName()).getFileName().toString();
        Date lastModified = getEntryLastModified(entry);

        if (requiredNames.contains(name)) {
            result.add(new ToolDefinitionsOutputDescriptor(name, zipPathOnly, lastModified, "UPDATED"));
            foundNames.add(name);
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
            List<ToolDefinitionsOutputDescriptor> result, Set<String> requiredNames, Set<String> foundNames) {
        for (String required : requiredNames) {
            if (!foundNames.contains(required)) {
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
            Set<String> requiredNames, String action) {
        try (InputStream is = getToolDefinitionsInputStream(); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = Path.of(entry.getName()).getFileName().toString();
                if (requiredNames.contains(name)) {
                    result.add(new ToolDefinitionsOutputDescriptor(name, ZIP_FILE_NAME, getEntryLastModified(entry),
                            action));
                }
            }
        } catch (IOException e) {
            throw new FcliSimpleException("Error loading tool definitions", e);
        }
    }

    private static Date getInternalResourceZipEntryLastModified(String fileName) {
        try (InputStream is = FileUtils.openResourceInputStream(DEFINITIONS_INTERNAL_ZIP);
                ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(fileName)) {
                    return entry.getLastModifiedTime() != null ? new Date(entry.getLastModifiedTime().toMillis())
                            : null;
                }
            }
        } catch (IOException e) {
            throw new FcliSimpleException("Error reading internal resource zip entry for: " + fileName, e);
        }
        return null;
    }

    /**
     * Determines whether tool definitions should be updated based on force flag or
     * age.
     * <p>
     * If force is true, always returns true. If maxAge is specified, checks if
     * current
     * definitions are older than that age. Otherwise, uses default age of 6 hours.
     * 
     * @param forceUpdate if true, always update regardless of age
     * @param maxAge      optional max age string (e.g., "4h", "1d"), or null to use
     *                    default
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
     * Supported format examples: "1d" (1 day), "4h" (4 hours), "30m" (30 minutes),
     * "1d4h" (1 day 4 hours).
     * Seconds are explicitly not supported to avoid confusion with "6h" default.
     * 
     * @param duration the duration string to parse
     * @return the duration in milliseconds
     * @throws FcliSimpleException if the format is invalid or contains unsupported
     *                             units
     */
    private static long parseDurationToMillis(String duration) {
        try {
            // Use restricted period helper that only allows days, hours, minutes
            var helper = DateTimePeriodHelper.byRange(Period.MINUTES, Period.DAYS);
            return helper.parsePeriodToMillis(duration);
        } catch (IllegalArgumentException e) {
            throw new FcliSimpleException(
                    "Invalid duration format: " + duration + ". Use only d (days), h (hours), m (minutes)", e);
        }
    }

}
