package com.fortify.cli.aviator.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipUtils {
    public static Path extractZip(String zipFilePath) throws IOException {
        Path tempDir = Files.createTempDirectory(new File(System.getProperty("java.io.tmpdir")).toPath(), "fpr-extraction-");
        tempDir.toFile().deleteOnExit();

        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path extractedFilePath = tempDir.resolve(entry.getName());

                if (!extractedFilePath.normalize().startsWith(tempDir.normalize())) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(extractedFilePath);
                } else {
                    Files.createDirectories(extractedFilePath.getParent());
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, extractedFilePath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                extractedFilePath.toFile().deleteOnExit();
            }
        } catch (IOException e) {
            try {
                Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ex) {

                            }
                        });
            } catch (IOException cleanupEx) {

            }
            throw new IOException("Failed to extract zip file: " + e.getMessage(), e);
        }

        return tempDir;
    }
}