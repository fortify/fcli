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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;

public final class RemediationsCacheWriter {
    private RemediationsCacheWriter() {}

    /**
     * Builds a remediations cache zip at {@code destination} from already-downloaded FPR files.
     *
     * @param destination cache zip path
     * @param product {@link RemediationsCacheConstants#PRODUCT_SSC} or {@link RemediationsCacheConstants#PRODUCT_FOD}
     * @param selection informational selection metadata
     * @param fprSources ordered FPR sources to pack
     */
    public static RemediationsCacheManifest write(
            Path destination,
            String product,
            Map<String, String> selection,
            List<FprSource> fprSources) {
        if (fprSources == null || fprSources.isEmpty()) {
            throw new FcliSimpleException("Cannot create remediations cache: no FPR files to include");
        }
        if (destination == null) {
            throw new FcliSimpleException("-f/--file must specify a remediations cache zip path");
        }

        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new FcliSimpleException("Failed to create parent directory for " + destination, e);
            }
        }

        RemediationsCacheManifest manifest = new RemediationsCacheManifest();
        manifest.setSchemaVersion(RemediationsCacheConstants.SCHEMA_VERSION);
        manifest.setKind(RemediationsCacheConstants.KIND);
        manifest.setProduct(product);
        manifest.setCreatedAt(Instant.now().toString());
        if (selection != null) {
            manifest.getSelection().putAll(selection);
        }

        Path tempZip = null;
        try {
            tempZip = Files.createTempFile("remediations-cache-", ".zip");
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempZip))) {
                for (int i = 0; i < fprSources.size(); i++) {
                    FprSource source = fprSources.get(i);
                    int order = i + 1;
                    String entryPath = entryPath(source, order);
                    String sha256 = RemediationsCacheSha256.hashFile(source.path());

                    ZipEntry fprEntry = new ZipEntry(entryPath);
                    zos.putNextEntry(fprEntry);
                    Files.copy(source.path(), zos);
                    zos.closeEntry();

                    RemediationsCacheEntry entry = new RemediationsCacheEntry();
                    entry.setOrder(order);
                    entry.setArtifactId(source.artifactId());
                    entry.setReleaseId(source.releaseId());
                    entry.setUploadDate(source.uploadDate());
                    entry.setPath(entryPath);
                    entry.setSha256(sha256);
                    manifest.getEntries().add(entry);
                }

                ZipEntry manifestEntry = new ZipEntry(RemediationsCacheConstants.MANIFEST_ENTRY);
                zos.putNextEntry(manifestEntry);
                byte[] manifestBytes = JsonHelper.getObjectMapper().writerWithDefaultPrettyPrinter()
                        .writeValueAsBytes(manifest);
                zos.write(manifestBytes);
                zos.closeEntry();
            }

            Files.move(tempZip, destination, StandardCopyOption.REPLACE_EXISTING);
            tempZip = null;
            return manifest;
        } catch (IOException e) {
            throw new FcliSimpleException("Failed to write remediations cache to " + destination, e);
        } finally {
            if (tempZip != null) {
                try {
                    Files.deleteIfExists(tempZip);
                } catch (IOException ignored) {
                    // best effort
                }
            }
        }
    }

    private static String entryPath(FprSource source, int order) {
        if (source.artifactId() != null && !source.artifactId().isBlank()) {
            return String.format("%s/%03d_artifact_%s.fpr",
                    RemediationsCacheConstants.FPRS_DIR, order, source.artifactId());
        }
        if (source.releaseId() != null && !source.releaseId().isBlank()) {
            return String.format("%s/%03d_release_%s.fpr",
                    RemediationsCacheConstants.FPRS_DIR, order, source.releaseId());
        }
        return String.format("%s/%03d_remediations.fpr", RemediationsCacheConstants.FPRS_DIR, order);
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
