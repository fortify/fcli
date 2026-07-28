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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.FuzzyContextSearcher;

public class RemediationProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(RemediationProcessor.class);
    private static final String NAMESPACE_URI = "xmlns://www.fortify.com/schema/remediations";

    private final FprHandle fprHandle;
    private final String sourceCodeDirectory;

    public record RemediationMetric(int totalRemediations, int appliedRemediations, int skippedRemediations, Set<String> modifiedFiles,
                                    Map<String, Integer> skippedByReason) {
        public RemediationMetric(int totalRemediations, int appliedRemediations, int skippedRemediations, Set<String> modifiedFiles) {
            this(totalRemediations, appliedRemediations, skippedRemediations, modifiedFiles, Map.of());
        }
    }

    private record FvdlMetadataResult(FVDLMetadata metadata, SkipReason skipReason) {}

    private record PendingFileWrite(String filename, Path filePath, String content, byte[] updatedBytes) {}

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
        UNEXPECTED_ERROR("Unexpected remediation processing error");

        private final String displayName;

        SkipReason(String displayName) {
            this.displayName = displayName;
        }
    }

    private static class SkipRemediationException extends RuntimeException {
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

    public RemediationProcessor(FprHandle fprHandle, String sourceCodeDirectory) {
        this.fprHandle = fprHandle;
        this.sourceCodeDirectory = sourceCodeDirectory;
    }

    public RemediationMetric processRemediationXML() {
        Path remediationPath = fprHandle.getPath("/remediations.xml");
        Document remediationDoc;
        int totalRemediations;
        int appliedRemediations;
        Set<String> modifiedFiles = new LinkedHashSet<>();
        Map<String, Integer> skippedByReason = new LinkedHashMap<>();

        // Sanitize and normalize the base source directory path once.
        String trimmedSourceDir = sourceCodeDirectory.trim();
        if (trimmedSourceDir.length() > 1 &&
            ((trimmedSourceDir.startsWith("\"") && trimmedSourceDir.endsWith("\"")) ||
             (trimmedSourceDir.startsWith("'") && trimmedSourceDir.endsWith("'")))) {
            trimmedSourceDir = trimmedSourceDir.substring(1, trimmedSourceDir.length() - 1);
        }
        final Path sourceBasePath = Paths.get(trimmedSourceDir).toAbsolutePath().normalize();
        LOG.debug("Applying remediations from {} to source directory {}", remediationPath, sourceBasePath);
        final FvdlMetadataResult fvdlMetadataResult = loadFvdlMetadata();

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
                try {
                    if (processRemediation(remediation, sourceBasePath, fvdlMetadataResult, modifiedFiles)) {
                        appliedRemediations++;
                    } else {
                        recordSkipped(skippedByReason, SkipReason.NO_CHANGES);
                    }
                } catch (AviatorTechnicalException e) {
                    throw e;
                } catch (SkipRemediationException e) {
                    recordSkipped(skippedByReason, e.reason);
                    LOG.info("Skipping remediation {}: {}", remediation.getAttribute("instanceId"), e.getMessage());
                    LOG.debug("Skip reason for remediation {}: {}", remediation.getAttribute("instanceId"), e.reason.displayName, e);
                } catch (Exception e) {
                    recordSkipped(skippedByReason, SkipReason.UNEXPECTED_ERROR);
                    LOG.info("Skipping remediation {} due to an unexpected processing error", remediation.getAttribute("instanceId"));
                    LOG.debug("Unexpected error while processing remediation {}", remediation.getAttribute("instanceId"), e);
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
        int skippedRemediations = totalRemediations - appliedRemediations;
        LOG.info("Auto-remediation summary: total={}, applied={}, skipped={}", totalRemediations, appliedRemediations, skippedRemediations);
        if (!skippedByReason.isEmpty()) {
            LOG.info("Skipped remediations by reason: {}", formatSkippedReasons(skippedByReason));
        }
        return new RemediationMetric(totalRemediations, appliedRemediations, skippedRemediations, modifiedFiles, skippedByReason);
    }

    private boolean processRemediation(Element remediation, Path sourceBasePath, FvdlMetadataResult fvdlMetadataResult,
            Set<String> modifiedFiles) {
        NodeList fileChangesNodes = remediation.getElementsByTagNameNS(NAMESPACE_URI, "FileChanges");
        if (fileChangesNodes.getLength() == 0) {
            throw new SkipRemediationException(SkipReason.NO_CHANGES, "No file changes found");
        }

        Map<Path, PendingFileWrite> pendingWrites = new LinkedHashMap<>();
        for (int j = 0; j < fileChangesNodes.getLength(); j++) {
            processFileChanges(remediation, (Element) fileChangesNodes.item(j), sourceBasePath, fvdlMetadataResult, pendingWrites);
        }
        if (pendingWrites.isEmpty()) {
            return false;
        }
        commitRemediationWrites(remediation.getAttribute("instanceId"), pendingWrites, modifiedFiles);
        return true;
    }

    private boolean processFileChanges(Element remediation, Element fileChanges, Path sourceBasePath, FvdlMetadataResult fvdlMetadataResult,
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
        List<String> newCodeLines = Arrays.asList(getRequiredElementText(change, "NewCode").split("\n"));
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

    private void commitRemediationWrites(String instanceId, Map<Path, PendingFileWrite> pendingWrites, Set<String> modifiedFiles) {
        Map<Path, byte[]> originalBytesByPath = readOriginalBytesForRollback(pendingWrites);
        List<PendingFileWrite> attemptedWrites = new ArrayList<>();
        try {
            for (PendingFileWrite pendingWrite : pendingWrites.values()) {
                attemptedWrites.add(pendingWrite);
                LOG.debug("Writing remediation {} to '{}' using staged bytes; encodedBytes={}", instanceId, pendingWrite.filename(),
                        pendingWrite.updatedBytes().length);
                writeSourceFile(pendingWrite.filePath(), pendingWrite.updatedBytes(), pendingWrite.filename());
            }
        } catch (SkipRemediationException e) {
            rollbackRemediationWrites(instanceId, attemptedWrites, originalBytesByPath);
            throw e;
        }

        for (PendingFileWrite pendingWrite : pendingWrites.values()) {
            modifiedFiles.add(pendingWrite.filename());
            LOG.info("Remediation applied for {} in file {}", instanceId, pendingWrite.filename());
        }
    }

    private Map<Path, byte[]> readOriginalBytesForRollback(Map<Path, PendingFileWrite> pendingWrites) {
        Map<Path, byte[]> originalBytesByPath = new LinkedHashMap<>();
        for (PendingFileWrite pendingWrite : pendingWrites.values()) {
            try {
                originalBytesByPath.put(pendingWrite.filePath(), Files.readAllBytes(pendingWrite.filePath()));
            } catch (IOException e) {
                throw new SkipRemediationException(SkipReason.SOURCE_READ_FAILED,
                        "Error reading source code file '" + pendingWrite.filename() + "' before writing remediation", e);
            }
        }
        return originalBytesByPath;
    }

    private void rollbackRemediationWrites(String instanceId, List<PendingFileWrite> attemptedWrites,
            Map<Path, byte[]> originalBytesByPath) {
        for (PendingFileWrite pendingWrite : attemptedWrites) {
            byte[] originalBytes = originalBytesByPath.get(pendingWrite.filePath());
            if (originalBytes == null) {
                continue;
            }
            try {
                Files.write(pendingWrite.filePath(), originalBytes);
                LOG.warn("Rolled back remediation {} changes for '{}' after write failure", instanceId, pendingWrite.filename());
            } catch (IOException rollbackException) {
                LOG.error("Failed to roll back remediation {} changes for '{}'", instanceId, pendingWrite.filename(), rollbackException);
                throw new AviatorTechnicalException("Failed to roll back remediation changes for '" + pendingWrite.filename() +
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

    private void writeSourceFile(Path filePath, byte[] updatedBytes, String filename) {
        try {
            Files.write(filePath, updatedBytes);
        } catch (IOException e) {
            throw new SkipRemediationException(SkipReason.SOURCE_WRITE_FAILED, "Error writing source code file '" + filename + "'", e);
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

    private void recordSkipped(Map<String, Integer> skippedByReason, SkipReason reason) {
        skippedByReason.merge(reason.displayName, 1, Integer::sum);
    }

    private String formatSkippedReasons(Map<String, Integer> skippedByReason) {
        List<String> parts = new ArrayList<>();
        skippedByReason.forEach((reason, count) -> parts.add(reason + "=" + count));
        return String.join(", ", parts);
    }
}
