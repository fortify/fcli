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
package com.fortify.cli.fpr.merge.cli.cmd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
 * Merges audit data (tags, comments, suppression) from a source FPR into the
 * primary FPR. The primary FPR's audit data takes precedence for conflicting
 * entries. The primary FPR's FVDL (scan results) is preserved unchanged.
 *
 * <p>This provides the audit-merge functionality of FPRUtility's {@code -merge}
 * option. Full FVDL merge (combining scan results with instance-id migration)
 * is not supported; use FPRUtility directly for that scenario.
 */
@Command(name = "merge")
public class FPRMergeCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String AUDIT_NS = "xmlns://www.fortify.com/schema/audit";

    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;

    @Option(names = {"--project"}, required = true, order = 1)
    private Path projectPath;

    @Option(names = {"--source"}, required = true, order = 2)
    private Path sourcePath;

    @Option(names = {"-f", "--output-file"}, order = 3)
    private Path outputPath;

    @Override
    public JsonNode getJsonNode() {
        validateInputs();
        if (outputPath == null) { outputPath = projectPath; }

        try {
            int merged = mergeAuditData();
            var node = MAPPER.createObjectNode();
            node.put("project", projectPath.toString());
            node.put("source", sourcePath.toString());
            node.put("output", outputPath.toString());
            node.put("mergedIssues", merged);
            node.put("__action__", merged > 0 ? "MERGED" : "NO_CHANGES");
            return node;
        } catch (IOException e) {
            throw new FcliTechnicalException("Error merging FPR files", e);
        }
    }

    private void validateInputs() {
        if (!Files.exists(projectPath)) {
            throw new FcliSimpleException("Primary project file not found: " + projectPath);
        }
        if (!Files.exists(sourcePath)) {
            throw new FcliSimpleException("Source project file not found: " + sourcePath);
        }
    }

    private int mergeAuditData() throws IOException {
        // Parse audit.xml from source FPR
        Document sourceAuditDoc = readAuditXml(sourcePath);
        if (sourceAuditDoc == null) { return 0; }

        // Parse audit.xml from primary FPR
        Document primaryAuditDoc = readAuditXml(projectPath);

        // Build map of source issues by instanceId
        var sourceIssues = extractIssueElements(sourceAuditDoc);
        if (sourceIssues.isEmpty()) { return 0; }

        // Merge: add source issues that are not in primary
        if (primaryAuditDoc == null) {
            primaryAuditDoc = createEmptyAuditDoc();
        }
        var primaryIssues = extractIssueElements(primaryAuditDoc);
        var primaryIds = new HashSet<>(primaryIssues.keySet());

        Element projectRoot = (Element) primaryAuditDoc.getElementsByTagNameNS(AUDIT_NS, "ProjectVersionAudit").item(0);
        if (projectRoot == null) {
            projectRoot = (Element) primaryAuditDoc.getDocumentElement();
        }

        int mergedCount = 0;
        for (var entry : sourceIssues.entrySet()) {
            if (!primaryIds.contains(entry.getKey())) {
                var imported = primaryAuditDoc.importNode(entry.getValue(), true);
                projectRoot.appendChild(imported);
                mergedCount++;
            }
        }

        // Write updated FPR
        writeUpdatedFpr(primaryAuditDoc, mergedCount > 0);
        return mergedCount;
    }

    private Document readAuditXml(Path fprPath) throws IOException {
        try (var zipFile = new ZipFile(fprPath.toFile())) {
            ZipEntry auditEntry = zipFile.getEntry("audit.xml");
            if (auditEntry == null) { return null; }
            try (InputStream is = zipFile.getInputStream(auditEntry)) {
                return XmlUtils.secureDocumentBuilder(true).parse(is);
            } catch (Exception e) {
                throw new FcliTechnicalException("Failed to parse audit.xml from " + fprPath, e);
            }
        }
    }

    private Map<String, Element> extractIssueElements(Document doc) {
        var map = new java.util.LinkedHashMap<String, Element>();
        NodeList issues = doc.getElementsByTagNameNS(AUDIT_NS, "Issue");
        for (int i = 0; i < issues.getLength(); i++) {
            var elem = (Element) issues.item(i);
            String instanceId = elem.getAttribute("instanceId");
            if (instanceId != null && !instanceId.isBlank()) {
                map.put(instanceId, elem);
            }
        }
        return map;
    }

    private Document createEmptyAuditDoc() {
        try {
            var doc = XmlUtils.secureDocumentBuilder(true).newDocument();
            var root = doc.createElementNS(AUDIT_NS, "ProjectVersionAudit");
            doc.appendChild(root);
            return doc;
        } catch (Exception e) {
            throw new FcliTechnicalException("Failed to create audit document", e);
        }
    }

    private void writeUpdatedFpr(Document auditDoc, boolean changed) throws IOException {
        if (!changed) {
            if (!outputPath.equals(projectPath)) {
                Files.copy(projectPath, outputPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return;
        }

        Path tempFpr = Files.createTempFile("fcli-merge-", ".fpr");
        try {
            try (var zipIn = new ZipFile(projectPath.toFile());
                 OutputStream fos = Files.newOutputStream(tempFpr);
                 var zipOut = new ZipOutputStream(fos)) {

                Enumeration<? extends ZipEntry> entries = zipIn.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if ("audit.xml".equals(entry.getName())) {
                        continue; // replaced below
                    }
                    zipOut.putNextEntry(new ZipEntry(entry.getName()));
                    try (InputStream is = zipIn.getInputStream(entry)) {
                        is.transferTo(zipOut);
                    }
                    zipOut.closeEntry();
                }

                // Write merged audit.xml
                zipOut.putNextEntry(new ZipEntry("audit.xml"));
                var transformer = XmlUtils.secureTransformerFactory().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.transform(new DOMSource(auditDoc), new StreamResult(zipOut));
                zipOut.closeEntry();
            }

            Files.move(tempFpr, outputPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Files.deleteIfExists(tempFpr);
            throw new FcliTechnicalException("Failed to write merged FPR", e);
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
