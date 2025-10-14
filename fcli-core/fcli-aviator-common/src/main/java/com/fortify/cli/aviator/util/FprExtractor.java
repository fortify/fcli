package com.fortify.cli.aviator.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.exception.FcliTechnicalException;

public class FprExtractor implements Closeable, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FprExtractor.class);
    private final Path zipFilePath;
    private final Path extractedPath;
    private Thread shutdownHook;

    public FprExtractor(Path zipFilePath) throws IOException {
        this.zipFilePath = zipFilePath;
        this.extractedPath = Files.createTempDirectory(new File(System.getProperty("java.io.tmpdir")).toPath(), "fpr-extraction-");
        this.shutdownHook = registerShutdownHook(this);
        extract();
    }

    public Path getExtractedPath() {
        return extractedPath;
    }

    @Override
    public void close() {
        if (shutdownHook!=null) {
            FileUtil.deleteDirectoryRecursive(extractedPath.toFile());
            unregisterShutdownHook(shutdownHook);
            this.shutdownHook = null;
        }
    }
    
    private static final Thread registerShutdownHook(Closeable closeable) {
        var shutdownHook = new Thread(() -> {
            try {
                closeable.close();
            } catch (Exception e) {
                LOG.warn("Failed to invoke close() method on shutdown: {}", e.getMessage());
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        return shutdownHook;
    }
    
    private static final void unregisterShutdownHook(Thread shutdownHook) {
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // JVM is already shutting down
            }
        }
    }
    

    private void extract() throws IOException {
        LOG.debug("Starting extraction of zip file: {}", zipFilePath);
        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path extractedFilePath = extractedPath.resolve(entry.getName());
                if (!extractedFilePath.normalize().startsWith(extractedPath.normalize())) {
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
            }
        } catch (IOException e) {
            close();
            throw new FcliTechnicalException("Failed to extract zip file: " + e.getMessage(), e);
        }
        LOG.debug("Successfully extracted zip file to: {}", extractedPath);
    }
}