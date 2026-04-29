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
package com.fortify.cli.fpr.trim.cli.cmd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * Trims an FPR to only the latest scan by removing any previous scan FVDL
 * files. In a merged FPR, additional scan data is stored as numbered
 * {@code audit_N.fvdl} entries. This command removes those older scans,
 * keeping only the primary {@code audit.fvdl}.
 */
@Command(name = "trim")
public class FPRTrimCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;

    @Option(names = {"--fpr"}, required = true, order = 1)
    private Path fprPath;

    @Option(names = {"-f", "--output-file"}, order = 2)
    private Path outputPath;

    @Override
    public JsonNode getJsonNode() {
        if (!Files.exists(fprPath)) {
            throw new FcliSimpleException("FPR file not found: " + fprPath);
        }
        if (outputPath == null) { outputPath = fprPath; }

        try {
            var result = trimToLastScan();
            var node = MAPPER.createObjectNode();
            node.put("fpr", fprPath.toString());
            node.put("output", outputPath.toString());
            node.put("removedEntries", result.removedCount);
            node.put("removedEntryNames", String.join(", ", result.removedNames));
            node.put("__action__", result.removedCount > 0 ? "TRIMMED" : "UNCHANGED");
            return node;
        } catch (IOException e) {
            throw new FcliTechnicalException("Error trimming FPR file", e);
        }
    }

    private record TrimResult(int removedCount, Set<String> removedNames) {}

    private TrimResult trimToLastScan() throws IOException {
        Set<String> toRemove = new HashSet<>();

        // Identify entries to remove: older scan FVDLs and their MACs
        try (var zipFile = new ZipFile(fprPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                // Older scans are stored as audit_N.fvdl, audit_N.fvdl.mac
                if (name.matches("audit_\\d+\\.fvdl(\\.mac)?")) {
                    toRemove.add(name);
                }
            }
        }

        if (toRemove.isEmpty()) {
            if (!outputPath.equals(fprPath)) {
                Files.copy(fprPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return new TrimResult(0, toRemove);
        }

        Path tempFpr = Files.createTempFile("fcli-trim-", ".fpr");
        try {
            try (var zipIn = new ZipFile(fprPath.toFile());
                 OutputStream fos = Files.newOutputStream(tempFpr);
                 var zipOut = new ZipOutputStream(fos)) {

                Enumeration<? extends ZipEntry> entries = zipIn.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (toRemove.contains(entry.getName())) {
                        continue;
                    }
                    zipOut.putNextEntry(new ZipEntry(entry.getName()));
                    try (InputStream is = zipIn.getInputStream(entry)) {
                        is.transferTo(zipOut);
                    }
                    zipOut.closeEntry();
                }
            }

            Files.move(tempFpr, outputPath, StandardCopyOption.REPLACE_EXISTING);
            return new TrimResult(toRemove.size(), toRemove);
        } catch (Exception e) {
            Files.deleteIfExists(tempFpr);
            throw new FcliTechnicalException("Failed to write trimmed FPR", e);
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
