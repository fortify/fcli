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
package com.fortify.cli.tool._common.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.util.FileUtils;

/**
 * Utility class providing helper methods for version detection.
 * Concrete register command implementations use these utilities to implement
 * their tool-specific version detection logic.
 * 
 * @author Ruud Senden
 */
public class ToolVersionDetector {
    private static final Logger LOG = LoggerFactory.getLogger(ToolVersionDetector.class);
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\b(\\d+\\.\\d+(?:\\.\\d+)?(?:[.-][a-zA-Z0-9]+)?)\\b");
    
    /**
     * Try to execute tool with given arguments and capture output.
     * Returns null if execution fails or times out.
     * 
     * @param toolBinary Tool binary to execute
     * @param args Arguments to pass
     * @return Output from execution, or null if failed
     */
    public static String tryExecute(File toolBinary, String... args) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command().add(toolBinary.getAbsolutePath());
            for (String arg : args) {
                pb.command().add(arg);
            }
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Read output with timeout
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Limit output size to prevent memory issues
                    if (output.length() > 10000) break;
                }
            }
            
            // Wait for process to complete with timeout
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return null;
            }
            
            // Only return output if process succeeded
            if (process.exitValue() == 0 || process.exitValue() == 1) {
                // Exit code 1 is also acceptable for some tools that return non-zero for --version
                return output.toString();
            }
            
            return null;
        } catch (Exception e) {
            LOG.debug("Failed to execute " + toolBinary + " with args " + String.join(" ", args), e);
            return null;
        }
    }
    
    /**
     * Extract version string from tool output.
     * Looks for common version patterns like "version 1.2.3" or "v1.2.3".
     * 
     * @param output Tool output
     * @return Extracted version or null
     */
    public static String extractVersionFromOutput(String output) {
        if (StringUtils.isBlank(output)) return null;
        
        // Try common version patterns
        String[] patterns = {
            "(?i)version[:\\s]+([\\d.]+(?:-[a-zA-Z0-9]+)?)",
            "(?i)v([\\d.]+(?:-[a-zA-Z0-9]+)?)",
            "([\\d]+\\.[\\d]+\\.[\\d]+(?:-[a-zA-Z0-9]+)?)"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(output);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        // Fallback: look for any version-like string
        Matcher matcher = VERSION_PATTERN.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Detect version from installation descriptor if tool was installed via fcli.
     * 
     * @param toolBinary Tool binary file
     * @return Version from descriptor or null
     */
    public static String detectVersionFromDescriptor(File toolBinary) {
        try {
            File installDir = ToolRegistrationHelper.resolveInstallDir(toolBinary);
            Path descriptorPath = installDir.toPath().resolve("install-descriptor");
            
            if (Files.exists(descriptorPath)) {
                // Look for descriptor file in subdirectories
                // Structure is: install-descriptor/{tool-name}/{version}
                // The version file name contains the version number
                // There may be multiple files - prefer the one that looks like a version (starts with digit)
                try (Stream<Path> paths = Files.walk(descriptorPath, 2)) {
                    return paths
                        .filter(Files::isRegularFile)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .filter(name -> name.matches("^\\d+.*"))  // Prefer files starting with a digit (version numbers)
                        .findFirst()
                        .orElse(null);
                }
            }
        } catch (IOException e) {
            LOG.debug("Failed to read installation descriptor", e);
        }
        return null;
    }
    
    /**
     * Extract version from a JAR file's manifest.
     * Checks common manifest attributes:
     * - Implementation-Version
     * - Bundle-Version
     * - Specification-Version
     * 
     * @param jarFile JAR file to inspect
     * @return Version from manifest or null if not found
     */
    public static String extractVersionFromJarManifest(File jarFile) {
        if (jarFile == null || !jarFile.exists() || !jarFile.getName().endsWith(".jar")) {
            return null;
        }
        
        try (JarFile jar = new JarFile(jarFile)) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                return null;
            }
            
            Attributes mainAttributes = manifest.getMainAttributes();
            
            // Try common version attributes in order of preference
            String[] versionAttributes = {
                "Implementation-Version",
                "Bundle-Version",
                "Specification-Version",
                "Version"
            };
            
            for (String attr : versionAttributes) {
                String version = mainAttributes.getValue(attr);
                if (StringUtils.isNotBlank(version)) {
                    LOG.debug("Found version {} in manifest attribute {}", version, attr);
                    return version.trim();
                }
            }
        } catch (IOException e) {
            LOG.debug("Failed to read manifest from " + jarFile, e);
        }
        
        return null;
    }
    
    /**
     * Extract version from JAR filename matching a pattern.
     * Pattern should use {version} as placeholder for version string.
     * Example: "Core/lib/scancentral-cli-{version}.jar" will match "Core/lib/scancentral-cli-25.4.0.0047.jar"
     * and extract "25.4.0.0047" as the version.
     * 
     * @param installDir Installation directory to search
     * @param jarPattern Pattern with {version} placeholder (e.g., "lib/tool-{version}.jar")
     * @param maxDepth Maximum directory depth to search
     * @return Extracted version from filename or null if no matching file found
     */
    public static String extractVersionFromJarFilename(File installDir, String jarPattern, int maxDepth) {
        if (installDir == null || !installDir.isDirectory()) return null;
        if (StringUtils.isBlank(jarPattern) || !jarPattern.contains("{version}")) {
            LOG.warn("JAR pattern must contain {{version}} placeholder");
            return null;
        }
        
        return extractVersionFromFilePattern(installDir, jarPattern, maxDepth);
    }
    
    /**
     * Find JAR file matching Ant-style glob pattern and extract version from manifest.
     * Pattern supports:
     * - {@code *} matches any characters within a single path segment
     * - {@code **} matches zero or more directory levels
     * Reads Implementation-Version, Bundle-Version, Specification-Version, or Version attribute.
     * Example: "FodUpload.jar", "lib/*.jar", "**\/scancentral-*.jar"
     * 
     * @param installDir Installation directory to search
     * @param jarPattern Ant-style glob pattern for JAR file (e.g., "FodUpload.jar" or "**\/*.jar")
     * @param maxDepth Maximum directory depth to search
     * @return Extracted version from manifest or null if not found
     */
    public static String extractVersionFromJarManifestPattern(File installDir, String jarPattern, int maxDepth) {
        if (installDir == null || !installDir.isDirectory()) return null;
        if (StringUtils.isBlank(jarPattern)) {
            LOG.warn("JAR pattern cannot be blank");
            return null;
        }
        
        try {
            Path jarPath = FileUtils.processMatchingFileStream(
                installDir.toPath(), 
                jarPattern, 
                maxDepth,
                stream -> stream
                    .filter(p -> p.toString().endsWith(".jar"))
                    .findFirst()
                    .orElse(null)
            );
            
            if (jarPath != null) {
                String versionFromManifest = extractVersionFromJarManifest(jarPath.toFile());
                if (versionFromManifest != null) {
                    return versionFromManifest;
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to scan directory for JAR pattern: " + jarPattern, e);
        }
        
        return null;
    }
    
    /**
     * Extract version from a file matching a specific pattern.
     * Pattern should use {version} as placeholder for version string.
     * Example: "Core/lib/scancentral-cli-{version}.jar" will match "Core/lib/scancentral-cli-25.4.0.0047.jar"
     * and extract "25.4.0.0047" as the version.
     * 
     * @param installDir Installation directory to search
     * @param filePattern Pattern with {version} placeholder (e.g., "lib/tool-{version}.jar")
     * @param maxDepth Maximum directory depth to search
     * @return Extracted version or null if no matching file found
     */
    public static String extractVersionFromFilePattern(File installDir, String filePattern, int maxDepth) {
        if (installDir == null || !installDir.isDirectory()) return null;
        if (StringUtils.isBlank(filePattern) || !filePattern.contains("{version}")) {
            LOG.warn("Invalid file pattern: must contain {{version}} placeholder");
            return null;
        }
        
        // Convert pattern to regex, escaping special chars except {version}
        String regexPattern = Pattern.quote(filePattern)
            .replace("\\{version\\}", "\\E([\\d.]+(?:-[a-zA-Z0-9.]+)?)\\Q");
        Pattern pattern = Pattern.compile(regexPattern);
        
        try (Stream<Path> paths = Files.walk(installDir.toPath(), maxDepth)) {
            return paths
                .filter(Files::isRegularFile)
                .map(p -> installDir.toPath().relativize(p).toString().replace('\\', '/'))
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .map(m -> m.group(1))
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            LOG.debug("Failed to scan directory for version pattern: " + filePattern, e);
            return null;
        }
    }
}
