package com.fortify.cli.aviator.util;

public class FileTypeLanguageMapperUtil {
    private static ExtensionsConfig extensionsConfig;

    public static void initializeConfig(ExtensionsConfig loadedExtensionsConfig) {
        extensionsConfig = loadedExtensionsConfig;
    }

    public static String getProgrammingLanguage(String fileExtension) {
        if(StringUtil.isEmpty(fileExtension)){
            return "Unknown";
        }
        String ext = fileExtension.startsWith(".")
                ? fileExtension
                : "." + fileExtension;

        return extensionsConfig != null
                ? extensionsConfig.getLanguageForExtension(ext)
                : "Unknown";
    }
}