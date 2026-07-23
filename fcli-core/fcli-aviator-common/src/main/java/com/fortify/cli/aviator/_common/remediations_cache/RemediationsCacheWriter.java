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
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.ZipHelper;

/**
 * Builds a remediations cache zip at a destination path. FPR content is written
 * directly into a ZipFS (no per-FPR temp staging). The ZipFS is opened on a sibling
 * {@code *.partial} work file; on successful {@link #close()}, the work file is moved
 * onto {@code destination} (atomic when the filesystem supports it).
 *
 * <p>Use with try-with-resources. {@link #close()} writes the manifest (when entries
 * exist), closes ZipFS, and publishes or discards the work file.
 */
public final class RemediationsCacheWriter implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(RemediationsCacheWriter.class);
    private static final String PARTIAL_SUFFIX = ".partial";

    private final Path destination;
    private final Path workPath;
    private final FileSystem zipFs;
    private final RemediationsCacheManifest manifest;
    private int nextOrder = 1;

    /**
     * Opens ZipFS on a sibling work file. Exceptions from open are thrown immediately.
     * Entry parent dirs (including {@code fprs/}) are created lazily in {@link #prepareEntryPath}.
     */
    private RemediationsCacheWriter(Path destination, String product, Map<String, String> selection) {
        this.destination = destination;
        this.workPath = workPathFor(destination);
        this.manifest = newManifest(product, selection);
        this.zipFs = ZipHelper.createZipFileSystem(workPath);
    }

    /**
     * Opens a work zip next to {@code destination}. Use with try-with-resources.
     */
    public static RemediationsCacheWriter create(Path destination, String product, Map<String, String> selection) {
        FcliSimpleException.throwIf(destination == null,
                "-f/--file must specify a remediations cache zip path");
        return new RemediationsCacheWriter(destination, product, selection);
    }

    /**
     * Convenience for callers that already have FPR files on disk (for example unit tests).
     * Publishes on successful try-with-resources close.
     */
    public static RemediationsCacheManifest write(
            Path destination,
            String product,
            Map<String, String> selection,
            List<? extends LocalFpr> fprSources) {
        FcliSimpleException.throwIf(fprSources == null || fprSources.isEmpty(),
                "Cannot create remediations cache: no FPR files to include");
        try (RemediationsCacheWriter writer = create(destination, product, selection)) {
            for (LocalFpr source : fprSources) {
                if (source instanceof SscFpr ssc) {
                    writer.addFprFromFile(ssc);
                } else if (source instanceof FodFpr fod) {
                    writer.addFprFromFile(fod);
                } else {
                    throw new FcliTechnicalException("Unsupported local FPR type: " + source.getClass().getName());
                }
            }
            return writer.getManifest();
        }
    }

    /** In-memory manifest (complete after successful close with entries). */
    public RemediationsCacheManifest getManifest() {
        return manifest;
    }

    /**
     * Writes an SSC artifact FPR into the next zip entry (for streaming downloads).
     */
    public void addSscFpr(String artifactId, String uploadDate, Consumer<Path> contentWriter) {
        int order = nextOrder++;
        addEntry(order, sscEntryPath(order, artifactId), artifactId, null, uploadDate, contentWriter);
    }

    /**
     * Writes a FoD release FPR into the next zip entry (for streaming downloads).
     */
    public void addFodFpr(String releaseId, Consumer<Path> contentWriter) {
        int order = nextOrder++;
        addEntry(order, fodEntryPath(order, releaseId), null, releaseId, null, contentWriter);
    }

    /** Copies an existing SSC FPR file into the cache zip and records its checksum. */
    public void addFprFromFile(SscFpr source) {
        validateLocalPath(source.path());
        addSscFpr(source.artifactId(), source.uploadDate(), copyFrom(source.path()));
    }

    /** Copies an existing FoD FPR file into the cache zip and records its checksum. */
    public void addFprFromFile(FodFpr source) {
        validateLocalPath(source.path());
        addFodFpr(source.releaseId(), copyFrom(source.path()));
    }

    private void addEntry(
            int order,
            String entryPath,
            String artifactId,
            String releaseId,
            String uploadDate,
            Consumer<Path> contentWriter) {
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

    private Path prepareEntryPath(String entryPath) throws IOException {
        Path target = zipFs.getPath(entryPath);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return target;
    }

    private static void writeEntryContent(Path target, String entryPath, Consumer<Path> contentWriter) {
        contentWriter.accept(target);
        FcliSimpleException.throwIf(!Files.isRegularFile(target),
                "Cache entry was not written: %s", entryPath);
    }

    private void recordEntry(
            int order, String entryPath, String artifactId, String releaseId, String uploadDate, Path target) {
        String sha256 = RemediationsCacheSha256.hashFile(target);
        manifest.getEntries().add(RemediationsCacheEntry.of(
                order, entryPath, artifactId, releaseId, uploadDate, sha256));
    }

    /**
     * Writes manifest when entries exist, closes ZipFS, then publishes the work file
     * or deletes it if incomplete. Intended for try-with-resources (single close).
     */
    @Override
    public void close() {
        try {
            if (zipFs.isOpen()) {
                if (!manifest.getEntries().isEmpty()) {
                    writeManifest();
                }
                zipFs.close();
            }
        } catch (AbstractFcliException e) {
            deleteQuietly(workPath);
            throw e;
        } catch (IOException e) {
            deleteQuietly(workPath);
            throw new FcliTechnicalException("Failed to finalize remediations cache zip: " + destination, e);
        } catch (RuntimeException e) {
            deleteQuietly(workPath);
            throw new FcliTechnicalException("Failed to finalize remediations cache zip: " + destination, e);
        }
        // Guard with workPath existence so a second close is a no-op after publish/delete.
        if (!Files.exists(workPath)) {
            return;
        }
        if (!manifest.getEntries().isEmpty()) {
            publishWorkFile();
        } else {
            deleteQuietly(workPath);
        }
    }

    private void writeManifest() {
        try {
            byte[] manifestBytes = JsonHelper.getObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(manifest);
            Files.write(zipFs.getPath(RemediationsCacheConstants.MANIFEST_ENTRY), manifestBytes);
        } catch (AbstractFcliException e) {
            throw e;
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to write remediations cache manifest to " + destination, e);
        } catch (RuntimeException e) {
            throw new FcliTechnicalException("Failed to write remediations cache manifest to " + destination, e);
        }
    }

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

    private static void validateLocalPath(Path path) {
        FcliSimpleException.throwIf(path == null, "FPR source path is required");
        FcliSimpleException.throwIf(!Files.isRegularFile(path),
                "FPR source is not a readable regular file: %s", path);
    }

    private static Consumer<Path> copyFrom(Path source) {
        return target -> {
            try {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new FcliTechnicalException("Failed to copy FPR into cache: " + source, e);
            }
        };
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

    private static String sscEntryPath(int order, String artifactId) {
        return String.format("%s/%03d_artifact_%s.fpr",
                RemediationsCacheConstants.FPRS_DIR, order, sanitizePathSegment(artifactId));
    }

    private static String fodEntryPath(int order, String releaseId) {
        return String.format("%s/%03d_release_%s.fpr",
                RemediationsCacheConstants.FPRS_DIR, order, sanitizePathSegment(releaseId));
    }

    private static String sanitizePathSegment(String id) {
        FcliSimpleException.throwIf(id == null || id.isBlank(), "Entry id is required for cache path");
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

    /** Local on-disk FPR to copy into the cache (product-specific; no shared null fields). */
    public sealed interface LocalFpr permits SscFpr, FodFpr {
        Path path();
    }

    /** SSC artifact FPR for cache write helpers/tests. */
    public record SscFpr(Path path, String artifactId, String uploadDate) implements LocalFpr {}

    /** FoD release FPR for cache write helpers/tests. */
    public record FodFpr(Path path, String releaseId) implements LocalFpr {}
}
