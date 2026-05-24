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
package com.fortify.cli.fpr.source.cli.cmd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.aviator.fpr.utils.XmlUtils;
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
 * Merges a source directory into an FPR file as a source archive.
 * Creates or replaces the {@code src-archive/} entries with a
 * generated {@code index.xml} and numbered archive entries.
 */
@Command(name = "merge-source", aliases = {"ms"})
public class FPRSourceMergeCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;

    @Option(names = {"--fpr"}, required = true, order = 1)
    private Path fprPath;

    @Option(names = {"--source-dir"}, required = true, order = 2)
    private Path sourceDir;

    @Option(names = {"-f", "--output-file"}, order = 3)
    private Path outputPath;

    @Override
    public JsonNode getJsonNode() {
        validateInputs();
        if (outputPath == null) { outputPath = fprPath; }

        try {
            int added = mergeSourceArchive();
            var node = MAPPER.createObjectNode();
            node.put("fpr", fprPath.toString());
            node.put("sourceDir", sourceDir.toString());
            node.put("output", outputPath.toString());
            node.put("filesAdded", added);
            node.put("__action__", added > 0 ? "MERGED" : "NO_FILES");
            return node;
        } catch (IOException e) {
            throw new FcliTechnicalException("Error merging source archive", e);
        }
    }

    private void validateInputs() {
        if (!Files.exists(fprPath)) {
            throw new FcliSimpleException("FPR file not found: " + fprPath);
        }
        if (!Files.isDirectory(sourceDir)) {
            throw new FcliSimpleException("Source directory not found: " + sourceDir);
        }
    }

    private int mergeSourceArchive() throws IOException {
        // Collect source files
        var sourceFiles = new java.util.ArrayList<Path>();
        try (var walk = Files.walk(sourceDir)) {
            walk.filter(Files::isRegularFile).forEach(sourceFiles::add);
        }
        if (sourceFiles.isEmpty()) { return 0; }

        Path tempFpr = Files.createTempFile("fcli-source-merge-", ".fpr");
        try {
            try (var zipIn = new ZipFile(fprPath.toFile());
                 OutputStream fos = Files.newOutputStream(tempFpr);
                 var zipOut = new ZipOutputStream(fos)) {

                // Copy all existing entries except src-archive/*
                Enumeration<? extends ZipEntry> entries = zipIn.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.getName().startsWith("src-archive/")) {
                        continue;
                    }
                    zipOut.putNextEntry(new ZipEntry(entry.getName()));
                    try (InputStream is = zipIn.getInputStream(entry)) {
                        is.transferTo(zipOut);
                    }
                    zipOut.closeEntry();
                }

                // Generate index.xml and add source files
                var doc = XmlUtils.secureDocumentBuilder(false).newDocument();
                var root = doc.createElement("index");
                doc.appendChild(root);

                int id = 0;
                for (var file : sourceFiles) {
                    String relativePath = sourceDir.relativize(file).toString().replace('\\', '/');

                    // Add to index
                    var entryElem = doc.createElement("entry");
                    entryElem.setAttribute("id", String.valueOf(id));
                    entryElem.setTextContent(relativePath);
                    root.appendChild(entryElem);

                    // Add file content
                    zipOut.putNextEntry(new ZipEntry("src-archive/" + id));
                    Files.copy(file, zipOut);
                    zipOut.closeEntry();

                    id++;
                }

                // Write index.xml
                zipOut.putNextEntry(new ZipEntry("src-archive/index.xml"));
                var transformer = XmlUtils.secureTransformerFactory().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.transform(new DOMSource(doc), new StreamResult(zipOut));
                zipOut.closeEntry();
            }

            Files.move(tempFpr, outputPath, StandardCopyOption.REPLACE_EXISTING);
            return sourceFiles.size();
        } catch (Exception e) {
            Files.deleteIfExists(tempFpr);
            if (e instanceof IOException ioe) { throw ioe; }
            throw new FcliTechnicalException("Failed to merge source archive", e);
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
