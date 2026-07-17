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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;

/**
 * Opens a remediations cache zip, validates the manifest and entry checksums,
 * and extracts FPR files to a temp directory for ordered processing.
 */
public final class RemediationsCacheReader implements AutoCloseable {
    private final Path cacheZip;
    private final Path extractDir;
    private final RemediationsCacheManifest manifest;
    private final List<Path> orderedFprPaths;

    private RemediationsCacheReader(Path cacheZip, Path extractDir, RemediationsCacheManifest manifest,
            List<Path> orderedFprPaths) {
        this.cacheZip = cacheZip;
        this.extractDir = extractDir;
        this.manifest = manifest;
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

        Path extractDir;
        try {
            extractDir = Files.createTempDirectory("remediations-cache-");
        } catch (IOException e) {
            throw new FcliSimpleException("Failed to create temporary directory for remediations cache", e);
        }

        try {
            Map<String, Path> extracted = extractAll(cacheZip, extractDir);
            Path manifestPath = extracted.get(RemediationsCacheConstants.MANIFEST_ENTRY);
            if (manifestPath == null) {
                throw new FcliSimpleException("Remediations cache is missing " + RemediationsCacheConstants.MANIFEST_ENTRY
                        + ": " + cacheZip);
            }

            RemediationsCacheManifest manifest = JsonHelper.getObjectMapper()
                    .readValue(manifestPath.toFile(), RemediationsCacheManifest.class);
            validateManifest(manifest, cacheZip);

            List<RemediationsCacheEntry> entries = new ArrayList<>(manifest.getEntries());
            entries.sort(Comparator.comparingInt(RemediationsCacheEntry::getOrder));

            List<Path> orderedFprs = new ArrayList<>();
            for (RemediationsCacheEntry entry : entries) {
                Path fprPath = extracted.get(normalizeZipPath(entry.getPath()));
                if (fprPath == null || !Files.isRegularFile(fprPath)) {
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

            return new RemediationsCacheReader(cacheZip, extractDir, manifest, orderedFprs);
        } catch (FcliSimpleException e) {
            deleteRecursivelyQuietly(extractDir);
            throw e;
        } catch (IOException e) {
            deleteRecursivelyQuietly(extractDir);
            throw new FcliSimpleException("Failed to read remediations cache: " + cacheZip, e);
        }
    }

    public RemediationsCacheManifest getManifest() {
        return manifest;
    }

    public List<Path> getOrderedFprPaths() {
        return List.copyOf(orderedFprPaths);
    }

    public Path getCacheZip() {
        return cacheZip;
    }

    @Override
    public void close() {
        deleteRecursivelyQuietly(extractDir);
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

    private static Map<String, Path> extractAll(Path cacheZip, Path extractDir) throws IOException {
        Map<String, Path> extracted = new HashMap<>();
        try (InputStream fis = Files.newInputStream(cacheZip); ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = normalizeZipPath(entry.getName());
                if (name.contains("..")) {
                    throw new FcliSimpleException("Remediations cache contains unsafe path: " + entry.getName());
                }
                Path out = extractDir.resolve(name).normalize();
                if (!out.startsWith(extractDir)) {
                    throw new FcliSimpleException("Remediations cache contains path outside extract dir: " + entry.getName());
                }
                Files.createDirectories(out.getParent());
                Files.copy(zis, out);
                extracted.put(name, out);
            }
        }
        return extracted;
    }

    private static String normalizeZipPath(String path) {
        return path.replace('\\', '/');
    }

    private static void deleteRecursivelyQuietly(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort
                }
            });
        } catch (IOException ignored) {
            // best effort
        }
    }
}
