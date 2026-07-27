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
package com.fortify.cli.aviator._common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.FcliTechnicalException;

/**
 * Host-filesystem temporary FPR used only when a real {@link Path} is required
 * (for example online downloads before {@code FprHandle} can open a nested zip FS).
 * Prefer ZipFS entry paths from {@code RemediationsCacheReader}/{@code RemediationsCacheWriter}
 * whenever content already lives in a durable cache zip.
 *
 * <p>Owns cleanup via {@link AutoCloseable}; use try-with-resources so interrupt/exception
 * paths still delete the file.
 */
public final class AviatorTempFprFile implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(AviatorTempFprFile.class);

    private final Path path;

    private AviatorTempFprFile(Path path) {
        this.path = path;
    }

    /**
     * @param nameHint short label (artifact/release id); sanitized into the temp file prefix
     */
    public static AviatorTempFprFile create(String nameHint) {
        String safe = sanitize(nameHint);
        try {
            return new AviatorTempFprFile(Files.createTempFile("aviator-" + safe + "-", ".fpr"));
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to create temporary FPR file for " + safe, e);
        }
    }

    public Path path() {
        return path;
    }

    @Override
    public void close() {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warn("Failed to delete temporary FPR file: {}", path, e);
        }
    }

    private static String sanitize(String nameHint) {
        if (nameHint == null || nameHint.isBlank()) {
            return "fpr";
        }
        String cleaned = nameHint.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.length() > 40 ? cleaned.substring(0, 40) : cleaned;
    }
}
