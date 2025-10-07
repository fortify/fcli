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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipFile;

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
	private static String toolDefinitionCustomFilePath;
    private static boolean shouldUpdate = false;

    public static final List<ToolDefinitionsOutputDescriptor> getOutputDescriptors() {
		List<ToolDefinitionsOutputDescriptor> result = new ArrayList<>();
		addZipOutputDescriptor(result);
		addYamlOutputDescriptors(result);
		return result;
	}

	@SneakyThrows
	public static final List<ToolDefinitionsOutputDescriptor> updateToolDefinitions(String source, boolean forceUpdate, String maxAge) {
		toolDefinitionCustomFilePath = source;
        shouldUpdate = shouldUpdateToolDefinitions(forceUpdate, maxAge);
        if (shouldUpdate) {
		createDefinitionsStateDir(ToolDefinitionsHelper.DEFINITIONS_STATE_DIR);
		var zip = ToolDefinitionsHelper.DEFINITIONS_STATE_ZIP;
		var descriptor = update(source, zip);
		FcliDataHelper.saveFile(DESCRIPTOR_PATH, descriptor, true);
        }
		return getOutputDescriptors();
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
			if (!source.toLowerCase().endsWith(".zip") && !source.toLowerCase().endsWith(".yaml")) {
				throw new FcliSimpleException("Invalid Tools definitions file");
			}
			mergeDefinitionsZip(dest);
		}
		return new ToolDefinitionsStateDescriptor(source, new Date(getModifiedTime(dest).toMillis()));
	}

	@SneakyThrows
	private static boolean extractYamlFromZip(InputStream is, String yamlFile, Path outFile) {
		try (ZipInputStream zis = new ZipInputStream(is)) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.getName().equals(yamlFile)) {
					Files.copy(zis, outFile, StandardCopyOption.REPLACE_EXISTING);
					if (entry.getLastModifiedTime() != null) {
						Files.setLastModifiedTime(outFile, entry.getLastModifiedTime());
					}
					return true;
				}
			}
		}
		return false;
	}

	@SneakyThrows
	private static void mergeDefinitionsZip(Path dest) {
		var yamlFileNames = new ArrayList<>(getRequiredYamlNames());
		createDefinitionsStateDir(DEFINITIONS_STATE_DIR);
		for (String yamlFile : yamlFileNames) {
			boolean found = false;
			Path outFile = DEFINITIONS_STATE_DIR.resolve(yamlFile);
			if (!found && toolDefinitionCustomFilePath != null && Files.exists(Path.of(toolDefinitionCustomFilePath))) {
				try (InputStream is = Files.newInputStream(Path.of(toolDefinitionCustomFilePath))) {
					found = extractYamlFromZip(is, yamlFile, outFile);
				}
			}
			if (!found && Files.exists(DEFINITIONS_STATE_ZIP)) {
				try (InputStream is = Files.newInputStream(DEFINITIONS_STATE_ZIP)) {
					found = extractYamlFromZip(is, yamlFile, outFile);
				}
			}
			if (!found) {
				try (InputStream is = FileUtils.getResourceInputStream(DEFINITIONS_INTERNAL_ZIP)) {
					found = extractYamlFromZip(is, yamlFile, outFile);
				}
			}
			if (!found) {
				throw new FcliSimpleException("Required tool definition file missing: " + yamlFile);
			}
		}

		FileTime currentTime = FileTime.fromMillis(System.currentTimeMillis());
		try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(dest))) {
			for (String yamlFile : yamlFileNames) {
				Path filePath = DEFINITIONS_STATE_DIR.resolve(yamlFile);
				if (Files.exists(filePath)) {
					java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(yamlFile);
					FileTime fileTime = Files.getLastModifiedTime(filePath);
					zipEntry.setLastModifiedTime(fileTime);
					zos.putNextEntry(zipEntry);
					Files.copy(filePath, zos);
					zos.closeEntry();
				}
			}
		}
        Files.setLastModifiedTime(dest, currentTime);

		for (String yamlFile : yamlFileNames) {
			Path filePath = DEFINITIONS_STATE_DIR.resolve(yamlFile);
			try {
				Files.deleteIfExists(filePath);
			} catch (Exception ignore) {
			}
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
	    String actionResult = shouldUpdate ? "UPDATED" : "SKIPPED_BY_AGE";
        if ( stateDescriptor!=null ) {
            result.add(new ToolDefinitionsOutputDescriptor(ZIP_FILE_NAME, stateDescriptor, actionResult));
        } else {
            result.add(new ToolDefinitionsOutputDescriptor(ZIP_FILE_NAME, "INTERNAL", FcliBuildProperties.INSTANCE.getFcliBuildDate(), actionResult));
        }
	}

	private static Set<String> getRequiredYamlNames() {
		Set<String> requiredYamlNames = new HashSet<>();
		for (String tool : ToolRegistry.getRegisteredToolNames()) {
			requiredYamlNames.add(tool + ".yaml");
		}
		return requiredYamlNames;
	}

	private static final void addYamlOutputDescriptors(List<ToolDefinitionsOutputDescriptor> result) {
		Set<String> requiredYamlNames = getRequiredYamlNames();
		if (!shouldUpdate) {
		    addDefaultYamlDescriptor(result, requiredYamlNames, "SKIPPED_BY_AGE");
		}
		else if (isUpdateDefault()) {
            addDefaultYamlDescriptor(result, requiredYamlNames, "UPDATED");
		}
        else {
			Set<String> foundYamlNames = new HashSet<>();
			String zipPathOnly = toolDefinitionCustomFilePath != null
				? Path.of(toolDefinitionCustomFilePath).getFileName().toString()
				: null;
			ToolDefinitionsZipContent userZipContent = null;
			if (toolDefinitionCustomFilePath != null) {
				try {
					userZipContent = new ToolDefinitionsZipContent(Path.of(toolDefinitionCustomFilePath), requiredYamlNames);
					for (String name : userZipContent.getFileNames()) {
						Date lastModified = userZipContent.getFileDate(name);
						if (requiredYamlNames.contains(name)) {
							result.add(new ToolDefinitionsOutputDescriptor(name, zipPathOnly, lastModified, "UPDATED"));
							foundYamlNames.add(name);
						} else {
							result.add(new ToolDefinitionsOutputDescriptor(name, zipPathOnly, lastModified, "IGNORED"));
						}
					}
				} catch (IOException e) {
					throw new FcliSimpleException("Error loading files from user definitions", e);
				}
			}
			ToolDefinitionsZipContent internalZipContent = null;
			for (String required : requiredYamlNames) {
				if (foundYamlNames.contains(required)) continue;
				String source = ZIP_FILE_NAME;
				Date lastModified = null;
				Path destFile = DEFINITIONS_STATE_DIR.resolve(required);
				if (Files.exists(destFile)) {
					try {
						lastModified = new Date(Files.getLastModifiedTime(destFile).toMillis());
					} catch (IOException e) {
						throw new FcliSimpleException("Error getting last modified time for: " + destFile, e);
					}
				} else {
					if (internalZipContent == null) {
						try (InputStream is = FileUtils.getResourceInputStream(DEFINITIONS_INTERNAL_ZIP)) {
							internalZipContent = new ToolDefinitionsZipContent(is, requiredYamlNames);
						} catch (IOException e) {
							throw new FcliSimpleException("Error loading internal resource zip", e);
						}
					}
					lastModified = internalZipContent.getFileDate(required);
				}
				result.add(new ToolDefinitionsOutputDescriptor(required, source, lastModified, "NOT_PRESENT"));
			}
		}
	}

    private static void addDefaultYamlDescriptor(List<ToolDefinitionsOutputDescriptor> result,
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

	private static boolean isUpdateDefault() {
		return toolDefinitionCustomFilePath != null && toolDefinitionCustomFilePath.contains("https://");
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
