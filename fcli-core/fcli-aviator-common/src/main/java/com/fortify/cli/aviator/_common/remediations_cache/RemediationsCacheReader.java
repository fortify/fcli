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

import com.fortify.cli.common.exception.AbstractFcliException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.ZipHelper;

import lombok.Getter;

/**
 * Opens a remediations cache zip as a {@link FileSystem} and exposes manifest data and
 * ordered FPR paths. Construction only validates the zip path and opens the filesystem;
 * manifest/entry validation happens lazily via getters so a single try-with-resources on
 * this reader owns {@code cacheFs} cleanup.
 */
public final class RemediationsCacheReader implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(RemediationsCacheReader.class);

    private final Path cacheZip;
    private final FileSystem cacheFs;

    @Getter(lazy = true)
    private final RemediationsCacheManifest manifest = loadAndValidateManifest();

    @Getter(lazy = true)
    private final List<RemediationsCacheEntry> orderedEntries = loadOrderedEntries();

    @Getter(lazy = true)
    private final List<ResolvedFpr> orderedResolvedFprs = loadOrderedResolvedFprs();

    @Getter(lazy = true)
    private final List<Path> orderedFprPaths = loadOrderedFprPaths();

    /** Manifest entry paired with its validated ZipFS path (same order as apply). */
    public record ResolvedFpr(RemediationsCacheEntry entry, Path fprPath) {}

    /**
     * Validates {@code cacheZip} and opens it as a zip file system. Prefer use via
     * try-with-resources so {@link #close()} always runs.
     */
    public RemediationsCacheReader(Path cacheZip) {
        Path validated = validateCacheZip(cacheZip);
        FileSystem opened;
        try {
            opened = ZipHelper.openZipFileSystem(validated);
        } catch (AbstractFcliException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new FcliTechnicalException("Failed to open remediations cache: " + validated, e);
        }
        this.cacheZip = validated;
        this.cacheFs = opened;
    }

    /** Factory alias for call sites that prefer a static open style. */
    public static RemediationsCacheReader open(Path cacheZip) {
        return new RemediationsCacheReader(cacheZip);
    }

    public Path getCacheZip() {
        return cacheZip;
    }

    public List<String> getOrderedEntryPaths() {
        return getOrderedEntries().stream().map(RemediationsCacheEntry::getPath).toList();
    }

    public List<String> getOrderedArtifactIds() {
        return getOrderedEntries().stream()
                .map(e -> e.getArtifactId() != null ? e.getArtifactId() : "")
                .toList();
    }

    public List<String> getOrderedReleaseIds() {
        return getOrderedEntries().stream()
                .map(e -> e.getReleaseId() != null ? e.getReleaseId() : "")
                .toList();
    }

    /**
     * Ensures the cache was produced for the expected product ({@code ssc} or {@code fod}).
     */
    public void requireProduct(String expectedProduct) {
        String actual = getManifest().getProduct();
        FcliSimpleException.throwIf(!expectedProduct.equals(actual),
                "Remediations cache product is '%s' but this command expects '%s': %s",
                actual, expectedProduct, cacheZip);
    }

    @Override
    public void close() {
        if (cacheFs == null || !cacheFs.isOpen()) {
            return;
        }
        try {
            cacheFs.close();
        } catch (IOException e) {
            logger.warn("Failed to close cache filesystem", e);
        }
    }

    private static Path validateCacheZip(Path cacheZip) {
        FcliSimpleException.throwIf(cacheZip == null,
                "--from-cache must specify a remediations cache zip path");
        FcliSimpleException.throwIf(!Files.exists(cacheZip),
                "Remediations cache file does not exist: %s", cacheZip);
        FcliSimpleException.throwIf(!Files.isRegularFile(cacheZip),
                "Remediations cache path is not a regular file: %s", cacheZip);
        FcliSimpleException.throwIf(!Files.isReadable(cacheZip),
                "Remediations cache file is not readable: %s", cacheZip);
        return cacheZip;
    }

    private RemediationsCacheManifest loadAndValidateManifest() {
        Path manifestPath = cacheFs.getPath(RemediationsCacheConstants.MANIFEST_ENTRY);
        FcliSimpleException.throwIf(!Files.isRegularFile(manifestPath),
                "Remediations cache is missing %s: %s",
                RemediationsCacheConstants.MANIFEST_ENTRY, cacheZip);
        try (var manifestInputStream = Files.newInputStream(manifestPath)) {
            RemediationsCacheManifest manifest = JsonHelper.getObjectMapper()
                    .readValue(manifestInputStream, RemediationsCacheManifest.class);
            validateManifest(manifest);
            return manifest;
        } catch (AbstractFcliException e) {
            throw e;
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to read remediations cache manifest: " + cacheZip, e);
        } catch (RuntimeException e) {
            throw new FcliTechnicalException("Failed to parse remediations cache manifest: " + cacheZip, e);
        }
    }

    private void validateManifest(RemediationsCacheManifest manifest) {
        FcliSimpleException.throwIf(manifest == null,
                "Remediations cache manifest is empty: %s", cacheZip);
        FcliSimpleException.throwIf(manifest.getSchemaVersion() != RemediationsCacheConstants.SCHEMA_VERSION,
                "Unsupported remediations cache schemaVersion %s (expected %s): %s",
                manifest.getSchemaVersion(), RemediationsCacheConstants.SCHEMA_VERSION, cacheZip);
        FcliSimpleException.throwIf(!RemediationsCacheConstants.KIND.equals(manifest.getKind()),
                "Invalid remediations cache kind '%s' (expected %s): %s",
                manifest.getKind(), RemediationsCacheConstants.KIND, cacheZip);
        FcliSimpleException.throwIf(StringUtils.isBlank(manifest.getProduct()),
                "Remediations cache manifest is missing product: %s", cacheZip);
        FcliSimpleException.throwIf(manifest.getEntries() == null || manifest.getEntries().isEmpty(),
                "Remediations cache has no entries: %s", cacheZip);
        for (RemediationsCacheEntry entry : manifest.getEntries()) {
            FcliSimpleException.throwIf(entry == null,
                    "Remediations cache contains a null entry: %s", cacheZip);
            FcliSimpleException.throwIf(StringUtils.isBlank(entry.getPath()),
                    "Remediations cache entry is missing path: %s", cacheZip);
            FcliSimpleException.throwIf(StringUtils.isBlank(entry.getSha256()),
                    "Remediations cache entry is missing sha256: %s", entry.getPath());
        }
    }

    private List<RemediationsCacheEntry> loadOrderedEntries() {
        List<RemediationsCacheEntry> entries = new ArrayList<>(getManifest().getEntries());
        entries.sort(Comparator.comparingInt(RemediationsCacheEntry::getOrder));
        return List.copyOf(entries);
    }

    private List<ResolvedFpr> loadOrderedResolvedFprs() {
        List<ResolvedFpr> resolved = new ArrayList<>();
        for (RemediationsCacheEntry entry : getOrderedEntries()) {
            Path fprPath = resolveEntryPath(entry.getPath());
            FcliSimpleException.throwIf(!Files.isRegularFile(fprPath),
                    "Remediations cache entry path not found in zip: %s", entry.getPath());
            String actualSha = RemediationsCacheSha256.hashFile(fprPath);
            FcliSimpleException.throwIf(!actualSha.equalsIgnoreCase(entry.getSha256()),
                    "SHA-256 mismatch for cache entry %s (expected %s, actual %s)",
                    entry.getPath(), entry.getSha256(), actualSha);
            resolved.add(new ResolvedFpr(entry, fprPath));
        }
        FcliSimpleException.throwIf(resolved.isEmpty(),
                "Remediations cache contains no FPR entries: %s", cacheZip);
        return List.copyOf(resolved);
    }

    private List<Path> loadOrderedFprPaths() {
        return getOrderedResolvedFprs().stream().map(ResolvedFpr::fprPath).toList();
    }

    /**
     * Resolves a manifest entry path inside the zip FS. Rejects empty, absolute, and parent-escape paths.
     * ZipFS keeps paths in-archive (not host zip-slip), but untrusted manifests must still stay relative.
     */
    private Path resolveEntryPath(String entryPath) {
        FcliSimpleException.throwIf(StringUtils.isBlank(entryPath),
                "Remediations cache entry path is blank: %s", cacheZip);
        String normalized = entryPath.replace('\\', '/').trim();
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        FcliSimpleException.throwIf(normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*"),
                "Remediations cache contains absolute path: %s", entryPath);
        FcliSimpleException.throwIf(normalized.contains(".."),
                "Remediations cache contains unsafe path: %s", entryPath);
        Path resolved = cacheFs.getPath(normalized).normalize();
        // After normalize, parent segments must not reappear.
        String resolvedStr = resolved.toString().replace('\\', '/');
        FcliSimpleException.throwIf(resolvedStr.contains("..") || resolvedStr.startsWith("/"),
                "Remediations cache contains unsafe path: %s", entryPath);
        return resolved;
    }
}
