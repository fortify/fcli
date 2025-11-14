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
package com.fortify.cli.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

/**
 * Utility methods for JRE detection and version validation.
 * 
 * @author Ruud Senden
 */
public final class JreHelper {
    private static final Pattern VERSION_PATTERN = Pattern.compile("version\\s+\"([^\"]+)\"");
    
    private JreHelper() {}
    
    /**
     * Searches for a Java installation with the specified major version.
     * First checks JAVA_HOME environment variable, then searches PATH entries.
     * 
     * @param majorVersion Major version to search for (e.g., "8", "11", "17", "21")
     * @return Path to Java home directory, or null if not found
     */
    public static String findJavaHome(String majorVersion) {
        if (StringUtils.isBlank(majorVersion)) {
            return null;
        }
        
        // Check JAVA_HOME environment variable
        String javaHome = EnvHelper.env("JAVA_HOME");
        if (javaHome != null) {
            String detectedVersion = detectJavaVersion(Paths.get(javaHome));
            if (isCompatibleVersion(detectedVersion, majorVersion)) {
                return javaHome;
            }
        }
        
        // Search PATH entries
        String pathEnv = EnvHelper.env("PATH");
        if (pathEnv != null) {
            String[] pathEntries = pathEnv.split(SystemUtils.IS_OS_WINDOWS ? ";" : ":");
            for (String pathEntry : pathEntries) {
                Path javaExecutable = findJavaExecutable(Paths.get(pathEntry));
                if (javaExecutable != null) {
                    Path derivedJavaHome = deriveJavaHome(javaExecutable);
                    if (derivedJavaHome != null) {
                        String detectedVersion = detectJavaVersion(derivedJavaHome);
                        if (isCompatibleVersion(detectedVersion, majorVersion)) {
                            return derivedJavaHome.toString();
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Detects the major Java version from a Java home directory by executing java -version.
     * 
     * @param javaHome Path to Java home directory
     * @return Major version string (e.g., "8", "11", "17"), or null if detection fails
     */
    public static String detectJavaVersion(Path javaHome) {
        if (javaHome == null || !Files.isDirectory(javaHome)) {
            return null;
        }
        
        Path javaExecutable = findJavaExecutable(javaHome);
        if (javaExecutable == null) {
            return null;
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(javaExecutable.toString(), "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return null;
            }
            
            if (process.exitValue() != 0) {
                return null;
            }
            
            return parseVersionFromOutput(output.toString());
            
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }
    
    /**
     * Checks if detected version is compatible with required version.
     * Currently performs simple equality check on major versions.
     * 
     * @param detectedVersion Detected major version (e.g., "17")
     * @param requiredVersion Required major version (e.g., "17")
     * @return true if versions are compatible
     */
    public static boolean isCompatibleVersion(String detectedVersion, String requiredVersion) {
        if (detectedVersion == null || requiredVersion == null) {
            return false;
        }
        return detectedVersion.equals(requiredVersion);
    }
    
    /**
     * Finds the java executable in a directory. Checks both the directory itself
     * and a bin subdirectory.
     * 
     * @param dir Directory to search
     * @return Path to java executable, or null if not found
     */
    public static Path findJavaExecutable(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return null;
        }
        
        String javaExecName = SystemUtils.IS_OS_WINDOWS ? "java.exe" : "java";
        
        // Check directory itself
        Path directExec = dir.resolve(javaExecName);
        if (Files.isRegularFile(directExec) && Files.isExecutable(directExec)) {
            return directExec;
        }
        
        // Check bin subdirectory
        Path binExec = dir.resolve("bin").resolve(javaExecName);
        if (Files.isRegularFile(binExec) && Files.isExecutable(binExec)) {
            return binExec;
        }
        
        return null;
    }
    
    /**
     * Derives the Java home directory from a java executable path by following symlinks
     * and navigating up the directory tree.
     * 
     * @param javaExecutable Path to java executable
     * @return Path to Java home directory, or null if cannot be determined
     */
    public static Path deriveJavaHome(Path javaExecutable) {
        if (javaExecutable == null || !Files.exists(javaExecutable)) {
            return null;
        }
        
        try {
            // Follow symlinks to get the real path
            Path realPath = javaExecutable.toRealPath();
            
            // If in bin directory, parent is java home
            // e.g., /usr/lib/jvm/java-17-openjdk/bin/java -> /usr/lib/jvm/java-17-openjdk
            if (realPath.getParent() != null && "bin".equals(realPath.getParent().getFileName().toString())) {
                return realPath.getParent().getParent();
            }
            
            // Otherwise, parent itself is java home
            return realPath.getParent();
            
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * Parses major version from java -version output.
     * Handles formats like:
     * - "1.8.0_292" -> "8"
     * - "11.0.11" -> "11"
     * - "17.0.1" -> "17"
     * 
     * @param output Output from java -version command
     * @return Major version string, or null if parsing fails
     */
    private static String parseVersionFromOutput(String output) {
        if (output == null) {
            return null;
        }
        
        Matcher matcher = VERSION_PATTERN.matcher(output);
        if (!matcher.find()) {
            return null;
        }
        
        String versionString = matcher.group(1);
        
        // Handle old format: 1.8.0_292 -> 8
        if (versionString.startsWith("1.")) {
            String[] parts = versionString.substring(2).split("[._]");
            return parts.length > 0 ? parts[0] : null;
        }
        
        // Handle new format: 11.0.11 or 17.0.1 -> 11 or 17
        String[] parts = versionString.split("[._-]");
        return parts.length > 0 ? parts[0] : null;
    }
}
