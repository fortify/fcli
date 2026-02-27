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
package com.fortify.cli.common.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Utility methods for platform detection and normalization.
 * Provides OS and architecture detection, normalization, and GitHub Actions compatibility.
 * 
 * @author Ruud Senden
 */
public final class PlatformHelper {
    private PlatformHelper() {}
    
    public static String getOSString() {
        return getOSString(false);
    }
    
    public static String getOSString(boolean detectMusl) {
        return normalizeOs(System.getProperty("os.name", "unknown"), detectMusl);
    }
    
    public static String getArchString() {
        return normalizeArch(System.getProperty("os.arch", "unknown"));
    }
    
    public static String getPlatform() {
        return getPlatform(false);
    }
    
    public static String getPlatform(boolean detectMusl) {
        return String.format("%s/%s", getOSString(detectMusl), getArchString());
    }
    
    public static boolean isWindows() {
        return "windows".equals(getOSString());
    }
    
    public static boolean isLinux() {
        return "linux".equals(getOSString());
    }
    
    public static boolean isMac() {
        return "darwin".equals(getOSString());
    }

    /**
     * Detects if the current Linux system uses musl libc (e.g., Alpine Linux) instead of glibc.
     * This is important for selecting compatible JRE distributions, as glibc-based JREs
     * will not work on musl-based systems.
     * 
     * Detection strategy:
     * 1. Check for Alpine-specific release file (most common musl distribution)
     * 2. Check for musl dynamic linker in standard locations (/lib, /usr/lib)
     * 
     * This approach is more reliable than executing external commands (ldd, sh) which
     * may not be available in minimal Docker images.
     * 
     * @return true if running on a musl-based Linux system, false otherwise
     */
    public static final boolean isMusl() {
        try {
            // Check for Alpine-specific release file (most common musl distribution)
            if (Files.exists(Path.of("/etc/alpine-release"))) {
                return true;
            }
            
            // Check for musl dynamic linker in /lib and /usr/lib
            // Common patterns: /lib/ld-musl-x86_64.so.1, /lib/ld-musl-aarch64.so.1, etc.
            return java.util.stream.Stream.of("/lib", "/usr/lib")
                    .map(Path::of)
                    .filter(Files::exists)
                    .anyMatch(dir -> FileUtils.processMatchingFileStream(dir, "ld-musl-*", 1,
                            stream -> stream.findFirst().isPresent()));
            
        } catch (Exception e) {
            // If detection fails, assume glibc (more common)
        }
        return false;
    }
    
    private static String normalizeOs(String value, boolean detectMusl) {
        value = normalize(value);
        if (value.startsWith("aix")) {
            return "linux";
        }
        if (value.startsWith("hpux")) {
            return "hpux";
        }
        if (value.startsWith("os400")) {
            // Avoid the names such as os4000
            if (value.length() <= 5 || !Character.isDigit(value.charAt(5))) {
                return "os400";
            }
        }
        if (value.startsWith("linux")) {
            return detectMusl && isMusl() ? "linux-musl" : "linux";
        }
        if (value.startsWith("mac") || value.startsWith("osx") || value.contains("darwin")) {
            return "darwin";
        }
        if (value.startsWith("freebsd")) {
            return "linux";
        }
        if (value.startsWith("openbsd")) {
            return "linux";
        }
        if (value.startsWith("netbsd")) {
            return "linux";
        }
        if (value.startsWith("solaris") || value.startsWith("sunos")) {
            return "linux";
        }
        if (value.startsWith("windows")) {
            return "windows";
        }
        if (value.startsWith("zos")) {
            return "linux";
        }
        return value;
    }

    private static String normalizeArch(String value) {
        value = normalize(value);
        if (value.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
            return "x64";
        }
        if (value.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
            return "x86";
        }
        if (value.matches("^(ia64w?|itanium64)$")) {
            return "itanium_64";
        }
        if ("ia64n".equals(value)) {
            return "itanium_32";
        }
        if (value.matches("^(sparc|sparc32)$")) {
            return "sparc_32";
        }
        if (value.matches("^(sparcv9|sparc64)$")) {
            return "sparc_64";
        }
        if (value.matches("^(arm|arm32)$")) {
            return "arm_32";
        }
        if ("aarch64".equals(value)) {
            return "arm64";
        }
        if (value.matches("^(mips|mips32)$")) {
            return "mips_32";
        }
        if (value.matches("^(mipsel|mips32el)$")) {
            return "mipsel_32";
        }
        if ("mips64".equals(value)) {
            return "mips_64";
        }
        if ("mips64el".equals(value)) {
            return "mipsel_64";
        }
        if (value.matches("^(ppc|ppc32)$")) {
            return "ppc_32";
        }
        if (value.matches("^(ppcle|ppc32le)$")) {
            return "ppcle_32";
        }
        if ("ppc64".equals(value)) {
            return "ppc_64";
        }
        if ("ppc64le".equals(value)) {
            return "ppcle_64";
        }
        if ("s390".equals(value)) {
            return "s390_32";
        }
        if ("s390x".equals(value)) {
            return "s390_64";
        }
        if (value.matches("^(riscv|riscv32)$")) {
            return "riscv";
        }
        if ("riscv64".equals(value)) {
            return "riscv64";
        }
        if ("e2k".equals(value)) {
            return "e2k";
        }
        if ("loongarch64".equals(value)) {
            return "loongarch_64";
        }
        return value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }
    
    /**
     * Returns the architecture suffix used by GitHub Actions for JAVA_HOME environment variables.
     * GitHub Actions uses patterns like JAVA_HOME_17_X64 instead of standard os.arch values.
     * Returns the normalized architecture string in uppercase (e.g., "X64", "X86", "ARM64").
     * 
     * @return GitHub Actions-style architecture suffix (e.g., "X64", "X86", "ARM64")
     */
    public static String getGitHubActionsArchSuffix() {
        return getArchString().toUpperCase();
    }
    
    /**
     * Returns the architecture suffix for CI tool cache directories (e.g., GitHub Actions RUNNER_TOOL_CACHE).
     * This method returns platform-specific suffixes that distinguish between binary-incompatible systems,
     * particularly musl vs glibc on Linux, which is critical for tools with embedded JREs.
     * 
     * Examples:
     * - Linux glibc x64: "X64"
     * - Linux musl x64 (Alpine): "X64-musl"
     * - Linux glibc ARM64: "ARM64"
     * - Linux musl ARM64 (Alpine): "ARM64-musl"
     * - Windows x64: "X64"
     * - macOS ARM64: "ARM64"
     * 
     * The musl suffix is only added on Linux systems where musl libc is detected (e.g., Alpine Linux).
     * This ensures that tools with embedded JREs don't share cache directories between glibc and musl systems,
     * as glibc-based JREs will not work on musl-based systems.
     * 
     * @return CI tool cache architecture suffix (e.g., "X64", "X64-musl", "ARM64", "ARM64-musl")
     */
    public static String getToolCacheArchSuffix() {
        String baseArch = getGitHubActionsArchSuffix();
        
        // Only add musl suffix on Linux systems where musl is detected
        if (isLinux() && isMusl()) {
            return baseArch + "-musl";
        }
        
        return baseArch;
    }
}
