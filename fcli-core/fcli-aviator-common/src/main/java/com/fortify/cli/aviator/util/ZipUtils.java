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

import com.fortify.cli.common.exception.FcliBugException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipUtils {
    private static final Logger logger = LoggerFactory.getLogger(ZipUtils.class);

    public static Path extractZip(String zipFilePath) throws IOException {
        logger.debug("Starting extraction of zip file: {}", zipFilePath);
        Path tempDir = Files.createTempDirectory(new File(System.getProperty("java.io.tmpdir")).toPath(), "fpr-extraction-");
        tempDir.toFile().deleteOnExit();

        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path extractedFilePath = tempDir.resolve(entry.getName());

                if (!extractedFilePath.normalize().startsWith(tempDir.normalize())) {
                    throw new FcliBugException("Bad zip entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(extractedFilePath);
                } else {
                    Files.createDirectories(extractedFilePath.getParent());
                    if (entry.getSize() == 0) {
                        Files.createFile(extractedFilePath);
                    } else {
                        try (InputStream is = zipFile.getInputStream(entry)) {
                            Files.copy(is, extractedFilePath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
                extractedFilePath.toFile().deleteOnExit();
            }
        } catch (IOException e) {
            try (var stream = Files.walk(tempDir).sorted(Comparator.reverseOrder())) {
                stream.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ex) {
                        logger.warn("Failed to delete temporary file: {} due to {}", path, ex.getMessage());
                    }
                });
            } catch (IOException cleanupEx) {
                logger.warn("Failed to walk temporary directory for cleanup: {}", cleanupEx.getMessage());
            }
            throw new FcliBugException("Failed to extract zip file: " + e.getMessage(), e);
        }

        logger.debug("Successfully extracted zip file to: {}", tempDir);
        return tempDir;
    }
}