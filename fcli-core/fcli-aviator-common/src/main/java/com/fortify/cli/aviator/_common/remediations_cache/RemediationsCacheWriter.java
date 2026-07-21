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
package com.fortify.cli.aviator._common.remediations_cache;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.AbstractFcliException;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.ZipHelper;

/**
 * Builds a remediations cache zip at a destination path. Prefer opening a writer, writing
 * each FPR directly into the zip filesystem (for example via download APIs that accept
 * {@link Path}), then {@link #finish()} and close. No intermediate temp zip is used.
 *
 * <p>The destination is kept only when {@link #finish()} succeeded <em>and</em>
 * {@link #close()} closed the zip filesystem successfully (ZipFS finalizes the archive on close).
 */
public final class RemediationsCacheWriter implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(RemediationsCacheWriter.class);

    private final Path destination;
    private final FileSystem zipFs;
    private final RemediationsCacheManifest manifest;
    private int nextOrder = 1;
    /** True after manifest.json has been written into the open ZipFS (not yet durable until close). */
    private boolean manifestWritten;
    private boolean closed;

    private RemediationsCacheWriter(Path destination, FileSystem zipFs, RemediationsCacheManifest manifest) {
        this.destination = destination;
        this.zipFs = zipFs;
        this.manifest = manifest;
    }

    /**
     * Opens (or recreates) the destination zip and prepares an empty manifest.
     * Existing destination files are replaced by {@link ZipHelper#createZipFileSystem(Path)}.
     */
    public static RemediationsCacheWriter create(Path destination, String product, Map<String, String> selection) {
        FcliSimpleException.throwIf(destination == null,
                "-f/--file must specify a remediations cache zip path");
        // ZipHelper.createZipFileSystem creates parent dirs and replaces any existing file.
        FileSystem zipFs = ZipHelper.createZipFileSystem(destination);
        try {
            Files.createDirectories(zipFs.getPath(RemediationsCacheConstants.FPRS_DIR));
            // On success, zipFs ownership transfers to the returned writer (not closed here).
            return new RemediationsCacheWriter(destination, zipFs, newManifest(product, selection));
        } catch (AbstractFcliException e) {
            abortCreate(zipFs, destination);
            throw e;
        } catch (IOException e) {
            abortCreate(zipFs, destination);
            throw new FcliTechnicalException("Failed to initialize remediations cache zip: " + destination, e);
        } catch (RuntimeException e) {
            abortCreate(zipFs, destination);
            throw new FcliTechnicalException("Failed to initialize remediations cache zip: " + destination, e);
        }
    }

    /**
     * Convenience for callers that already have FPR files on disk (for example unit tests).
     * Writes directly to {@code destination} (no temp zip).
     */
    public static RemediationsCacheManifest write(
            Path destination,
            String product,
            Map<String, String> selection,
            List<FprSource> fprSources) {
        FcliSimpleException.throwIf(fprSources == null || fprSources.isEmpty(),
                "Cannot create remediations cache: no FPR files to include");
        try (RemediationsCacheWriter writer = create(destination, product, selection)) {
            for (FprSource source : fprSources) {
                writer.addFprFromFile(source);
            }
            return writer.finish();
        }
    }

    /**
     * Allocates the next zip entry path, invokes {@code contentWriter} to populate it
     * (for example download into the path), then records SHA-256 and manifest metadata.
     */
    public void addFpr(String artifactId, String releaseId, String uploadDate, Consumer<Path> contentWriter) {
        requireWritable();
        int order = nextOrder++;
        String entryPath = entryPath(artifactId, releaseId, order);
        try {
            Path target = prepareEntryPath(entryPath);
            writeEntryContent(target, entryPath, contentWriter);
            recordEntry(order, entryPath, artifactId, releaseId, uploadDate, target);
        } catch (AbstractFcliException e) {
            throw e;
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to add remediations cache entry " + entryPath, e);
        } catch (RuntimeException e) {
            throw new FcliTechnicalException("Failed to add remediations cache entry " + entryPath, e);
        }
    }

    /** Copies an existing FPR file into the cache zip and records its checksum. */
    public void addFprFromFile(FprSource source) {
        requireWritable();
        FcliSimpleException.throwIf(source == null || source.path() == null,
                "FPR source path is required");
        FcliSimpleException.throwIf(!Files.isRegularFile(source.path()),
                "FPR source is not a readable regular file: %s", source.path());
        addFpr(source.artifactId(), source.releaseId(), source.uploadDate(), target -> {
            try {
                Files.copy(source.path(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new FcliTechnicalException("Failed to copy FPR into cache: " + source.path(), e);
            }
        });
    }

    /** Creates parent directories for a zip entry and returns its path. */
    private Path prepareEntryPath(String entryPath) throws IOException {
        Path target = zipFs.getPath(entryPath);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return target;
    }

    /** Invokes {@code contentWriter} and ensures a regular file was produced at {@code target}. */
    private static void writeEntryContent(Path target, String entryPath, Consumer<Path> contentWriter) {
        contentWriter.accept(target);
        FcliSimpleException.throwIf(!Files.isRegularFile(target),
                "Cache entry was not written: %s", entryPath);
    }

    /** Hashes the entry and appends it to the in-memory manifest. */
    private void recordEntry(
            int order, String entryPath, String artifactId, String releaseId, String uploadDate, Path target) {
        String sha256 = RemediationsCacheSha256.hashFile(target);
        manifest.getEntries().add(toManifestEntry(order, entryPath, artifactId, releaseId, uploadDate, sha256));
    }

    /**
     * Writes {@code manifest.json} into the open zip. Must be called before {@link #close()}
     * for a successful cache. The archive is only durable after a successful {@link #close()}.
     * Returns the completed manifest.
     */
    public RemediationsCacheManifest finish() {
        requireWritable();
        FcliSimpleException.throwIf(manifest.getEntries().isEmpty(),
                "Cannot create remediations cache: no FPR files to include");
        try {
            byte[] manifestBytes = JsonHelper.getObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(manifest);
            Files.write(zipFs.getPath(RemediationsCacheConstants.MANIFEST_ENTRY), manifestBytes);
            manifestWritten = true;
            return manifest;
        } catch (AbstractFcliException e) {
            throw e;
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to write remediations cache manifest to " + destination, e);
        } catch (RuntimeException e) {
            throw new FcliTechnicalException("Failed to write remediations cache manifest to " + destination, e);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        boolean closeSucceeded = false;
        try {
            if (zipFs != null && zipFs.isOpen()) {
                zipFs.close();
            }
            closeSucceeded = true;
        } catch (IOException e) {
            // ZipFS finalizes the archive on close; failure means destination is not trustworthy.
            if (manifestWritten) {
                deleteQuietly(destination);
                throw new FcliTechnicalException("Failed to finalize remediations cache zip: " + destination, e);
            }
            logger.warn("Failed to close remediations cache zip: {}", destination, e);
        } finally {
            // Keep destination only when finish() wrote the manifest and ZipFS closed cleanly.
            if (!(manifestWritten && closeSucceeded)) {
                deleteQuietly(destination);
            }
        }
    }

    private void requireWritable() {
        if (closed) {
            throw new FcliBugException("RemediationsCacheWriter is already closed: " + destination);
        }
        if (manifestWritten) {
            throw new FcliBugException("RemediationsCacheWriter is already finished: " + destination);
        }
    }

    private static void abortCreate(FileSystem zipFs, Path destination) {
        closeQuietly(zipFs);
        deleteQuietly(destination);
    }

    private static RemediationsCacheManifest newManifest(String product, Map<String, String> selection) {
        RemediationsCacheManifest manifest = new RemediationsCacheManifest();
        manifest.setSchemaVersion(RemediationsCacheConstants.SCHEMA_VERSION);
        manifest.setKind(RemediationsCacheConstants.KIND);
        manifest.setProduct(product);
        manifest.setCreatedAt(Instant.now().toString());
        if (selection != null) {
            manifest.getSelection().putAll(selection);
        }
        return manifest;
    }

    private static RemediationsCacheEntry toManifestEntry(
            int order, String entryPath, String artifactId, String releaseId, String uploadDate, String sha256) {
        RemediationsCacheEntry entry = new RemediationsCacheEntry();
        entry.setOrder(order);
        entry.setArtifactId(artifactId);
        entry.setReleaseId(releaseId);
        entry.setUploadDate(uploadDate);
        entry.setPath(entryPath);
        entry.setSha256(sha256);
        return entry;
    }

    private static String entryPath(String artifactId, String releaseId, int order) {
        if (artifactId != null && !artifactId.isBlank()) {
            return String.format("%s/%03d_artifact_%s.fpr",
                    RemediationsCacheConstants.FPRS_DIR, order, artifactId);
        }
        if (releaseId != null && !releaseId.isBlank()) {
            return String.format("%s/%03d_release_%s.fpr",
                    RemediationsCacheConstants.FPRS_DIR, order, releaseId);
        }
        return String.format("%s/%03d_remediations.fpr", RemediationsCacheConstants.FPRS_DIR, order);
    }

    private static void closeQuietly(FileSystem zipFs) {
        if (zipFs == null || !zipFs.isOpen()) {
            return;
        }
        try {
            zipFs.close();
        } catch (IOException e) {
            logger.warn("Failed to close zip filesystem during abort", e);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warn("Failed to delete incomplete remediations cache file: {}", path, e);
        }
    }

    public record FprSource(Path path, String artifactId, String releaseId, String uploadDate) {
        public static FprSource forSsc(Path path, String artifactId, String uploadDate) {
            return new FprSource(path, artifactId, null, uploadDate);
        }

        public static FprSource forFod(Path path, String releaseId) {
            return new FprSource(path, null, releaseId, null);
        }
    }
}
