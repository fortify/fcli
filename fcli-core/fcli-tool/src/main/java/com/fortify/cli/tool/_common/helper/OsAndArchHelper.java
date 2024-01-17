package com.fortify.cli.tool._common.helper;

import java.util.Locale;

public class OsAndArchHelper {
    public static final String getOSString() {
        return normalizeOs(System.getProperty("os.name", "unknown"));
    }
    
    public static final String getArchString() {
        return normalizeArch(System.getProperty("os.arch", "unknown"));
    }
    
    private static String normalizeOs(String value) {
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
            return "linux";
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
}
