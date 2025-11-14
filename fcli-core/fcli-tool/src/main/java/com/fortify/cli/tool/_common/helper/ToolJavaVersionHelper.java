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

import java.nio.file.Path;

import com.fortify.cli.common.util.JreHelper;

/**
 * Utility class for detecting and validating Java versions.
 * Delegates to JreHelper in fcli-common for actual implementation.
 * 
 * @author Ruud Senden
 */
public final class ToolJavaVersionHelper {
    private ToolJavaVersionHelper() {}
    
    /**
     * Detects the Java version from a given Java home directory.
     * Delegates to {@link JreHelper#detectJavaVersion(Path)}.
     * 
     * @param javaHome Path to Java home directory
     * @return Major version string (e.g., "8", "11", "17", "21") or null if detection fails
     */
    public static String detectJavaVersion(Path javaHome) {
        return JreHelper.detectJavaVersion(javaHome);
    }
    
    /**
     * Checks if the detected Java version is compatible with the required version.
     * Delegates to {@link JreHelper#isCompatibleVersion(String, String)}.
     * 
     * @param detectedVersion Detected major version (e.g., "8", "11", "17")
     * @param requiredVersion Required major version (e.g., "8", "11", "17")
     * @return true if versions are compatible, false otherwise
     */
    public static boolean isCompatibleVersion(String detectedVersion, String requiredVersion) {
        return JreHelper.isCompatibleVersion(detectedVersion, requiredVersion);
    }
    
    /**
     * Finds the java executable in the given directory.
     * Delegates to {@link JreHelper#findJavaExecutable(Path)}.
     * 
     * @param dir Directory to search for java executable
     * @return Path to java executable, or null if not found
     */
    public static Path findJavaExecutable(Path dir) {
        return JreHelper.findJavaExecutable(dir);
    }
    
    /**
     * Derives the Java home directory from a java executable path.
     * Delegates to {@link JreHelper#deriveJavaHome(Path)}.
     * 
     * @param javaExecutable Path to java executable
     * @return Path to Java home directory, or null if cannot be determined
     */
    public static Path deriveJavaHome(Path javaExecutable) {
        return JreHelper.deriveJavaHome(javaExecutable);
    }
}
