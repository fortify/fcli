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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliSimpleException;

class RemediationsCacheRoundTripTest {
    @TempDir Path tempDir;

    @Test
    void writeAndReadRoundTripPreservesOrderAndHashes() throws Exception {
        Path fpr1 = tempDir.resolve("one.fpr");
        Path fpr2 = tempDir.resolve("two.fpr");
        Files.writeString(fpr1, "fpr-content-1");
        Files.writeString(fpr2, "fpr-content-2");
        Path zip = tempDir.resolve("cache.zip");

        RemediationsCacheWriter.write(
                zip,
                RemediationsCacheConstants.PRODUCT_SSC,
                Map.of("mode", "all", "appVersionId", "10001"),
                List.of(
                        RemediationsCacheWriter.FprSource.forSsc(fpr1, "123", "2026-07-10T08:00:00Z"),
                        RemediationsCacheWriter.FprSource.forSsc(fpr2, "456", "2026-07-11T09:30:00Z")));

        try (RemediationsCacheReader reader = RemediationsCacheReader.open(zip)) {
            assertEquals(RemediationsCacheConstants.PRODUCT_SSC, reader.getManifest().getProduct());
            assertEquals(2, reader.getOrderedFprPaths().size());
            assertEquals("fpr-content-1", Files.readString(reader.getOrderedFprPaths().get(0)));
            assertEquals("fpr-content-2", Files.readString(reader.getOrderedFprPaths().get(1)));
            assertEquals("123", reader.getManifest().getEntries().get(0).getArtifactId());
            assertEquals("456", reader.getManifest().getEntries().get(1).getArtifactId());
        }
    }

    @Test
    void writeDirectlyIntoDestinationZipWithoutTempStaging() throws Exception {
        Path zip = tempDir.resolve("direct.zip");
        try (RemediationsCacheWriter writer = RemediationsCacheWriter.create(
                zip, RemediationsCacheConstants.PRODUCT_FOD, Map.of("mode", "release"))) {
            writer.addFpr(null, "rel-1", null, path -> {
                try {
                    Files.writeString(path, "streamed-fpr");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            writer.finish();
        }
        try (RemediationsCacheReader reader = RemediationsCacheReader.open(zip)) {
            reader.requireProduct(RemediationsCacheConstants.PRODUCT_FOD);
            assertEquals("streamed-fpr", Files.readString(reader.getOrderedFprPaths().get(0)));
            assertEquals("rel-1", reader.getOrderedReleaseIds().get(0));
        }
    }

    @Test
    void emptySourcesRejected() {
        Path zip = tempDir.resolve("empty.zip");
        assertThrows(FcliSimpleException.class, () ->
                RemediationsCacheWriter.write(zip, RemediationsCacheConstants.PRODUCT_SSC, Map.of(), List.of()));
    }

    @Test
    void cachedFprCanBeOpenedAsNestedZipFileSystem() throws Exception {
        Path fpr = tempDir.resolve("nested.fpr");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(fpr))) {
            zos.putNextEntry(new ZipEntry("audit.fvdl"));
            zos.write("<FVDL />".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        Path zip = tempDir.resolve("cache.zip");
        RemediationsCacheWriter.write(
                zip,
                RemediationsCacheConstants.PRODUCT_SSC,
                Map.of("mode", "artifact-id"),
                List.of(RemediationsCacheWriter.FprSource.forSsc(fpr, "1", null)));

        try (RemediationsCacheReader reader = RemediationsCacheReader.open(zip);
             FprHandle fprHandle = new FprHandle(reader.getOrderedFprPaths().get(0))) {
            assertTrue(Files.exists(fprHandle.getPath("/audit.fvdl")));
        }
    }

    @Test
    void badSha256RejectedOnLazyPathLoad() throws Exception {
        Path fpr = tempDir.resolve("one.fpr");
        Files.writeString(fpr, "original");
        Path zip = tempDir.resolve("cache.zip");
        RemediationsCacheWriter.write(
                zip,
                RemediationsCacheConstants.PRODUCT_SSC,
                Map.of("mode", "artifact-id"),
                List.of(RemediationsCacheWriter.FprSource.forSsc(fpr, "1", null)));

        Path corruptZip = tempDir.resolve("corrupt.zip");
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip));
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(corruptZip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                zos.putNextEntry(new ZipEntry(entry.getName()));
                if (entry.getName().endsWith(".fpr")) {
                    zos.write("corrupted".getBytes(StandardCharsets.UTF_8));
                } else {
                    zis.transferTo(zos);
                }
                zos.closeEntry();
            }
        }

        assertThrows(FcliSimpleException.class, () -> {
            try (RemediationsCacheReader reader = RemediationsCacheReader.open(corruptZip)) {
                reader.getOrderedFprPaths();
            }
        });
    }

    @Test
    void missingManifestRejectedOnLazyManifestLoad() throws Exception {
        Path zip = tempDir.resolve("no-manifest.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("fprs/001.fpr"));
            zos.write("x".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        FcliSimpleException ex = assertThrows(FcliSimpleException.class, () -> {
            try (RemediationsCacheReader reader = RemediationsCacheReader.open(zip)) {
                reader.getManifest();
            }
        });
        assertTrue(ex.getMessage().contains("manifest.json"));
    }

    @Test
    void requireProductRejectsMismatch() throws Exception {
        Path fpr = tempDir.resolve("one.fpr");
        Files.writeString(fpr, "x");
        Path zip = tempDir.resolve("cache.zip");
        RemediationsCacheWriter.write(
                zip,
                RemediationsCacheConstants.PRODUCT_SSC,
                Map.of("mode", "artifact-id"),
                List.of(RemediationsCacheWriter.FprSource.forSsc(fpr, "1", null)));

        assertThrows(FcliSimpleException.class, () -> {
            try (RemediationsCacheReader reader = RemediationsCacheReader.open(zip)) {
                reader.requireProduct(RemediationsCacheConstants.PRODUCT_FOD);
            }
        });
    }
}
