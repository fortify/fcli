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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator._common.util.AviatorRemediationMetricsHelper;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.FuzzyContextSearcher;

public class RemediationProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(RemediationProcessor.class);
    private static final String NAMESPACE_URI = "xmlns://www.fortify.com/schema/remediations";

    private final FprHandle fprHandle;
    private final String sourceCodeDirectory;
    private final Set<String> issueIdFilter;

    /**
     * Apply-remediations summary. Mode is explicit: unfiltered counts XML remediations;
     * filtered counts requested issue IDs. Factories are the only public construction path.
     */
    public record RemediationMetric(
            Mode mode,
            int totalRemediations,
            int appliedRemediations,
            int skippedRemediations,
            Set<String> modifiedFiles,
            Map<String, Integer> skippedByReason,
            Set<String> requestedIssueIds,
            Set<String> appliedIssueIds) {

        public enum Mode {
            UNFILTERED,
            FILTERED
        }

        public RemediationMetric {
            if (mode == null) {
                throw new IllegalArgumentException("RemediationMetric mode is required");
            }
            modifiedFiles = immutableCopy(modifiedFiles);
            // Preserve insertion order (LinkedHashMap) for stable skippedReasons table text.
            skippedByReason = skippedByReason == null || skippedByReason.isEmpty()
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(skippedByReason));
            if (mode == Mode.UNFILTERED) {
                requestedIssueIds = Set.of();
                appliedIssueIds = Set.of();
            } else {
                requestedIssueIds = immutableCopy(requestedIssueIds);
                appliedIssueIds = immutableCopy(appliedIssueIds);
            }
        }

        public static RemediationMetric unfiltered(int totalRemediations, int appliedRemediations, Set<String> modifiedFiles) {
            return unfiltered(totalRemediations, appliedRemediations, modifiedFiles, Map.of());
        }

        public static RemediationMetric unfiltered(int totalRemediations, int appliedRemediations, Set<String> modifiedFiles,
                Map<String, Integer> skippedByReason) {
            return new RemediationMetric(Mode.UNFILTERED, totalRemediations, appliedRemediations,
                    totalRemediations - appliedRemediations, modifiedFiles, skippedByReason, Set.of(), Set.of());
        }

        public static RemediationMetric filtered(Set<String> requestedIssueIds, Set<String> appliedIssueIds, Set<String> modifiedFiles) {
            return filtered(requestedIssueIds, appliedIssueIds, modifiedFiles, Map.of());
        }

        public static RemediationMetric filtered(Set<String> requestedIssueIds, Set<String> appliedIssueIds, Set<String> modifiedFiles,
                Map<String, Integer> skippedByReason) {
            Set<String> requested = requestedIssueIds == null ? Set.of() : requestedIssueIds;
            Set<String> applied = appliedIssueIds == null ? Set.of() : appliedIssueIds;
            int totalRemediations = requested.size();
            int appliedRemediations = applied.size();
            return new RemediationMetric(Mode.FILTERED, totalRemediations, appliedRemediations,
                    totalRemediations - appliedRemediations, modifiedFiles, skippedByReason, requested, applied);
        }

        public boolean isFiltered() {
            return mode == Mode.FILTERED;
        }

        private static Set<String> immutableCopy(Set<String> values) {
            return values == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(values));
        }
    }

    private record FvdlMetadataResult(FVDLMetadata metadata, SkipReason skipReason) {}

    private record PendingFileWrite(String filename, Path filePath, String content, byte[] updatedBytes) {}

    private record RollbackFileWrite(String filename, Path filePath, byte[] originalBytes) {}

    private enum SkipReason {
        FVDL_METADATA_UNAVAILABLE("FVDL metadata unavailable"),
        FVDL_ENCODING_MISSING("FVDL source encoding missing"),
        FVDL_ENCODING_UNSUPPORTED("FVDL source encoding unsupported"),
        SOURCE_FILE_MISSING("Source file missing"),
        SOURCE_FILE_OUTSIDE_SOURCE_DIR("Source file outside source directory"),
        SOURCE_READ_FAILED("Source file read failed"),
        SOURCE_DECODE_FAILED("Source file decode failed"),
        REMEDIATION_DATA_INVALID("Remediation data invalid"),
        REMEDIATION_LINE_RANGE_INVALID("Remediation line range invalid"),
        SOURCE_CONTEXT_NOT_FOUND("Source context not found"),
        ORIGINAL_CODE_NOT_FOUND("Original code not found"),
        REMEDIATION_ENCODE_FAILED("Remediation encode failed"),
        SOURCE_WRITE_FAILED("Source file write failed"),
        NO_CHANGES("No file changes found"),
        REQUESTED_ISSUE_NOT_FOUND("Requested issue not found in remediations"),
        UNEXPECTED_ERROR("Unexpected remediation processing error");

        private final String displayName;

        SkipReason(String displayName) {
            this.displayName = displayName;
        }
    }

    private static class SkipRemediationException extends AviatorSimpleException {
        private static final long serialVersionUID = 1L;

        private final SkipReason reason;

        SkipRemediationException(SkipReason reason, String message) {
            super(message);
            this.reason = reason;
        }

        SkipRemediationException(SkipReason reason, String message, Throwable cause) {
            super(message, cause);
            this.reason = reason;
        }
    }

    private static class RemediationCommitException extends AviatorTechnicalException {
        private static final long serialVersionUID = 1L;

        private final List<RollbackFileWrite> rollbacks;

        RemediationCommitException(String message, Throwable cause, List<RollbackFileWrite> rollbacks) {
            super(message, cause);
            this.rollbacks = rollbacks;
        }

        List<RollbackFileWrite> getRollbacks() {
            return rollbacks;
        }
    }

    private static class RollbackRemediationException extends AviatorTechnicalException {
        private static final long serialVersionUID = 1L;

        RollbackRemediationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public RemediationProcessor(FprHandle fprHandle, String sourceCodeDirectory) {
        this(fprHandle, sourceCodeDirectory, null);
    }

    public RemediationProcessor(FprHandle fprHandle, String sourceCodeDirectory, Set<String> issueIdFilter) {
        this.fprHandle = fprHandle;
        this.sourceCodeDirectory = sourceCodeDirectory;
        this.issueIdFilter = issueIdFilter == null ? null : Collections.unmodifiableSet(new LinkedHashSet<>(issueIdFilter));
    }

    public RemediationMetric processRemediationXML() {
        Path remediationPath = fprHandle.getPath("/remediations.xml");
        Path sourceBasePath = getSourceBasePath();
        LOG.debug("Applying remediations from {} to source directory {}", remediationPath, sourceBasePath);
        FvdlMetadataResult fvdlMetadataResult = loadFvdlMetadata();
        ProcessingState state = new ProcessingState(issueIdFilter);

        try (InputStream remediationStream = Files.newInputStream(remediationPath)) {
            Document remediationDoc = parseRemediationDocument(remediationStream);
            NodeList remediationNodes = remediationDoc.getElementsByTagNameNS(NAMESPACE_URI, "Remediation");
            state.setXmlEntryCount(remediationNodes.getLength());
            LOG.debug("Loaded {} remediation entries from {}", remediationNodes.getLength(), remediationPath);
            for (int i = 0; i < remediationNodes.getLength(); i++) {
                Element remediation = (Element) remediationNodes.item(i);
                String instanceId = remediation.getAttribute("instanceId");
                if (!state.shouldProcess(instanceId)) {
                    continue;
                }
                state.markSeen(instanceId);
                if (processRemediation(remediation, sourceBasePath, fvdlMetadataResult, state)) {
                    state.recordApplied(instanceId);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOG.error("Error parsing remediations.xml file: {}", remediationPath, e);
            throw new AviatorTechnicalException("Error processing remediation.xml file.", e);
        } catch (AviatorTechnicalException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Unexpected error processing remediation.xml: {}", remediationPath, e);
            throw new AviatorTechnicalException("Unexpected error processing remediations.xml.", e);
        }

        RemediationMetric metric = state.toMetric();
        logSummary(metric);
        return metric;
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

    private Document parseRemediationDocument(InputStream remediationStream)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(remediationStream);
    }

    private void logSummary(RemediationMetric metric) {
        String label = metric.isFiltered() ? "Auto-remediation summary (filtered)" : "Auto-remediation summary";
        LOG.info("{}: total={}, applied={}, skipped={}", label, metric.totalRemediations(), metric.appliedRemediations(),
                metric.skippedRemediations());
        if (!metric.skippedByReason().isEmpty()) {
            LOG.info("Skipped remediations by reason: {}",
                    AviatorRemediationMetricsHelper.formatSkippedReasons(metric.skippedByReason()));
        }
    }

    /**
     * Owns filter accounting, skip reasons, modified files, and metric construction so
     * {@link #processRemediationXML()} has a single exit path.
     */
    private static final class ProcessingState {
        /** Null means unfiltered (all XML remediations). Non-null means FILTERED mode. */
        private final Set<String> requestedIssueIds;
        private final Set<String> appliedIssueIds = new LinkedHashSet<>();
        private final Set<String> seenRequestedIssueIds = new LinkedHashSet<>();
        private final Set<String> modifiedFiles = new LinkedHashSet<>();
        private final Map<String, Integer> skippedByReason = new LinkedHashMap<>();
        private int xmlEntryCount;
        private int appliedRemediations;

        private ProcessingState(Set<String> issueIdFilter) {
            this.requestedIssueIds = issueIdFilter == null ? null : new LinkedHashSet<>(issueIdFilter);
        }

        private void setXmlEntryCount(int xmlEntryCount) {
            this.xmlEntryCount = xmlEntryCount;
        }

        private boolean shouldProcess(String instanceId) {
            return requestedIssueIds == null || requestedIssueIds.contains(instanceId);
        }

        private void markSeen(String instanceId) {
            if (requestedIssueIds != null) {
                seenRequestedIssueIds.add(instanceId);
            }
        }

        private void recordApplied(String instanceId) {
            appliedRemediations++;
            if (requestedIssueIds != null) {
                appliedIssueIds.add(instanceId);
            }
        }

        private void recordSkip(SkipReason reason) {
            skippedByReason.merge(reason.displayName, 1, Integer::sum);
        }

        private RemediationMetric toMetric() {
            if (requestedIssueIds == null) {
                return RemediationMetric.unfiltered(xmlEntryCount, appliedRemediations, modifiedFiles, skippedByReason);
            }
            for (String requestedId : requestedIssueIds) {
                if (appliedIssueIds.contains(requestedId)) {
                    continue;
                }
                if (!seenRequestedIssueIds.contains(requestedId)) {
                    recordSkip(SkipReason.REQUESTED_ISSUE_NOT_FOUND);
                    LOG.debug("Requested issue ID '{}' was not found in remediations.xml", requestedId);
                } else {
                    LOG.debug("Requested issue ID '{}' was present in remediations.xml but could not be applied",
                            requestedId);
                }
            }
            return RemediationMetric.filtered(requestedIssueIds, appliedIssueIds, modifiedFiles, skippedByReason);
        }
    }

    private boolean processRemediation(
            Element remediation, Path sourceBasePath, FvdlMetadataResult fvdlMetadataResult, ProcessingState state) {
        String instanceId = remediation.getAttribute("instanceId");
        try {
            Map<Path, PendingFileWrite> pendingWrites = prepareFileChanges(remediation, sourceBasePath, fvdlMetadataResult);
            if (pendingWrites.isEmpty()) {
                state.recordSkip(SkipReason.NO_CHANGES);
                return false;
            }
            try {
                commitRemediationWrites(instanceId, pendingWrites, state.modifiedFiles);
                return true;
            } catch (RemediationCommitException e) {
                rollbackRemediationWrites(instanceId, e.getRollbacks());
                throw new SkipRemediationException(SkipReason.SOURCE_WRITE_FAILED, e.getMessage(), e);
            }
        } catch (SkipRemediationException e) {
            state.recordSkip(e.reason);
            LOG.info("Skipping remediation {}: {}", instanceId, e.getMessage());
            LOG.debug("Skip reason for remediation {}: {}", instanceId, e.reason.displayName, e);
            return false;
        } catch (RollbackRemediationException e) {
            throw e;
        } catch (Exception e) {
            state.recordSkip(SkipReason.UNEXPECTED_ERROR);
            LOG.info("Skipping remediation {} due to an unexpected processing error", instanceId);
            LOG.debug("Unexpected error while processing remediation {}", instanceId, e);
            return false;
        }
    }

    private Map<Path, PendingFileWrite> prepareFileChanges(Element remediation, Path sourceBasePath,
            FvdlMetadataResult fvdlMetadataResult) {
        NodeList fileChangesNodes = remediation.getElementsByTagNameNS(NAMESPACE_URI, "FileChanges");
        if (fileChangesNodes.getLength() == 0) {
            throw new SkipRemediationException(SkipReason.NO_CHANGES, "No file changes found");
        }

        Map<Path, PendingFileWrite> pendingWrites = new LinkedHashMap<>();
        for (int j = 0; j < fileChangesNodes.getLength(); j++) {
            processFileChanges(remediation, (Element) fileChangesNodes.item(j), sourceBasePath, fvdlMetadataResult, pendingWrites);
        }
        return pendingWrites;
    }

    private void processFileChanges(Element remediation, Element fileChanges, Path sourceBasePath, FvdlMetadataResult fvdlMetadataResult,
            Map<Path, PendingFileWrite> pendingWrites) {
        String instanceId = remediation.getAttribute("instanceId");
        String filename = getRequiredElementText(fileChanges, "Filename");
        Path filePath = sourceBasePath.resolve(filename).normalize();
        LOG.debug("Processing remediation {} file change for '{}' resolved to '{}'", instanceId, filename, filePath);

        if (!filePath.startsWith(sourceBasePath)) {
            throw new SkipRemediationException(SkipReason.SOURCE_FILE_OUTSIDE_SOURCE_DIR,
                    "Source file resolves outside source directory: " + filename);
        }

        if (!isFilePresent(filePath)) {
            throw new SkipRemediationException(SkipReason.SOURCE_FILE_MISSING, "Source code file not present at: " + filePath);
        }

        String fileHash = getRequiredElementText(fileChanges, "Hash");
        Charset sourceEncoding = getRequiredSourceEncoding(filename, fvdlMetadataResult);
        NodeList changesNodes = fileChanges.getElementsByTagNameNS(NAMESPACE_URI, "Change");
        if (changesNodes.getLength() == 0) {
            throw new SkipRemediationException(SkipReason.NO_CHANGES, "No changes found for file: " + filename);
        }
        LOG.debug("Remediation {} has {} change(s) for '{}' using FVDL encoding {}", instanceId, changesNodes.getLength(), filename,
                sourceEncoding.name());

        String updatedContent = getPendingOrSourceContent(filePath, filename, sourceEncoding, pendingWrites);
        for (int k = 0; k < changesNodes.getLength(); k++) {
            updatedContent = applyChange(instanceId, filename, fileHash, sourceEncoding, updatedContent,
                    (Element) changesNodes.item(k), k + 1);
        }
        byte[] updatedBytes = encodeStrict(updatedContent, sourceEncoding, filename);
        pendingWrites.put(filePath, new PendingFileWrite(filename, filePath, updatedContent, updatedBytes));
        LOG.debug("Staged remediation {} for '{}' using FVDL encoding {}; changes={}, encodedBytes={}", instanceId, filename,
                sourceEncoding.name(), changesNodes.getLength(), updatedBytes.length);
    }

    private String applyChange(String instanceId, String filename, String fileHash, Charset sourceEncoding, String originalContent,
            Element change, int changeIndex) {
        String lineSeparator = detectLineSeparator(originalContent);
        String content = normalizeLineEndings(originalContent);

        List<String> originalLines = Arrays.asList(content.split("\n", -1));
        LOG.debug("Decoded '{}' using {}; lineSeparator={}, normalizedLines={}", filename, sourceEncoding.name(),
                describeLineSeparator(lineSeparator), originalLines.size());

        int lineFrom = parseRequiredInt(change, "LineFrom");
        int lineTo = parseRequiredInt(change, "LineTo");
        LOG.debug("Remediation {} change {} for '{}' targets lines {}-{}", instanceId, changeIndex, filename, lineFrom, lineTo);

        String calculatedHash = calculateHashBase64(content, "SHA-256");
        boolean fileHashMatches = calculatedHash.equals(fileHash);
        LOG.debug("Remediation {} hash check for '{}': {}", instanceId, filename, fileHashMatches ? "matched" : "mismatched");
        if (!fileHashMatches) {
            LOG.debug("File hash mismatch for remediation {} in {}; searching changed source content", instanceId, filename);
            String contextText = getRequiredElementText(change, "Context");
            List<String> contextLine = Arrays.asList(contextText.split("\\r?\\n"));
            int contextLineFrom = fuzzySearchContext(instanceId, filename, originalLines, contextLine);
            if (contextLineFrom == -1) {
                LOG.debug("Context search failed for remediation {} in {}; context lines={}, source lines={}", instanceId, filename,
                        contextLine.size(), originalLines.size());
                throw new SkipRemediationException(SkipReason.SOURCE_CONTEXT_NOT_FOUND, "Source context not found for file '" + filename +
                    "'; file may have changed or remediation may overlap a previous change");
            }
            LOG.debug("Context for remediation {} in {} matched at line {}", instanceId, filename, contextLineFrom + 1);

            String originalCodeText = getRequiredElementText(change, "OriginalCode");
            List<String> originalCodeLine = Arrays.asList(originalCodeText.split("\\r?\\n"));
            int[] lineFromTo = fuzzySearchOriginalCode(instanceId, filename, originalLines, originalCodeLine, contextLineFrom);
            if (lineFromTo[0] == -1 || lineFromTo[1] == -1) {
                LOG.debug("Original code search failed for remediation {} in {}; context line={}, original code lines={}, source lines={}",
                        instanceId, filename, contextLineFrom + 1, originalCodeLine.size(), originalLines.size());
                throw new SkipRemediationException(SkipReason.ORIGINAL_CODE_NOT_FOUND, "Original code not found for file '" + filename +
                    "'; file may have changed or remediation may overlap a previous change");
            }
            lineFrom = lineFromTo[0] + 1;
            lineTo = lineFromTo[1] + 1;
            LOG.debug("Original code for remediation {} in {} matched at lines {}-{}", instanceId, filename, lineFrom, lineTo);
        }

        validateLineRange(lineFrom, lineTo, originalLines.size(), filename);
        List<String> newCodeLines = Arrays.asList(getRequiredElementText(change, "NewCode").split("\\r?\\n"));
        List<String> updatedLines = new ArrayList<>();
        updatedLines.addAll(originalLines.subList(0, lineFrom - 1));
        updatedLines.addAll(newCodeLines);
        updatedLines.addAll(originalLines.subList(lineTo, originalLines.size()));
        LOG.debug("Staged remediation {} change {} for '{}' using FVDL encoding {}; updatedLines={}", instanceId, changeIndex,
                filename, sourceEncoding.name(), updatedLines.size());
        return String.join(lineSeparator, updatedLines);
    }

    private String getPendingOrSourceContent(Path filePath, String filename, Charset sourceEncoding,
            Map<Path, PendingFileWrite> pendingWrites) {
        PendingFileWrite pendingWrite = pendingWrites.get(filePath);
        return pendingWrite == null ? readSourceFile(filePath, filename, sourceEncoding) : pendingWrite.content();
    }

    private void commitRemediationWrites(String instanceId, Map<Path, PendingFileWrite> pendingWrites, Set<String> modifiedFiles)
            throws RemediationCommitException {
        List<RollbackFileWrite> rollbacks = new ArrayList<>();
        for (PendingFileWrite pendingWrite : pendingWrites.values()) {
            try {
                byte[] originalBytes = Files.readAllBytes(pendingWrite.filePath());
                rollbacks.add(new RollbackFileWrite(pendingWrite.filename(), pendingWrite.filePath(), originalBytes));
                LOG.debug("Writing remediation {} to '{}' using staged bytes; encodedBytes={}", instanceId, pendingWrite.filename(),
                        pendingWrite.updatedBytes().length);
                Files.write(pendingWrite.filePath(), pendingWrite.updatedBytes());
            } catch (Exception e) {
                throw new RemediationCommitException("Error writing source code file '" + pendingWrite.filename() + "'", e, rollbacks);
            }
        }

        for (PendingFileWrite pendingWrite : pendingWrites.values()) {
            modifiedFiles.add(pendingWrite.filename());
            LOG.info("Remediation applied for {} in file {}", instanceId, pendingWrite.filename());
        }
    }

    private void rollbackRemediationWrites(String instanceId, List<RollbackFileWrite> rollbacks) {
        for (RollbackFileWrite rollback : rollbacks) {
            try {
                Files.write(rollback.filePath(), rollback.originalBytes());
                LOG.warn("Rolled back remediation {} changes for '{}' after write failure", instanceId, rollback.filename());
            } catch (IOException rollbackException) {
                LOG.error("Failed to roll back remediation {} changes for '{}'", instanceId, rollback.filename(), rollbackException);
                throw new RollbackRemediationException("Failed to roll back remediation changes for '" + rollback.filename() +
                        "'. Source files may be partially modified; inspect the source tree before retrying", rollbackException);
            }
        }
    }

    private int fuzzySearchContext(String instanceId, String filename, List<String> originalLines, List<String> contextLine) {
        try {
            return FuzzyContextSearcher.fuzzySearchContext(originalLines, contextLine, 0);
        } catch (IOException e) {
            throw new SkipRemediationException(SkipReason.SOURCE_CONTEXT_NOT_FOUND,
                    "Error searching source context for remediation '" + instanceId + "' in file '" + filename + "'", e);
        }
    }

    private int[] fuzzySearchOriginalCode(String instanceId, String filename, List<String> originalLines, List<String> originalCodeLine,
            int contextLineFrom) {
        return FuzzyContextSearcher.fuzzySearchOriginalCode(originalLines, originalCodeLine, 0, contextLineFrom);
    }

    private boolean isFilePresent(Path path) {
        return Files.exists(path) && Files.isRegularFile(path);
    }

    private FvdlMetadataResult loadFvdlMetadata() {
        if (!Files.exists(fprHandle.getPath("/audit.fvdl"))) {
            LOG.warn("FVDL file '/audit.fvdl' is missing; source remediations will be skipped");
            return new FvdlMetadataResult(null, SkipReason.FVDL_METADATA_UNAVAILABLE);
        }

        try (ZipFile zipFile = new ZipFile(fprHandle.getFprPath().toFile())) {
            LOG.debug("Loading FVDL build metadata from '{}' to resolve source encodings", fprHandle.getFprPath());
            StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle);
            processor.parseBuildMetadata(zipFile, "audit.fvdl");
            LOG.debug("Loaded FVDL build metadata from '{}'", fprHandle.getFprPath());
            return new FvdlMetadataResult(processor.getFvdlMetadata(), null);
        } catch (Exception e) {
            LOG.warn("Error reading source file encodings from audit.fvdl; source remediations will be skipped", e);
            return new FvdlMetadataResult(null, SkipReason.FVDL_METADATA_UNAVAILABLE);
        }
    }

    private Charset getRequiredSourceEncoding(String filename, FvdlMetadataResult fvdlMetadataResult) {
        if (fvdlMetadataResult.skipReason() != null || fvdlMetadataResult.metadata() == null) {
            throw new SkipRemediationException(SkipReason.FVDL_METADATA_UNAVAILABLE,
                    "FVDL metadata is unavailable; cannot determine source encoding for file '" + filename + "'");
        }

        String encoding = fvdlMetadataResult.metadata().findSourceFileEncodingForFileName(filename);
        if (encoding == null || encoding.isBlank()) {
            LOG.debug("FVDL source encoding lookup failed for '{}'", filename);
            throw new SkipRemediationException(SkipReason.FVDL_ENCODING_MISSING,
                    "FVDL does not declare a source encoding for file '" + filename + "'");
        }

        try {
            Charset charset = Charset.forName(encoding);
            LOG.debug("FVDL source encoding for '{}' resolved to '{}'", filename, charset.name());
            return charset;
        } catch (Exception e) {
            throw new SkipRemediationException(SkipReason.FVDL_ENCODING_UNSUPPORTED,
                    "FVDL declares unsupported source encoding '" + encoding + "' for file '" + filename + "'", e);
        }
    }

    private String readSourceFile(Path filePath, String filename, Charset sourceEncoding) {
        try {
            byte[] sourceBytes = Files.readAllBytes(filePath);
            String decodedContent = decodeStrict(sourceBytes, sourceEncoding);
            LOG.debug("Strict decoded '{}' using {}; sourceBytes={}, decodedChars={}", filename, sourceEncoding.name(), sourceBytes.length,
                    decodedContent.length());
            return decodedContent;
        } catch (CharacterCodingException e) {
            throw new SkipRemediationException(SkipReason.SOURCE_DECODE_FAILED,
                    "FVDL declares source encoding '" + sourceEncoding.name() + "' for file '" + filename +
                            "', but the source file cannot be decoded using that encoding", e);
        } catch (IOException e) {
            throw new SkipRemediationException(SkipReason.SOURCE_READ_FAILED, "Error reading source code file '" + filePath + "'", e);
        }
    }

    private String decodeStrict(byte[] bytes, Charset charset) throws CharacterCodingException {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private byte[] encodeStrict(String content, Charset charset, String filename) {
        try {
            ByteBuffer buffer = charset.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(content));
            byte[] result = new byte[buffer.remaining()];
            buffer.get(result);
            return result;
        } catch (CharacterCodingException e) {
            throw new SkipRemediationException(SkipReason.REMEDIATION_ENCODE_FAILED,
                    "Remediation content for file '" + filename + "' cannot be encoded using FVDL source encoding '" +
                            charset.name() + "'", e);
        }
    }

    private String getRequiredElementText(Element parent, String elementName) {
        NodeList nodes = parent.getElementsByTagNameNS(NAMESPACE_URI, elementName);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            throw new SkipRemediationException(SkipReason.REMEDIATION_DATA_INVALID,
                    "Missing required remediation element '" + elementName + "'");
        }
        return nodes.item(0).getTextContent();
    }

    private int parseRequiredInt(Element parent, String elementName) {
        String value = getRequiredElementText(parent, elementName);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new SkipRemediationException(SkipReason.REMEDIATION_DATA_INVALID,
                    "Invalid integer value for remediation element '" + elementName + "': " + value, e);
        }
    }

    private void validateLineRange(int lineFrom, int lineTo, int sourceLineCount, String filename) {
        if (lineFrom < 1 || lineTo < lineFrom || lineTo > sourceLineCount) {
            throw new SkipRemediationException(SkipReason.REMEDIATION_LINE_RANGE_INVALID,
                    "Invalid remediation line range " + lineFrom + "-" + lineTo + " for file '" + filename + "'");
        }
    }

    private String detectLineSeparator(String content) {
        int crlfIndex = content.indexOf("\r\n");
        int lfIndex = content.indexOf('\n');
        int crIndex = content.indexOf('\r');

        if (crlfIndex >= 0 && (lfIndex == crlfIndex + 1 || lfIndex < 0) && (crIndex == crlfIndex || crIndex < 0)) {
            return "\r\n";
        }
        if (lfIndex >= 0 && (crIndex < 0 || lfIndex < crIndex)) {
            return "\n";
        }
        if (crIndex >= 0) {
            return "\r";
        }
        return System.lineSeparator();
    }

    private String normalizeLineEndings(String content) {
        return content.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String describeLineSeparator(String lineSeparator) {
        return switch (lineSeparator) {
            case "\r\n" -> "CRLF";
            case "\n" -> "LF";
            case "\r" -> "CR";
            default -> "system";
        };
    }

    private String calculateHashBase64(String content, String algorithm) {
        String hash;
        if (content == null) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            hash = Base64.getEncoder().encodeToString(digest);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new AviatorTechnicalException("Hashing algorithm not available: " + algorithm, e);
        }
    }

}
