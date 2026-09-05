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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.FcliTechnicalException;


public final class FileUtil {

    private static final Logger LOG = LoggerFactory.getLogger(FileTypeLanguageMapperUtil.class);

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
            LOG.error("Failed to delete directory: {} - {}", path, e.getMessage());
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

    /**
     * P2.2 canonical form for file hashing. Normalises line endings to LF and strips a single
     * trailing newline. Both the audit side (writing the hash into remediations.xml) and the
     * apply side (verifying it) must call this before hashing so the two sides agree
     * byte-for-byte regardless of the OS that ran the audit or whether the file had a
     * trailing newline on disk. Callers hash the UTF-8 bytes of the returned string.
     */
    public static String canonicalizeForHash(String content) {
        if (content == null) return "";
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.endsWith("\n")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public static String stripSyntheticLineMarkers(String content, String fileName) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        String language = FileTypeLanguageMapperUtil.getProgrammingLanguage(getFileExtension(fileName));
        String commentSymbol = LanguageCommentMapperUtil.getProgrammingLanguageComment(language);
        String stripped = content;
        if (!"Unknown".equals(commentSymbol)) {
            String closingToken = commentSymbol.equals("<!--") ? "-->"
                : commentSymbol.equals("<%--") ? "--%>"
                : null;
            Pattern markerPattern = Pattern.compile(
                "[ \\t]*" + Pattern.quote(commentSymbol) + " L\\d+"
                    + (closingToken != null ? "[ \\t]*" + Pattern.quote(closingToken) : "")
                    + "[ \\t]*$");
            String[] lines = content.split("\\R", -1);
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                Matcher matcher = markerPattern.matcher(lines[i]);
                result.append(matcher.find() ? lines[i].substring(0, matcher.start()) : lines[i]);
                if (i < lines.length - 1) {
                    result.append('\n');
                }
            }
            stripped = result.toString();
        }
        return trimBlankLines(stripped);
    }

    private static String trimBlankLines(String content) {
        String[] lines = content.split("\\R", -1);
        int start = 0;
        int end = lines.length - 1;
        while (start <= end && lines[start].isBlank()) start++;
        while (end >= start && lines[end].isBlank()) end--;
        if (start > end) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            sb.append(lines[i]);
            if (i < end) sb.append('\n');
        }
        return sb.toString();
    }

}
