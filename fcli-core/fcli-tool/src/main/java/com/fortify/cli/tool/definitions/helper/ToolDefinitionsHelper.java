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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

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
	private static final String DEFINITIONS_INTERNAL_ZIP = "com/fortify/cli/tool/config/" + ZIP_FILE_NAME;
	private static final Path DESCRIPTOR_PATH = ToolDefinitionsHelper.DEFINITIONS_STATE_DIR.resolve("state.json");
    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());

    public static final List<ToolDefinitionsOutputDescriptor> getOutputDescriptors() {
		List<ToolDefinitionsOutputDescriptor> result = new ArrayList<>();
		addZipOutputDescriptor(result);
		addYamlOutputDescriptors(result);
		return result;
	}
    public static final List<ToolDefinitionsOutputDescriptor> getOutputDescriptors(String source, boolean shouldUpdate) {
        List<ToolDefinitionsOutputDescriptor> result = new ArrayList<>();
        addZipOutputDescriptor(result, shouldUpdate);
        addYamlOutputDescriptors(result,source, shouldUpdate);
        return result;
    }

	@SneakyThrows
	public static final List<ToolDefinitionsOutputDescriptor> updateToolDefinitions(String source, boolean forceUpdate, String maxAge) {
        boolean shouldUpdate = shouldUpdateToolDefinitions(forceUpdate, maxAge);
        if (shouldUpdate) {
		createDefinitionsStateDir(ToolDefinitionsHelper.DEFINITIONS_STATE_DIR);
		var zip = ToolDefinitionsHelper.DEFINITIONS_STATE_ZIP;
		var descriptor = update(source, zip);
		FcliDataHelper.saveFile(DESCRIPTOR_PATH, descriptor, true);
        }
		return getOutputDescriptors(source, shouldUpdate);
	}

	@SneakyThrows
	public static final List<ToolDefinitionsOutputDescriptor> reset() {
		if (Files.exists(DEFINITIONS_STATE_ZIP)) {
			Files.delete(DEFINITIONS_STATE_ZIP);
			FcliDataHelper.deleteFile(DESCRIPTOR_PATH, false);
		}
		return getOutputDescriptors();
	}

	private static final void createDefinitionsStateDir(Path dir) throws IOException {
		if (!Files.exists(dir)) {
			Files.createDirectories(dir);
		}
	}

	private static FileTime getModifiedTime(Path path) throws IOException {
		if (!Files.exists(path)) {
			return null;
		}
		BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
		return attr.lastModifiedTime();
	}

	private static final ToolDefinitionsStateDescriptor update(String source, Path dest) throws IOException {
		try {
			UnirestHelper.download("tool", new URL(source).toString(), dest.toFile());
		} catch (MalformedURLException e) {
			if (!source.toLowerCase().endsWith(".zip") && !isValidZip(source)) {
				throw new FcliSimpleException("Invalid Tools definitions file");
			}
			mergeDefinitionsZip(dest, source);
		}
		return new ToolDefinitionsStateDescriptor(source, new Date(getModifiedTime(dest).toMillis()));
	}

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
                    return true;
                }
            }
        }
    } catch (IOException e) {
        throw new FcliSimpleException("Error Loading User ZIP file: " + zipPath);
    }
    return false;
}

    @SneakyThrows
	private static File extractYamlFromZip(InputStream is, String yamlFile) {
	    Path outFile = DEFINITIONS_STATE_DIR.resolve(yamlFile);
		try (ZipInputStream zis = new ZipInputStream(is)) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.getName().equals(yamlFile)) {
					Files.createDirectories(outFile.getParent());
					Files.copy(zis, outFile, StandardCopyOption.REPLACE_EXISTING);
                    if (entry.getLastModifiedTime() != null) {
						Files.setLastModifiedTime(outFile , entry.getLastModifiedTime());
					}
					return outFile.toFile();
				}
			}
		}
		return null;
	}

	/**
	 * This function finds the tool definition yaml files in the following order:
	 * 1. Zip file specified by user
	 * 2. Zip file in state directory
	 * 3. Internal resource zip file from the fcli jar
	 * It also extracts the required yaml files from the zip file where they are found.
	 * If a required yaml file is not found in any of the zip files, an exception is thrown.
	 * @param dest
	 */
	@SneakyThrows
    private static void mergeDefinitionsZip(Path dest, String source) {
        createDefinitionsStateDir(DEFINITIONS_STATE_DIR);
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(dest))) {
            for (String yamlFileName : getRequiredYamlFileNames()) {
                File file = null;
                if (source != null && Files.exists(Path.of(source))) {
                    try (InputStream is = Files.newInputStream(Path.of(source))) {
                        file = extractYamlFromZip(is, yamlFileName);
                        createZipEntry(zos, file);
                    }
                }

                if (file == null && Files.exists(DEFINITIONS_STATE_ZIP)) {
                    try (InputStream is = Files.newInputStream(DEFINITIONS_STATE_ZIP)) {
                        file = extractYamlFromZip(is, yamlFileName);
                        createZipEntry(zos, file);
                    }
                }

                if (file == null) {
                    try (InputStream is = FileUtils.getResourceInputStream(DEFINITIONS_INTERNAL_ZIP)) {
                        file = extractYamlFromZip(is, yamlFileName);
                        createZipEntry(zos, file);
                    }
                }
            }
        }
        FileTime currentTime = FileTime.fromMillis(System.currentTimeMillis());
        Files.setLastModifiedTime(dest, currentTime);
    }

    private static void createZipEntry(java.util.zip.ZipOutputStream zos, File file) throws IOException {
        if (file != null) {
            zos.putNextEntry(new ZipEntry(file.getName()));
            Files.copy(file.toPath(), zos);
            zos.closeEntry();
        }
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
        var stateDescriptor = FcliDataHelper.readFile(DESCRIPTOR_PATH, ToolDefinitionsStateDescriptor.class, false);
        if ( stateDescriptor!=null ) {
            result.add(new ToolDefinitionsOutputDescriptor(ZIP_FILE_NAME, stateDescriptor, "UPDATED"));
        } else {
            result.add(new ToolDefinitionsOutputDescriptor(ZIP_FILE_NAME, "INTERNAL", FcliBuildProperties.INSTANCE.getFcliBuildDate(), "RESET"));
        }
    }

    private static final void addZipOutputDescriptor(List<ToolDefinitionsOutputDescriptor> result, boolean shouldUpdate) {
	    var stateDescriptor = FcliDataHelper.readFile(DESCRIPTOR_PATH, ToolDefinitionsStateDescriptor.class, false);
	    String actionResult = shouldUpdate ? "UPDATED" : "SKIPPED_BY_AGE";
        if ( stateDescriptor!=null ) {
            result.add(new ToolDefinitionsOutputDescriptor(ZIP_FILE_NAME, stateDescriptor, actionResult));
        } else {
            result.add(new ToolDefinitionsOutputDescriptor(ZIP_FILE_NAME, "INTERNAL", FcliBuildProperties.INSTANCE.getFcliBuildDate(), actionResult));
        }
    }

    private static Set<String> getRequiredYamlFileNames() {
        return ToolRegistry.getRegisteredToolNames().stream().map(s -> s + ".yaml").collect(Collectors.toSet());
    }

	private static final void addYamlOutputDescriptors(List<ToolDefinitionsOutputDescriptor> result) {
        try ( InputStream is = getToolDefinitionsInputStream(); ZipInputStream zis = new ZipInputStream(is) ) {
            ZipEntry entry;
            while ( (entry = zis.getNextEntry())!=null ) {
                var name = Path.of(entry.getName()).getFileName().toString(); // Should already be just a file name, but just in case
                var source = ZIP_FILE_NAME;
                var lastModified = new Date(entry.getLastModifiedTime().toMillis());
                result.add(new ToolDefinitionsOutputDescriptor(name, source, lastModified, "RESET"));
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
            throw new FcliSimpleException("User ZIP file not found: " + zipPath);
        }

	    try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
	        Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory())
                    continue;

                String name = Path.of(entry.getName()).getFileName().toString();
                Date lastModified = entry.getLastModifiedTime() != null
                        ? new Date(entry.getLastModifiedTime().toMillis())
                        : null;

                if (requiredYamlNames.contains(name)) {
                    result.add(new ToolDefinitionsOutputDescriptor(name, zipPathOnly, lastModified, "UPDATED"));
                    foundYamlNames.add(name);
	            } else {
	                result.add(new ToolDefinitionsOutputDescriptor(name, zipPathOnly, lastModified, "IGNORED"));
	            }
	        }

	    } catch (IOException e) {
	        throw new FcliSimpleException("Error loading files from user ZIP: " + zipPath, e);
	    }
	}

    private static void updateActionResultForMissingFiles(
            List<ToolDefinitionsOutputDescriptor> result, Set<String> requiredYamlNames, Set<String> foundYamlNames) {
        for (String required : requiredYamlNames) {
            if (foundYamlNames.contains(required)) {
                continue;
            }

            String source = ZIP_FILE_NAME;
            Date lastModified = null;
            Path destFile = DEFINITIONS_STATE_DIR.resolve(required);
            try {
                if (Files.exists(destFile)) {
                    lastModified = new Date(Files.getLastModifiedTime(destFile).toMillis());
                } else {
                    lastModified = getInternalResourceZipEntryLastModified(required);
                }
            } catch (IOException e) {
                throw new FcliSimpleException("Error getting last modified time for: " + destFile, e);
            }
            result.add(new ToolDefinitionsOutputDescriptor(required, source, lastModified, "NOT_PRESENT"));
        }
    }

    private static void addYamlDescriptor(List<ToolDefinitionsOutputDescriptor> result,
            Set<String> requiredYamlNames, String action) {
        try (InputStream is = getToolDefinitionsInputStream(); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = Path.of(entry.getName()).getFileName().toString();
                if (requiredYamlNames.contains(name)) {
                    var source = ZIP_FILE_NAME;
                    var lastModified = new Date(entry.getLastModifiedTime().toMillis());
                    result.add(new ToolDefinitionsOutputDescriptor(name, source, lastModified, action));
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
					return new Date(entry.getLastModifiedTime().toMillis());
				}
			}
		} catch (IOException e) {
			throw new FcliSimpleException("Error reading internal resource zip entry for: " + fileName, e);
		}
		return null;
    }

    private static boolean shouldUpdateToolDefinitions(boolean forceUpdate, String maxAge) throws IOException {
        if (forceUpdate) {
            return true;
        }
        if (maxAge != null && !maxAge.isEmpty()) {
            Date threshold = parseDurationToDate(maxAge);
            if (!Files.exists(DEFINITIONS_STATE_ZIP)) {
                return true;
            }
            if (getModifiedTime(DEFINITIONS_STATE_ZIP).toMillis() < threshold.getTime()) {
                return true;
            }
            return false;
        }
        long sixHoursMillis = 6L * 60 * 60 * 1000;
        long now = System.currentTimeMillis();
        if (!Files.exists(DEFINITIONS_STATE_ZIP)) {
            return true;
        }
        long lastModified = getModifiedTime(DEFINITIONS_STATE_ZIP).toMillis();
        return (now - lastModified) > sixHoursMillis;
    }

    private static Date parseDurationToDate(String duration) {
        long now = System.currentTimeMillis();
        long millis = 0L;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)([dhm])");
        java.util.regex.Matcher matcher = pattern.matcher(duration);
        int matched = 0;
        while (matcher.find()) {
            matched++;
            int value = Integer.parseInt(matcher.group(1));
            switch (matcher.group(2)) {
            case "d":
                millis += value * 24L * 60 * 60 * 1000;
                break;
            case "h":
                millis += value * 60L * 60 * 1000;
                break;
            case "m":
                millis += value * 60L * 1000;
                break;
            }
        }
        if (duration.matches(".*\\d+s.*")) {
            throw new IllegalArgumentException(
                    "Invalid duration format: seconds (s) are not supported. Use only d, h, m.");
        }
        if (millis == 0L || matched == 0) {
            throw new IllegalArgumentException("Invalid duration format: " + duration + ". Use only d, h, m.");
        }
        return new Date(now - millis);
    }

}
