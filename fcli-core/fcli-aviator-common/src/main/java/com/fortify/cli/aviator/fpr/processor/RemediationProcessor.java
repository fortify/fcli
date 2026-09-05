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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
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
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder.DecodeResult;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder.SourceDecodeException;
import com.fortify.cli.aviator.fpr.utils.SourceDecoders;
import com.fortify.cli.aviator.fpr.utils.SourceEncoder;
import com.fortify.cli.aviator.fpr.utils.SourceEncoder.SourceEncodeException;
import com.fortify.cli.aviator.util.*;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.FuzzyContextSearcher;

public class RemediationProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(RemediationProcessor.class);
    private static final String NAMESPACE_URI = "xmlns://www.fortify.com/schema/remediations";

    private final FprHandle fprHandle;
    private final String sourceCodeDirectory;
    private final ISourceDecoder sourceDecoder;

    public record RemediationMetric(int totalRemediations, int appliedRemediations,     int identicalRemediations,int skippedRemediations, Set<String> modifiedFiles,
                                    Map<String, Integer> skippedByReason) {
        public RemediationMetric(int totalRemediations, int appliedRemediations,int identicalRemediations,int  skippedRemediations, Set<String> modifiedFiles) {
            this(totalRemediations, appliedRemediations,identicalRemediations, skippedRemediations, modifiedFiles, Map.of());
        }
    }

    private record RemediationKey(String fileName, Path filePath,int lineFrom,int lineTo,String comparisonCode){}

    private record SourceFileContent(String content, Charset charset, String encodingSource) {}

    private record PendingFileWrite(String filename, Path filePath, String content, Charset charset, String encodingSource,
                                    byte[] updatedBytes) {}

    private record RollbackFileWrite(String filename, Path filePath, byte[] originalBytes) {}

    private enum SkipReason {
        SOURCE_FILE_MISSING("Source file missing"),
        SOURCE_FILE_OUTSIDE_SOURCE_DIR("Source file outside source directory"),
        SOURCE_READ_FAILED("Source file read failed"),
        SOURCE_DECODE_FAILED("Source file decode failed"),
        REMEDIATION_DATA_INVALID("Remediation data invalid"),
        REMEDIATION_LINE_RANGE_INVALID("Remediation line range invalid"),
        SOURCE_CONTEXT_NOT_FOUND("Source context not found"),
        SOURCE_CONTEXT_AMBIGUOUS("Source context matched multiple locations"),
        ORIGINAL_CODE_NOT_FOUND("Original code not found"),
        REMEDIATION_ENCODE_FAILED("Remediation encode failed"),
        SOURCE_WRITE_FAILED("Source file write failed"),
        NO_CHANGES("No file changes found"),
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
        this(fprHandle, sourceCodeDirectory, SourceDecoders.defaults());
    }

    public RemediationProcessor(FprHandle fprHandle, String sourceCodeDirectory, ISourceDecoder sourceDecoder) {
        this.fprHandle = fprHandle;
        this.sourceCodeDirectory = sourceCodeDirectory;
        this.sourceDecoder = Objects.requireNonNull(sourceDecoder, "sourceDecoder");
    }

    public RemediationMetric processRemediationXML() {
        Path remediationPath = fprHandle.getPath("/remediations.xml");
        Document remediationDoc;
        int totalRemediations;
        int appliedRemediations;
        int identicalRemediations = 0;
        Set<String> modifiedFiles = new LinkedHashSet<>();
        Map<String, Integer> skippedByReason = new LinkedHashMap<>();
        Map<RemediationKey, String> remediationLookup = new LinkedHashMap<>();

        // Sanitize and normalize the base source directory path once.
        String trimmedSourceDir = sourceCodeDirectory.trim();
        if (trimmedSourceDir.length() > 1 &&
            ((trimmedSourceDir.startsWith("\"") && trimmedSourceDir.endsWith("\"")) ||
             (trimmedSourceDir.startsWith("'") && trimmedSourceDir.endsWith("'")))) {
            trimmedSourceDir = trimmedSourceDir.substring(1, trimmedSourceDir.length() - 1);
        }
        final Path sourceBasePath = Paths.get(trimmedSourceDir).toAbsolutePath().normalize();
        LOG.debug("Applying remediations from {} to source directory {}", remediationPath, sourceBasePath);
        final FVDLMetadata fvdlMetadata = loadFvdlMetadata();

        try (InputStream remediationStream = Files.newInputStream(remediationPath)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            remediationDoc = builder.parse(remediationStream);

            NodeList remediationNodes = remediationDoc.getElementsByTagNameNS(NAMESPACE_URI, "Remediation");
            totalRemediations = remediationNodes.getLength();
            LOG.debug("Loaded {} remediation entries from {}", totalRemediations, remediationPath);
            appliedRemediations = 0;
            for (int i = 0; i < remediationNodes.getLength(); i++) {
                Element remediation = (Element) remediationNodes.item(i);
                String instanceId =  remediation.getAttribute("instanceId");
                List<RemediationKey> remediationKeys = createRemediationKeys(remediation, sourceBasePath);

                // P2.3 hunk-level identity: partition keys into already-satisfied vs to-apply.
                Set<RemediationKey> satisfiedKeys = new LinkedHashSet<>();
                Set<RemediationKey> toApplyKeys = new LinkedHashSet<>();
                Set<String> satisfiedByInstances = new LinkedHashSet<>();
                for (RemediationKey key : remediationKeys) {
                    String owner = remediationLookup.get(key);
                    if (owner != null) {
                        satisfiedKeys.add(key);
                        satisfiedByInstances.add(owner);
                    } else {
                        toApplyKeys.add(key);
                    }
                }

                // Fully identical: every hunk was already applied by an earlier remediation.
                if (!remediationKeys.isEmpty() && toApplyKeys.isEmpty()) {
                    identicalRemediations++;
                    LOG.info("Remediation {} is fully identical to prior remediation(s) {}; {} hunk(s) already applied",
                        instanceId, satisfiedByInstances, satisfiedKeys.size());
                    continue;
                }

                // Partial identity: some hunks already applied; apply only the rest.
                if (!satisfiedKeys.isEmpty()) {
                    LOG.info("Remediation {} is partially identical to prior remediation(s) {}; {} of {} hunk(s) already applied, {} still to apply",
                        instanceId, satisfiedByInstances, satisfiedKeys.size(), remediationKeys.size(), toApplyKeys.size());
                }

                Set<RemediationKey> filter = satisfiedKeys.isEmpty() ? null : toApplyKeys;
                if (processRemediation(remediation, sourceBasePath, fvdlMetadata, modifiedFiles, skippedByReason, filter)) {
                    appliedRemediations++;
                    for (RemediationKey key : toApplyKeys) {
                        LOG.debug("putting {}", instanceId);
                        remediationLookup.put(key, instanceId);
                    }
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
        int skippedRemediations = totalRemediations - appliedRemediations - identicalRemediations;
        LOG.info("Auto-remediation summary: total={}, applied={},indentical={},skipped={}", totalRemediations, appliedRemediations, identicalRemediations,skippedRemediations);
        if (!skippedByReason.isEmpty()) {
            LOG.info("Skipped remediations by reason: {}", formatSkippedReasons(skippedByReason));
        }
        return new RemediationMetric(totalRemediations, appliedRemediations, identicalRemediations, skippedRemediations, modifiedFiles, skippedByReason);
    }

    private boolean processRemediation(Element remediation, Path sourceBasePath, FVDLMetadata fvdlMetadata,
                                       Set<String> modifiedFiles, Map<String, Integer> skippedByReason, Set<RemediationKey> keysToApply) {
        String instanceId = remediation.getAttribute("instanceId");
        try {
            Map<Path, PendingFileWrite> pendingWrites = prepareFileChanges(remediation, sourceBasePath, fvdlMetadata, keysToApply);

            if (pendingWrites.isEmpty()) {
                recordSkipped(skippedByReason, SkipReason.NO_CHANGES.displayName);
                return false;
            }
            try {
                commitRemediationWrites(instanceId, pendingWrites, modifiedFiles);
                return true;
            } catch (RemediationCommitException e) {
                rollbackRemediationWrites(instanceId, e.getRollbacks());
                throw new SkipRemediationException(SkipReason.SOURCE_WRITE_FAILED, e.getMessage(), e);
            }
        } catch (SkipRemediationException e) {
            recordSkipped(skippedByReason, skipReasonLabel(e));
            LOG.warn("Skipping remediation {}: {}", instanceId, e.getMessage());
            LOG.debug("Skip reason for remediation {}: {}", instanceId, e.reason.displayName, e);
            return false;
        } catch (RollbackRemediationException e) {
                throw e;
        } catch (Exception e) {
            recordSkipped(skippedByReason, SkipReason.UNEXPECTED_ERROR.displayName);
            LOG.warn("Skipping remediation {} due to an unexpected processing error", instanceId);
            LOG.debug("Unexpected error while processing remediation {}", instanceId, e);
            return false;
        }
    }

    private Map<Path, PendingFileWrite> prepareFileChanges(Element remediation, Path sourceBasePath,
                                                           FVDLMetadata fvdlMetadata, Set<RemediationKey> keysToApply) {
        NodeList fileChangesNodes = remediation.getElementsByTagNameNS(NAMESPACE_URI, "FileChanges");
        if (fileChangesNodes.getLength() == 0) {
            throw new SkipRemediationException(SkipReason.NO_CHANGES, "No file changes found");
        }

        Map<Path, PendingFileWrite> pendingWrites = new LinkedHashMap<>();
        for (int j = 0; j < fileChangesNodes.getLength(); j++) {
            processFileChanges(remediation, (Element) fileChangesNodes.item(j), sourceBasePath, fvdlMetadata, pendingWrites, keysToApply);
        }
        return pendingWrites;
    }

    private boolean processFileChanges(Element remediation, Element fileChanges, Path sourceBasePath, FVDLMetadata fvdlMetadata,
                                       Map<Path, PendingFileWrite> pendingWrites, Set<RemediationKey> keysToApply) {

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
        NodeList changesNodes = fileChanges.getElementsByTagNameNS(NAMESPACE_URI, "Change");
        if (changesNodes.getLength() == 0) {
            throw new SkipRemediationException(SkipReason.NO_CHANGES, "No changes found for file: " + filename);
        }
        SourceFileContent sourceFileContent = getPendingOrSourceContent(filePath, filename, fvdlMetadata, pendingWrites);
        Charset sourceEncoding = sourceFileContent.charset();
        LOG.debug("Remediation {} has {} change(s) for '{}' using source encoding {}", instanceId, changesNodes.getLength(), filename,
            sourceFileContent.encodingSource());

        String updatedContent = sourceFileContent.content();
        int appliedInThisFile = 0;
        int skippedAlreadySatisfied = 0;
        for (int k = 0; k < changesNodes.getLength(); k++) {
            Element changeElement = (Element) changesNodes.item(k);
            if (keysToApply != null) {
                String newCode = getRequiredElementText(changeElement, "NewCode");
                String normalizedCode = normalizeProposedCode(newCode, filename);
                String comparisonCode = createComparisonCode(normalizedCode, filename);
                RemediationKey key = createRemediationKey(fileChanges, changeElement, sourceBasePath, comparisonCode);
                if (!keysToApply.contains(key)) {
                    LOG.info("Skipping hunk {} of remediation {} in '{}': already applied by prior identical hunk",
                        k + 1, instanceId, filename);
                    skippedAlreadySatisfied++;
                    continue;
                }
            }
            updatedContent = applyChange(instanceId, filename, fileHash, sourceEncoding, updatedContent,
                changeElement, k + 1);
            appliedInThisFile++;
        }
        if (appliedInThisFile == 0) {
            LOG.debug("Remediation {} produced no new hunks for '{}' ({} already satisfied); no write staged",
                instanceId, filename, skippedAlreadySatisfied);
            return true;
        }
        byte[] updatedBytes = encodeSourceFile(updatedContent, sourceEncoding, filename);

        pendingWrites.put(filePath, new PendingFileWrite(filename, filePath, updatedContent, sourceEncoding,
            sourceFileContent.encodingSource(), updatedBytes));
        LOG.debug("Staged remediation {} for '{}' using source encoding {}; changes={}, encodedBytes={}", instanceId, filename,
            sourceFileContent.encodingSource(), changesNodes.getLength(), updatedBytes.length);
        return true;
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
            Element contextElement = getRequiredElement(change, "Context");
            String contextText = contextElement.getTextContent();
            List<String> contextLine = Arrays.asList(contextText.split("\\r?\\n"));
            int contextLineFrom = fuzzySearchContext(instanceId, filename, originalLines, contextLine);
            if (contextLineFrom == -1) {
                LOG.debug("Context search failed for remediation {} in {}; trying whole-file OriginalCode fallback",
                    instanceId, filename);
                String fallbackOriginalCodeText = getRequiredElementText(change, "OriginalCode");
                List<String> fallbackOriginalCodeLine = Arrays.asList(fallbackOriginalCodeText.split("\\r?\\n"));
                int[] wholeFile = fuzzySearchOriginalCode(instanceId, filename, originalLines, fallbackOriginalCodeLine,
                    0, originalLines.size(), 0, 0);
                if (wholeFile[0] != -1 && wholeFile[1] != -1) {
                    LOG.debug("Whole-file OriginalCode fallback matched remediation {} in {} at lines {}-{}",
                        instanceId, filename, wholeFile[0] + 1, wholeFile[1] + 1);
                    lineFrom = wholeFile[0] + 1;
                    lineTo = wholeFile[1] + 1;
                } else {
                    LOG.debug("Whole-file OriginalCode fallback failed for remediation {} in {}", instanceId, filename);
                    throw new SkipRemediationException(SkipReason.SOURCE_CONTEXT_NOT_FOUND, "Source context not found for file '" + filename +
                        "'; file may have changed or remediation may overlap a previous change");
                }
            } else {
                LOG.debug("Context for remediation {} in {} matched at line {}", instanceId, filename, contextLineFrom + 1);

                String originalCodeText = getRequiredElementText(change, "OriginalCode");
                List<String> originalCodeLine = Arrays.asList(originalCodeText.split("\\r?\\n"));
                int contextBefore = parseRequiredContextAttribute(contextElement, "before");
                int contextAfter = parseRequiredContextAttribute(contextElement, "after");
                int[] lineFromTo = fuzzySearchOriginalCode(instanceId, filename, originalLines, originalCodeLine,
                    contextLineFrom, contextLine.size(), contextBefore, contextAfter);
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
        }


        validateLineRange(lineFrom, lineTo, originalLines.size(), filename);
        List<String> newCodeLines = Arrays.asList(FileUtil.stripSyntheticLineMarkers(
            getRequiredElementText(change, "NewCode"), filename).split("\n"));
        List<String> updatedLines = new ArrayList<>();
        updatedLines.addAll(originalLines.subList(0, lineFrom - 1));
        updatedLines.addAll(newCodeLines);
        updatedLines.addAll(originalLines.subList(lineTo, originalLines.size()));
        LOG.debug("Staged remediation {} change {} for '{}' using FVDL encoding {}; updatedLines={}", instanceId, changeIndex,
                filename, sourceEncoding.name(), updatedLines.size());
        return String.join(lineSeparator, updatedLines);
    }

    private SourceFileContent getPendingOrSourceContent(Path filePath, String filename, FVDLMetadata fvdlMetadata,
            Map<Path, PendingFileWrite> pendingWrites) {
        PendingFileWrite pendingWrite = pendingWrites.get(filePath);
        return pendingWrite == null
                ? readSourceFile(filePath, filename, fvdlMetadata)
                : new SourceFileContent(pendingWrite.content(), pendingWrite.charset(), pendingWrite.encodingSource());
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
            List<Integer> matches = FuzzyContextSearcher.fuzzySearchContextMatches(originalLines, contextLine, 0);
            if (matches.size() > 1) {
                String candidateLines = matches.stream()
                        .map(line -> String.valueOf(line + 1))
                        .collect(Collectors.joining(", "));
                throw new SkipRemediationException(SkipReason.SOURCE_CONTEXT_AMBIGUOUS,
                        "Source context matched multiple locations in file '" + filename + "'; candidate lines: " + candidateLines);
            }
            return matches.isEmpty() ? -1 : matches.get(0);
        } catch (IOException e) {
            throw new SkipRemediationException(SkipReason.SOURCE_CONTEXT_NOT_FOUND,
                    "Error searching source context for remediation '" + instanceId + "' in file '" + filename + "'", e);
        }
    }

    private int[] fuzzySearchOriginalCode(String instanceId, String filename, List<String> originalLines, List<String> originalCodeLine,
            int contextLineFrom, int contextLineCount, int contextBefore, int contextAfter) {
        int contextStart = contextLineFrom + contextBefore;
        int contextEnd = contextLineFrom + contextLineCount - contextAfter;
        if (contextStart < 0 || contextStart >= contextEnd || contextEnd > originalLines.size()) {
            return new int[] {-1, -1};
        }

        int[] lineFromTo = FuzzyContextSearcher.fuzzySearchOriginalCode(
                originalLines.subList(contextStart, contextEnd), originalCodeLine, 0, 0);
        if (lineFromTo[0] == -1 || lineFromTo[1] == -1) {
            return lineFromTo;
        }
        return new int[] {lineFromTo[0] + contextStart, lineFromTo[1] + contextStart};
    }

    private boolean isFilePresent(Path path) {
        return Files.exists(path) && Files.isRegularFile(path);
    }

    /** Nullable: missing/unreadable FVDL means FPR encoding candidate is skipped. */
    private FVDLMetadata loadFvdlMetadata() {
        if (!Files.exists(fprHandle.getPath("/audit.fvdl"))) {
            LOG.warn("FVDL file '/audit.fvdl' is missing; FPR encoding candidate will be skipped");
            return null;
        }

        try (ZipFile zipFile = new ZipFile(fprHandle.getFprPath().toFile())) {
            LOG.debug("Loading FVDL build metadata from '{}' to resolve source encodings", fprHandle.getFprPath());
            // Decoder unused for metadata-only parse; ctor requires one for FileUtils wiring.
            StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle, sourceDecoder);
            processor.parseBuildMetadata(zipFile, "audit.fvdl");
            LOG.debug("Loaded FVDL build metadata from '{}'", fprHandle.getFprPath());
            return processor.getFvdlMetadata();
        } catch (Exception e) {
            LOG.warn("Error reading source file encodings from audit.fvdl; FPR encoding candidate will be skipped", e);
            return null;
        }
    }

    private SourceFileContent readSourceFile(Path filePath, String filename, FVDLMetadata fvdlMetadata) {
        try {
            byte[] sourceBytes = Files.readAllBytes(filePath);
            // Metadata may be null (FVDL missing); FPR candidate fails and other encodings are tried.
            DecodeResult decodeResult = sourceDecoder.decode(sourceBytes, filename, fvdlMetadata);
            LOG.debug("Strict decoded '{}' using {}; sourceBytes={}, decodedChars={}", filename, decodeResult.source(), sourceBytes.length,
                    decodeResult.content().length());
            return new SourceFileContent(decodeResult.content(), decodeResult.charset(), decodeResult.source());
        } catch (SourceDecodeException e) {
            throw new SkipRemediationException(SkipReason.SOURCE_DECODE_FAILED, e.getMessage(), e);
        } catch (IOException e) {
            throw new SkipRemediationException(SkipReason.SOURCE_READ_FAILED, "Error reading source code file '" + filePath + "'", e);
        }
    }

    private byte[] encodeSourceFile(String content, Charset charset, String filename) {
        try {
            return SourceEncoder.encode(content, charset, filename);
        } catch (SourceEncodeException e) {
            throw new SkipRemediationException(SkipReason.REMEDIATION_ENCODE_FAILED, e.getMessage(), e);
        }
    }

    private String getRequiredElementText(Element parent, String elementName) {
        return getRequiredElement(parent, elementName).getTextContent();
    }

    private Element getRequiredElement(Element parent, String elementName) {
        NodeList nodes = parent.getElementsByTagNameNS(NAMESPACE_URI, elementName);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            throw new SkipRemediationException(SkipReason.REMEDIATION_DATA_INVALID,
                    "Missing required remediation element '" + elementName + "'");
        }
        return (Element) nodes.item(0);
    }

    private int parseRequiredContextAttribute(Element context, String attributeName) {
        String value = context.getAttribute(attributeName);
        if (value == null || value.isBlank()) {
            throw new SkipRemediationException(SkipReason.REMEDIATION_DATA_INVALID,
                    "Missing required remediation context attribute '" + attributeName + "'");
        }
        try {
            int parsedValue = Integer.parseInt(value);
            if (parsedValue < 0) {
                throw new NumberFormatException("negative value");
            }
            return parsedValue;
        } catch (NumberFormatException e) {
            throw new SkipRemediationException(SkipReason.REMEDIATION_DATA_INVALID,
                    "Invalid remediation context attribute '" + attributeName + "': " + value, e);
        }
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

    private void recordSkipped(Map<String, Integer> skippedByReason, String reason) {
        skippedByReason.merge(reason, 1, Integer::sum);
    }

    private String skipReasonLabel(SkipRemediationException exception) {
        return exception.reason.displayName;
    }

    private String formatSkippedReasons(Map<String, Integer> skippedByReason) {
        List<String> parts = new ArrayList<>();
        skippedByReason.forEach((reason, count) -> parts.add(reason + "=" + count));
        return String.join(", ", parts);
    }

    private RemediationKey createRemediationKey(
        Element fileChanges, Element change, Path sourceBasePath, String comparisonCode) {

        String fileName = getRequiredElementText(fileChanges, "Filename");
        Path filePath = sourceBasePath.resolve(fileName).normalize();
        int lineFrom = parseRequiredInt(change, "LineFrom");
        int lineTo = parseRequiredInt(change, "LineTo");

        return new RemediationKey(fileName, filePath, lineFrom, lineTo, comparisonCode);
    }

    private String trimBlankLines(String content) {
        String[] lines = content.split("\\R", -1);
        int start = 0, end = lines.length - 1;

        while (start <= end && lines[start].isBlank()) start++;
        while (end >= start && lines[end].isBlank()) end--;

        return start > end ? "" :
            String.join(System.lineSeparator(), Arrays.copyOfRange(lines, start, end + 1));
    }

    private String normalizeProposedCode(String content, String fileName) {
        if (content == null) return null;

        String language = FileTypeLanguageMapperUtil.getProgrammingLanguage(
            FileUtil.getFileExtension(fileName));
        String commentSymbol = LanguageCommentMapperUtil.getProgrammingLanguageComment(language);

        if ("Unknown".equals(commentSymbol)) return trimBlankLines(content);

        String closingToken = commentSymbol.equals("<!--") ? "-->"
            : commentSymbol.equals("<%--") ? "--%>" : null;

        Pattern markerPattern = Pattern.compile(
            "[ \\t]*" + Pattern.quote(commentSymbol) + " L\\d+"
                + (closingToken != null ? "[ \\t]*" + Pattern.quote(closingToken) : "")
                + "[ \\t]*$");

        String[] lines = content.split("\\R", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = markerPattern.matcher(lines[i]);
            result.append(matcher.find() ? lines[i].substring(0, matcher.start()) : lines[i]);
            if (i < lines.length - 1) result.append(System.lineSeparator());
        }

        return trimBlankLines(result.toString());
    }

    private String createComparisonCode(String normalizedCode, String fileName) {
        if (normalizedCode == null) return null;

        String language = FileTypeLanguageMapperUtil.getProgrammingLanguage(
            FileUtil.getFileExtension(fileName));
        String commentSymbol = LanguageCommentMapperUtil.getProgrammingLanguageComment(language);

        if ("Unknown".equals(commentSymbol)) return normalizedCode.replaceAll("\\s+", "");

        String comparisonCode = normalizedCode;
        String closingToken = commentSymbol.equals("<!--") ? "-->"
            : commentSymbol.equals("<%--") ? "--%>" : null;

        if (closingToken != null) {
            comparisonCode = comparisonCode.replaceAll(
                "(?s)" + Pattern.quote(commentSymbol) + ".*?" + Pattern.quote(closingToken), "");
        } else if ("//".equals(commentSymbol)) {
            comparisonCode = comparisonCode.replaceAll("(?m)" + Pattern.quote(commentSymbol) + ".*$", "")
                .replaceAll("(?s)/\\*.*?\\*/", "");
        } else if ("#".equals(commentSymbol)) {
            comparisonCode = comparisonCode.replaceAll("(?m)" + Pattern.quote(commentSymbol) + ".*$", "");
        }

        return comparisonCode.replaceAll("\\s+", "");
    }

    private List<RemediationKey> createRemediationKeys(Element remediation, Path sourceBasePath) {
        List<RemediationKey> keys = new ArrayList<>();
        NodeList fileChangesNodes = remediation.getElementsByTagNameNS(NAMESPACE_URI, "FileChanges");

        for (int i = 0; i < fileChangesNodes.getLength(); i++) {
            Element fileChanges = (Element) fileChangesNodes.item(i);
            NodeList changeNodes = fileChanges.getElementsByTagNameNS(NAMESPACE_URI, "Change");

            for (int j = 0; j < changeNodes.getLength(); j++) {
                Element change = (Element) changeNodes.item(j);
                String fileName = getRequiredElementText(fileChanges, "Filename");
                String newCode = getRequiredElementText(change, "NewCode");
                String normalizedCode = normalizeProposedCode(newCode, fileName);
                String comparisonCode = createComparisonCode(normalizedCode, fileName);

                keys.add(createRemediationKey(fileChanges, change, sourceBasePath, comparisonCode));
            }
        }

        return keys;
    }
}
