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
import java.nio.file.AtomicMoveNotSupportedException;
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
 * Builds a remediations cache zip at a destination path. FPR content is written
 * directly into a ZipFS (no per-FPR temp staging). The ZipFS is opened on a sibling
 * {@code *.partial} work file; on successful {@link #finish()} + {@link #close()}, the
 * work file is moved onto {@code destination} (atomic when the filesystem supports it),
 * so an existing destination is not replaced until the archive is complete.
 *
 * <p>{@link #close()} is the sole owner of zip close, work-file cleanup, and publish.
 */
public final class RemediationsCacheWriter implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(RemediationsCacheWriter.class);
    private static final String PARTIAL_SUFFIX = ".partial";

    private final Path destination;
    /** Sibling work path where ZipFS content is written before publish. */
    private final Path workPath;
    /** May be null if initialization failed; {@link #close()} still cleans workPath. */
    private final FileSystem zipFs;
    private final RemediationsCacheManifest manifest;
    private final Exception initError;
    private int nextOrder = 1;
    /** True after manifest.json has been written into the open ZipFS (not yet durable until close+publish). */
    private boolean manifestWritten;
    private boolean closed;

    /**
     * Always constructs an instance so {@link #close()} remains the only cleanup path.
     * Callers must use {@link #create(Path, String, Map)} which closes and rethrows on init failure.
     */
    private RemediationsCacheWriter(Path destination, String product, Map<String, String> selection) {
        this.destination = destination;
        this.workPath = workPathFor(destination);
        this.manifest = newManifest(product, selection);
        FileSystem fs = null;
        Exception error = null;
        try {
            // ZipFS on sibling work file; existing destination is left intact until publish.
            fs = ZipHelper.createZipFileSystem(workPath);
            Files.createDirectories(fs.getPath(RemediationsCacheConstants.FPRS_DIR));
        } catch (AbstractFcliException e) {
            error = e;
        } catch (IOException | RuntimeException e) {
            error = e;
        }
        this.zipFs = fs;
        this.initError = error;
    }

    /**
     * Opens a work zip next to {@code destination} and prepares an empty manifest.
     * On initialization failure, {@link #close()} runs before the exception is rethrown so
     * cleanup is not split across factory and AutoCloseable paths.
     */
    public static RemediationsCacheWriter create(Path destination, String product, Map<String, String> selection) {
        FcliSimpleException.throwIf(destination == null,
                "-f/--file must specify a remediations cache zip path");
        RemediationsCacheWriter writer = new RemediationsCacheWriter(destination, product, selection);
        if (writer.initError != null) {
            Exception cause = writer.initError;
            writer.close();
            if (cause instanceof AbstractFcliException fcliException) {
                throw fcliException;
            }
            throw new FcliTechnicalException(
                    "Failed to initialize remediations cache zip: " + destination, cause);
        }
        return writer;
    }

    /**
     * Convenience for callers that already have FPR files on disk (for example unit tests).
     * Writes via ZipFS into a work file, then publishes to {@code destination}.
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
     * for a successful cache. The archive is only published to {@code destination} after a
     * successful {@link #close()}.
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

    /**
     * Sole owner of zip filesystem close, work-file cleanup, and publish to destination.
     * Destination is replaced only when finish() wrote the manifest and ZipFS closed cleanly.
     */
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
            // ZipFS finalizes the archive on close; failure means work file is not trustworthy.
            if (manifestWritten) {
                deleteQuietly(workPath);
                throw new FcliTechnicalException("Failed to finalize remediations cache zip: " + destination, e);
            }
            logger.warn("Failed to close remediations cache zip work file: {}", workPath, e);
        } finally {
            if (manifestWritten && closeSucceeded) {
                publishWorkFile();
            } else {
                deleteQuietly(workPath);
            }
        }
    }

    /** Moves the completed work zip onto {@link #destination}, preferring an atomic replace. */
    private void publishWorkFile() {
        try {
            try {
                Files.move(workPath, destination,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(workPath, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            deleteQuietly(workPath);
            throw new FcliTechnicalException(
                    "Failed to publish remediations cache zip to " + destination, e);
        }
    }

    private void requireWritable() {
        if (closed) {
            throw new FcliBugException("RemediationsCacheWriter is already closed: " + destination);
        }
        if (initError != null) {
            throw new FcliBugException("RemediationsCacheWriter failed to initialize: " + destination, initError);
        }
        if (manifestWritten) {
            throw new FcliBugException("RemediationsCacheWriter is already finished: " + destination);
        }
    }

    private static Path workPathFor(Path destination) {
        Path fileName = destination.getFileName();
        String name = fileName != null ? fileName.toString() : "remediations-cache.zip";
        Path parent = destination.toAbsolutePath().getParent();
        Path workName = Path.of(name + PARTIAL_SUFFIX);
        return parent != null ? parent.resolve(workName) : workName;
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
        return RemediationsCacheEntry.of(order, entryPath, artifactId, releaseId, uploadDate, sha256);
    }

    private static String entryPath(String artifactId, String releaseId, int order) {
        if (artifactId != null && !artifactId.isBlank()) {
            return String.format("%s/%03d_artifact_%s.fpr",
                    RemediationsCacheConstants.FPRS_DIR, order, sanitizePathSegment(artifactId));
        }
        if (releaseId != null && !releaseId.isBlank()) {
            return String.format("%s/%03d_release_%s.fpr",
                    RemediationsCacheConstants.FPRS_DIR, order, sanitizePathSegment(releaseId));
        }
        return String.format("%s/%03d_remediations.fpr", RemediationsCacheConstants.FPRS_DIR, order);
    }

    /**
     * Keeps artifact/release ids from introducing extra path segments inside the zip
     * ({@code /}, {@code \\}, {@code ..}). Dots are stripped so {@code ..} cannot appear.
     * Manifest still stores the original id fields.
     */
    private static String sanitizePathSegment(String id) {
        String cleaned = id.replaceAll("[^A-Za-z0-9_-]", "_");
        return cleaned.isEmpty() ? "id" : cleaned;
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warn("Failed to delete incomplete remediations cache work file: {}", path, e);
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
