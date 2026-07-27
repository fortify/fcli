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
package com.fortify.cli.aviator.fpr.processor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.FuzzyContextSearcher;

public class RemediationProcessor {
    Logger logger = LoggerFactory.getLogger(RemediationProcessor.class);
    private static final String NAMESPACE_URI = "xmlns://www.fortify.com/schema/remediations";


    private final FprHandle fprHandle;
    private final String sourceCodeDirectory;
    private final Set<String> issueIdFilter;

    public record RemediationMetric(int totalRemediations, int appliedRemediations, int skippedRemediations,
            Set<String> modifiedFiles, Set<String> requestedIssueIds, Set<String> appliedIssueIds) {
        public RemediationMetric {
            modifiedFiles = immutableCopy(modifiedFiles);
            requestedIssueIds = immutableCopy(requestedIssueIds);
            appliedIssueIds = immutableCopy(appliedIssueIds);
        }

        public static RemediationMetric unfiltered(int totalRemediations, int appliedRemediations, Set<String> modifiedFiles) {
            return new RemediationMetric(totalRemediations, appliedRemediations, totalRemediations - appliedRemediations,
                    modifiedFiles, Set.of(), Set.of());
        }

        public static RemediationMetric filtered(Set<String> requestedIssueIds, Set<String> appliedIssueIds, Set<String> modifiedFiles) {
            int totalRemediations = requestedIssueIds.size();
            int appliedRemediations = appliedIssueIds.size();
            return new RemediationMetric(totalRemediations, appliedRemediations, totalRemediations - appliedRemediations,
                    modifiedFiles, requestedIssueIds, appliedIssueIds);
        }

        public boolean isFiltered() {
            return !requestedIssueIds.isEmpty();
        }

        private static Set<String> immutableCopy(Set<String> values) {
            return values == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(values));
        }
    }

    public RemediationProcessor(FprHandle fprHandle, String sourceCodeDirectory, Set<String> issueIdFilter) {
        this.fprHandle = fprHandle;
        this.sourceCodeDirectory = sourceCodeDirectory;
        this.issueIdFilter = issueIdFilter == null ? null : Collections.unmodifiableSet(new LinkedHashSet<>(issueIdFilter));
    }

    public RemediationMetric processRemediationXML() {
        Path remediationPath = fprHandle.getPath("/remediations.xml");
        try (InputStream remediationStream = Files.newInputStream(remediationPath)) {
            Document remediationDoc = parseRemediationDocument(remediationStream);
            NodeList remediationNodes = remediationDoc.getElementsByTagNameNS(NAMESPACE_URI, "Remediation");
            ProcessingState processingState = new ProcessingState(issueIdFilter);
            Path sourceBasePath = getSourceBasePath();
            for (int i = 0; i < remediationNodes.getLength(); i++) {
                processRemediation((Element) remediationNodes.item(i), sourceBasePath, processingState);
            }
            // Remaining IDs may be absent from remediations.xml or present but not successfully applied.
            for (String unappliedIssueId : processingState.getRequestedButNotApplied()) {
                logger.debug(
                        "Requested issue ID '{}' was not successfully applied (missing from remediations.xml or remediation could not be applied)",
                        unappliedIssueId);
            }
            return processingState.toMetric(remediationNodes.getLength());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.error("Error parsing remediations.xml file: {}", remediationPath, e);
            throw new AviatorTechnicalException("Error processing remediation.xml file.", e);
        } catch (AviatorTechnicalException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error processing remediation.xml: {}", remediationPath, e);
            throw new AviatorTechnicalException("Unexpected error processing remediations.xml.", e);
        }
    }

    private Document parseRemediationDocument(InputStream remediationStream)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(remediationStream);
    }

    private Path getSourceBasePath() {
        String trimmedSourceDir = sourceCodeDirectory.trim();
        if (trimmedSourceDir.length() > 1
                && ((trimmedSourceDir.startsWith("\"") && trimmedSourceDir.endsWith("\""))
                        || (trimmedSourceDir.startsWith("'") && trimmedSourceDir.endsWith("'")))) {
            trimmedSourceDir = trimmedSourceDir.substring(1, trimmedSourceDir.length() - 1);
        }
        return Paths.get(trimmedSourceDir).toAbsolutePath().normalize();
    }

    private void processRemediation(Element remediation, Path sourceBasePath, ProcessingState processingState) throws IOException {
        String instanceId = remediation.getAttribute("instanceId");
        if (!processingState.shouldProcess(instanceId)) {
            return;
        }
        NodeList fileChangesNodes = remediation.getElementsByTagNameNS(NAMESPACE_URI, "FileChanges");
        boolean remediationApplied = false;
        for (int j = 0; j < fileChangesNodes.getLength(); j++) {
            remediationApplied |= processFileChanges((Element) fileChangesNodes.item(j), instanceId, sourceBasePath, processingState.modifiedFiles);
        }
        if (remediationApplied) {
            processingState.recordApplied(instanceId);
        }
    }

    private boolean processFileChanges(Element fileChanges, String instanceId, Path sourceBasePath, Set<String> modifiedFiles) throws IOException {
        String filename = getRequiredChildText(fileChanges, "Filename");
        Path filePath = resolveSourceFilePath(sourceBasePath, filename);
        if (filePath == null) {
            return false;
        }
        String fileHash = getRequiredChildText(fileChanges, "Hash");
        NodeList changesNodes = fileChanges.getElementsByTagNameNS(NAMESPACE_URI, "Change");
        boolean remediationApplied = false;
        for (int k = 0; k < changesNodes.getLength(); k++) {
            remediationApplied |= applyChange((Element) changesNodes.item(k), filePath, filename, fileHash, instanceId, modifiedFiles);
        }
        return remediationApplied;
    }

    private Path resolveSourceFilePath(Path sourceBasePath, String filename) {
        Path filePath = sourceBasePath.resolve(filename).normalize();
        if (!filePath.startsWith(sourceBasePath)) {
            logger.error("Skipping file '{}' as it resolves to a path outside the source directory (potential path traversal attack)", filename);
            return null;
        }
        if (!isFilePresent(filePath)) {
            logger.error("Source code file not present at: {}", filePath);
            throw new AviatorTechnicalException("Source code file not present at: " + filePath);
        }
        return filePath;
    }

    private boolean applyChange(Element change, Path filePath, String filename, String fileHash, String instanceId, Set<String> modifiedFiles)
            throws IOException {
        String content = Files.readString(filePath, StandardCharsets.UTF_8).replace("\r\n", "\n");
        List<String> originalLines = Arrays.asList(content.split("\n"));
        LineRange lineRange = getLineRange(change);
        if (!calculateHashBase64(content, "SHA-256").equals(fileHash)) {
            lineRange = resolveLineRangeFromContext(change, originalLines, filename, instanceId);
            if (lineRange == null) {
                return false;
            }
        }
        writeUpdatedContent(change, filePath, originalLines, lineRange, filename, instanceId, modifiedFiles);
        return true;
    }

    private LineRange getLineRange(Element change) {
        int lineFrom = Integer.parseInt(getRequiredChildText(change, "LineFrom"));
        int lineTo = Integer.parseInt(getRequiredChildText(change, "LineTo"));
        return new LineRange(lineFrom, lineTo);
    }

    private LineRange resolveLineRangeFromContext(Element change, List<String> originalLines, String filename, String instanceId)
            throws IOException {
        logger.trace("File hash mismatch for remediation {} in {}; searching changed source content", instanceId, filename);
        List<String> contextLines = splitLines(getRequiredChildText(change, "Context"));
        int contextLineFrom = FuzzyContextSearcher.fuzzySearchContext(originalLines, contextLines, 0);
        if (contextLineFrom == -1) {
            logger.trace("Context search failed for remediation {} in {}; context lines={}, source lines={}", instanceId, filename,
                    contextLines.size(), originalLines.size());
            logger.info("File content has changed. Context Lines not found. Remediation not possible for {}", instanceId);
            return null;
        }
        logger.trace("Context for remediation {} in {} matched at line {}", instanceId, filename, contextLineFrom + 1);
        List<String> originalCodeLines = splitLines(getRequiredChildText(change, "OriginalCode"));
        int[] lineFromTo = FuzzyContextSearcher.fuzzySearchOriginalCode(originalLines, originalCodeLines, 0, contextLineFrom);
        if (lineFromTo[0] == -1 || lineFromTo[1] == -1) {
            logger.trace("Original code search failed for remediation {} in {}; context line={}, original code lines={}, source lines={}",
                    instanceId, filename, contextLineFrom + 1, originalCodeLines.size(), originalLines.size());
            logger.info("File content has changed. Original Code lines not found. Remediation not possible for {}", instanceId);
            return null;
        }
        int lineFrom = lineFromTo[0] + 1;
        int lineTo = lineFromTo[1] + 1;
        logger.trace("Original code for remediation {} in {} matched at lines {}-{}", instanceId, filename, lineFrom, lineTo);
        return new LineRange(lineFrom, lineTo);
    }

    private void writeUpdatedContent(Element change, Path filePath, List<String> originalLines, LineRange lineRange, String filename,
            String instanceId, Set<String> modifiedFiles) throws IOException {
        List<String> newCodeLines = Arrays.asList(getRequiredChildText(change, "NewCode").split("\n"));
        List<String> updatedLines = new ArrayList<>();
        updatedLines.addAll(originalLines.subList(0, lineRange.lineFrom() - 1));
        updatedLines.addAll(newCodeLines);
        updatedLines.addAll(originalLines.subList(lineRange.lineTo(), originalLines.size()));
        Files.write(filePath, updatedLines);
        modifiedFiles.add(filename);
        logger.info("Remediation applied for {} in file {}", instanceId, filename);
    }

    private List<String> splitLines(String text) {
        return Arrays.asList(text.split("\\r?\\n"));
    }

    private String getRequiredChildText(Element element, String localName) {
        return element.getElementsByTagNameNS(NAMESPACE_URI, localName).item(0).getTextContent();
    }

    private boolean isFilePresent(Path path) {
        return Files.exists(path) && Files.isRegularFile(path);
    }

    private String calculateHashBase64(String content, String algorithm) {
        String hash;
        if (content == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            hash = Base64.getEncoder().encodeToString(digest);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new AviatorTechnicalException("Hashing algorithm not available: " + algorithm, e);
        }
    }

    private record LineRange(int lineFrom, int lineTo) {}

    private static final class ProcessingState {
        private final Set<String> requestedIssueIds;
        private final Set<String> appliedIssueIds = new LinkedHashSet<>();
        private final Set<String> modifiedFiles = new LinkedHashSet<>();
        private int appliedRemediations;

        private ProcessingState(Set<String> requestedIssueIds) {
            this.requestedIssueIds = requestedIssueIds == null ? null : new LinkedHashSet<>(requestedIssueIds);
        }

        private boolean shouldProcess(String instanceId) {
            return requestedIssueIds == null || requestedIssueIds.contains(instanceId);
        }

        private void recordApplied(String instanceId) {
            if (requestedIssueIds == null) {
                appliedRemediations++;
            } else {
                appliedIssueIds.add(instanceId);
            }
        }

        private RemediationMetric toMetric(int totalRemediations) {
            if (requestedIssueIds == null) {
                return RemediationMetric.unfiltered(totalRemediations, appliedRemediations, modifiedFiles);
            }
            return RemediationMetric.filtered(requestedIssueIds, appliedIssueIds, modifiedFiles);
        }

        private Set<String> getRequestedButNotApplied() {
            if (requestedIssueIds == null) {
                return Set.of();
            }
            Set<String> notApplied = new LinkedHashSet<>(requestedIssueIds);
            notApplied.removeAll(appliedIssueIds);
            return notApplied;
        }
    }


}
