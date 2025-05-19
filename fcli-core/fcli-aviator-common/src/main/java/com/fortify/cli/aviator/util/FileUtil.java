package com.fortify.cli.aviator.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

import com.fortify.cli.common.exception.FcliTechnicalException;

public final class FileUtil {

    private FileUtil() {
    }

    public static boolean isZipFile(String fileName) {
        return isZipFile(new File(fileName));
    }

    public static boolean isZipFile(File file) {
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            return false;
        }
        try (FileInputStream fis = new FileInputStream(file);
             ZipInputStream zis = new ZipInputStream(fis)) {
            return zis.getNextEntry() != null;
        } catch (IOException e) {
            return false;
        }
    }

    @Deprecated
    public static boolean deleteDirectoryStructure(File directory) {
        return directory.isDirectory() ? deleteDirectoryRecursive(directory) : false;
    }

    @Deprecated
    public static boolean deleteDirectoryStructure(File directory, Pattern pattern) {
        return deleteDirectoryRecursive(directory);
    }

    public static boolean deleteDirectoryRecursive(File directory) {
        if (!directory.isDirectory()) {
            return false;
        }
        Path path = directory.toPath();
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            return !Files.exists(path);
        } catch (IOException e) {
            System.err.println("Failed to delete directory: " + path + " - " + e.getMessage());
            return false;
        }
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot > 0 && lastIndexOfDot < fileName.length() - 1) {
            return fileName.substring(lastIndexOfDot + 1);
        }
        return "";
    }

    public static boolean isDirectory(String pathString) {
        if (pathString == null) return false;
        try {
            Path path = Paths.get(pathString);
            return Files.isDirectory(path);
        } catch (Exception e) {
            return false;
        }
    }

    public static void writeStringToFile(Path filePath, String content, boolean overwrite) {
        Path absolutePath = filePath.toAbsolutePath();
        Path parentDir = absolutePath.getParent();

        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                throw new FcliTechnicalException("Error creating parent directories for " + absolutePath, e);
            }
        }

        StandardOpenOption[] options;
        if (overwrite) {
            options = new StandardOpenOption[]{
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            };
        } else {
            options = new StandardOpenOption[]{
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            };
        }

        try {
            Files.writeString(absolutePath, content, StandardCharsets.UTF_8, options);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error writing to file " + absolutePath, e);
        }
    }
}