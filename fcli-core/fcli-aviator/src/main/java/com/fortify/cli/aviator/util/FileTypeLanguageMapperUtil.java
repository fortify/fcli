package com.fortify.cli.aviator.util;

import java.util.Optional;

public class FileTypeLanguageMapperUtil {
    private static ExtensionsConfig extensionsConfig;

    public static void initializeConfig(ExtensionsConfig loadedExtensionsConfig) {
        extensionsConfig = loadedExtensionsConfig;
    }

    public static String getProgrammingLanguage(String fileExtension) {
        if (extensionsConfig == null) {
            return "Unknown";
        }

        String ext = fileExtension.startsWith(".")
                ? fileExtension
                : "." + fileExtension;

        return extensionsConfig.getLanguageForExtension(ext);
    }
}