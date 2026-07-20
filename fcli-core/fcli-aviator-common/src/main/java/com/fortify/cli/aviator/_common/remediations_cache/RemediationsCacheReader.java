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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.ZipHelper;

/**
 * Opens a remediations cache zip, validates the manifest and entry checksums,
 * and exposes FPR files directly from the zip file system for ordered processing.
 */
public final class RemediationsCacheReader implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(RemediationsCacheReader.class);

    private final Path cacheZip;
    private final FileSystem cacheFs;
    private final RemediationsCacheManifest manifest;
    private final List<RemediationsCacheEntry> orderedEntries;
    private final List<Path> orderedFprPaths;

    private RemediationsCacheReader(Path cacheZip, FileSystem cacheFs, RemediationsCacheManifest manifest,
            List<RemediationsCacheEntry> orderedEntries, List<Path> orderedFprPaths) {
        this.cacheZip = cacheZip;
        this.cacheFs = cacheFs;
        this.manifest = manifest;
        this.orderedEntries = orderedEntries;
        this.orderedFprPaths = orderedFprPaths;
    }

    public static RemediationsCacheReader open(Path cacheZip) {
        if (cacheZip == null) {
            throw new FcliSimpleException("--from-cache must specify a remediations cache zip path");
        }
        if (!Files.exists(cacheZip)) {
            throw new FcliSimpleException("Remediations cache file does not exist: " + cacheZip);
        }
        if (!Files.isRegularFile(cacheZip)) {
            throw new FcliSimpleException("Remediations cache path is not a regular file: " + cacheZip);
        }
        if (!Files.isReadable(cacheZip)) {
            throw new FcliSimpleException("Remediations cache file is not readable: " + cacheZip);
        }

        FileSystem cacheFs = null;
        try {
            cacheFs = ZipHelper.openZipFileSystem(cacheZip);
            Path manifestPath = cacheFs.getPath(RemediationsCacheConstants.MANIFEST_ENTRY);
            if (!Files.isRegularFile(manifestPath)) {
                throw new FcliSimpleException("Remediations cache is missing " + RemediationsCacheConstants.MANIFEST_ENTRY
                        + ": " + cacheZip);
            }

            RemediationsCacheManifest manifest;
            try (var manifestInputStream = Files.newInputStream(manifestPath)) {
                manifest = JsonHelper.getObjectMapper()
                        .readValue(manifestInputStream, RemediationsCacheManifest.class);
            }
            validateManifest(manifest, cacheZip);

            List<RemediationsCacheEntry> entries = new ArrayList<>(manifest.getEntries());
            entries.sort(Comparator.comparingInt(RemediationsCacheEntry::getOrder));

            List<Path> orderedFprs = new ArrayList<>();
            for (RemediationsCacheEntry entry : entries) {
                Path fprPath = getEntryPath(cacheFs, entry.getPath());
                if (!Files.isRegularFile(fprPath)) {
                    throw new FcliSimpleException("Remediations cache entry path not found in zip: " + entry.getPath());
                }
                String actualSha = RemediationsCacheSha256.hashFile(fprPath);
                if (!actualSha.equalsIgnoreCase(entry.getSha256())) {
                    throw new FcliSimpleException("SHA-256 mismatch for cache entry " + entry.getPath()
                            + " (expected " + entry.getSha256() + ", actual " + actualSha + ")");
                }
                orderedFprs.add(fprPath);
            }

            if (orderedFprs.isEmpty()) {
                throw new FcliSimpleException("Remediations cache contains no FPR entries: " + cacheZip);
            }

            FileSystem openCacheFs = cacheFs;
            cacheFs = null;
            return new RemediationsCacheReader(cacheZip, openCacheFs, manifest, List.copyOf(entries), orderedFprs);
        } catch (FcliSimpleException | FcliTechnicalException e) {
            throw e;
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to read remediations cache: " + cacheZip, e);
        } finally {
            closeQuietly(cacheFs);
        }
    }

    public RemediationsCacheManifest getManifest() {
        return manifest;
    }

    public List<RemediationsCacheEntry> getOrderedEntries() {
        return orderedEntries;
    }

    public List<Path> getOrderedFprPaths() {
        return List.copyOf(orderedFprPaths);
    }

    public List<String> getOrderedEntryPaths() {
        return orderedEntries.stream().map(RemediationsCacheEntry::getPath).toList();
    }

    public List<String> getOrderedArtifactIds() {
        return orderedEntries.stream()
                .map(e -> e.getArtifactId() != null ? e.getArtifactId() : "")
                .toList();
    }

    public List<String> getOrderedReleaseIds() {
        return orderedEntries.stream()
                .map(e -> e.getReleaseId() != null ? e.getReleaseId() : "")
                .toList();
    }

    public Path getCacheZip() {
        return cacheZip;
    }

    @Override
    public void close() {
        closeQuietly(cacheFs);
    }

    private static void validateManifest(RemediationsCacheManifest manifest, Path cacheZip) {
        if (manifest == null) {
            throw new FcliSimpleException("Remediations cache manifest is empty: " + cacheZip);
        }
        if (manifest.getSchemaVersion() != RemediationsCacheConstants.SCHEMA_VERSION) {
            throw new FcliSimpleException("Unsupported remediations cache schemaVersion "
                    + manifest.getSchemaVersion() + " (expected " + RemediationsCacheConstants.SCHEMA_VERSION
                    + "): " + cacheZip);
        }
        if (!RemediationsCacheConstants.KIND.equals(manifest.getKind())) {
            throw new FcliSimpleException("Invalid remediations cache kind '" + manifest.getKind()
                    + "' (expected " + RemediationsCacheConstants.KIND + "): " + cacheZip);
        }
        if (StringUtils.isBlank(manifest.getProduct())) {
            throw new FcliSimpleException("Remediations cache manifest is missing product: " + cacheZip);
        }
        if (manifest.getEntries() == null || manifest.getEntries().isEmpty()) {
            throw new FcliSimpleException("Remediations cache has no entries: " + cacheZip);
        }
        for (RemediationsCacheEntry entry : manifest.getEntries()) {
            if (entry == null) {
                throw new FcliSimpleException("Remediations cache contains a null entry: " + cacheZip);
            }
            if (StringUtils.isBlank(entry.getPath())) {
                throw new FcliSimpleException("Remediations cache entry is missing path: " + cacheZip);
            }
            if (StringUtils.isBlank(entry.getSha256())) {
                throw new FcliSimpleException("Remediations cache entry is missing sha256: " + entry.getPath());
            }
        }
    }

    private static Path getEntryPath(FileSystem cacheFs, String entryPath) {
        String normalizedPath = normalizeZipPath(entryPath);
        if (normalizedPath.contains("..")) {
            throw new FcliSimpleException("Remediations cache contains unsafe path: " + entryPath);
        }
        return cacheFs.getPath(normalizedPath).normalize();
    }

    private static String normalizeZipPath(String path) {
        return path.replace('\\', '/');
    }

    private static void closeQuietly(FileSystem cacheFs) {
        if (cacheFs == null || !cacheFs.isOpen()) {
            return;
        }
        try {
            cacheFs.close();
        } catch (IOException e) {
            logger.warn("Failed to close cache filesystem", e);
        }
    }
}
