package com.fortify.cli.tool.definitions.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFileExtractor {
    private final Path zipPath;

    public ZipFileExtractor(Path zipPath) {
        this.zipPath = zipPath;
    }

    public Map<String, FileTime> extractFilesTo(Path targetDir, Iterable<String> fileNames) throws IOException {
        Map<String, FileTime> fileTimes = new HashMap<>();
        try (InputStream is = Files.newInputStream(zipPath); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (fileNames != null && fileNames.iterator().hasNext()) {
                    for (String fileName : fileNames) {
                        if (entryName.equals(fileName + ".yaml")) {
                            Path outFile = targetDir.resolve(entryName);
                            Files.copy(zis, outFile, StandardCopyOption.REPLACE_EXISTING);
                            fileTimes.put(entryName, entry.getLastModifiedTime());
                            break;
                        }
                    }
                }
            }
        }
        return fileTimes;
    }

    public boolean containsFile(String fileName) throws IOException {
        try (InputStream is = Files.newInputStream(zipPath); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(fileName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
