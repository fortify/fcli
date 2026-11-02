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
package com.fortify.cli.aviator.ssc.helper;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.util.ZipHelper;

public final class AviatorSSCFprFilterHelper {
    private static final String WEBINSPECT_XML = "webinspect.xml";
    private static final String AUDIT_FVDL = "audit.fvdl";
    private static final Set<String> DAST_ENTRIES = Set.of(
        "VERSION", WEBINSPECT_XML, "audit.xml", "attachments.xml");

    private AviatorSSCFprFilterHelper() {}

    public enum Engine {
        SAST, DAST
    }

    public static Path createEngineSpecificFpr(Path sourceFpr, Engine engine) {
        return createEngineSpecificFpr(sourceFpr, engine, null);
    }

    static Path createEngineSpecificFpr(Path sourceFpr, Engine engine, Path tempDirectory) {
        Path filteredFpr = null;
        try {
            String prefix = "aviator_" + engine.name().toLowerCase() + "_";
            filteredFpr = tempDirectory == null
                ? Files.createTempFile(prefix, ".fpr")
                : Files.createTempFile(tempDirectory, prefix, ".fpr");
            try (FileSystem sourceFileSystem = ZipHelper.openZipFileSystem(sourceFpr);
                    FileSystem filteredFileSystem = ZipHelper.createZipFileSystem(filteredFpr)) {
                Path sourceRoot = sourceFileSystem.getPath("/");
                Path filteredRoot = filteredFileSystem.getPath("/");
                try (Stream<Path> entries = Files.walk(sourceRoot)) {
                    entries.filter(path -> shouldCopy(path, sourceRoot, engine))
                        .forEach(path -> copyEntry(path, sourceRoot, filteredRoot));
                }
                validateRequiredEntry(filteredRoot, engine);
            }
            return filteredFpr;
        } catch (RuntimeException | IOException e) {
            if (filteredFpr != null) {
                try {
                    Files.deleteIfExists(filteredFpr);
                } catch (IOException cleanupException) {
                    e.addSuppressed(cleanupException);
                }
            }
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new FcliTechnicalException("Failed to filter FPR for " + engine + " upload", e);
        }
    }

    private static boolean shouldCopy(Path path, Path sourceRoot, Engine engine) {
        if (path.equals(sourceRoot)) {
            return false;
        }
        String entryName = sourceRoot.relativize(path).toString().replace('\\', '/');
        return switch (engine) {
            case SAST -> !entryName.equals(WEBINSPECT_XML);
            case DAST -> DAST_ENTRIES.contains(entryName);
        };
    }

    private static void copyEntry(Path source, Path sourceRoot, Path filteredRoot) {
        Path target = filteredRoot.resolve(sourceRoot.relativize(source).toString());
        try {
            if (Files.isDirectory(source)) {
                Files.createDirectories(target);
            } else {
                Files.createDirectories(target.getParent());
                Files.copy(source, target);
            }
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to copy FPR entry " + sourceRoot.relativize(source), e);
        }
    }

    private static void validateRequiredEntry(Path filteredRoot, Engine engine) {
        String requiredEntry = engine == Engine.SAST ? AUDIT_FVDL : WEBINSPECT_XML;
        if (!Files.isRegularFile(filteredRoot.resolve(requiredEntry))) {
            throw new FcliSimpleException(
                "FPR does not contain the required " + engine + " engine payload: " + requiredEntry);
        }
    }
}
