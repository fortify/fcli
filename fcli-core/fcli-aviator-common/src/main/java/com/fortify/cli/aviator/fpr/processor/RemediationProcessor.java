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
import java.util.LinkedHashSet;
import java.util.List;
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
    Logger logger = LoggerFactory.getLogger(RemediationProcessor.class);
    private static final String NAMESPACE_URI = "xmlns://www.fortify.com/schema/remediations";


    private final FprHandle fprHandle;
    private final String sourceCodeDirectory;
    public record RemediationMetric(int totalRemediations, int appliedRemediations, int skippedRemediations, Set<String> modifiedFiles){}

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

        // Sanitize and normalize the base source directory path once.
        String trimmedSourceDir = sourceCodeDirectory.trim();
        if (trimmedSourceDir.length() > 1 &&
            ((trimmedSourceDir.startsWith("\"") && trimmedSourceDir.endsWith("\"")) ||
             (trimmedSourceDir.startsWith("'") && trimmedSourceDir.endsWith("'")))) {
            trimmedSourceDir = trimmedSourceDir.substring(1, trimmedSourceDir.length() - 1);
        }
        final Path sourceBasePath = Paths.get(trimmedSourceDir).toAbsolutePath().normalize();
        logger.debug("Applying remediations from {} to source directory {}", remediationPath, sourceBasePath);
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
            logger.debug("Loaded {} remediation entries from {}", totalRemediations, remediationPath);
            appliedRemediations=0;
            for (int i = 0; i < remediationNodes.getLength(); i++) {
                Element remediation = (Element) remediationNodes.item(i);
                NodeList fileChangesNodes = remediation.getElementsByTagNameNS(NAMESPACE_URI, "FileChanges");
                boolean remediationAppliedOnIssue = false;
                for (int j = 0; j < fileChangesNodes.getLength(); j++) {
                    Element fileChanges = (Element) fileChangesNodes.item(j);
                    String filename = fileChanges.getElementsByTagNameNS(NAMESPACE_URI, "Filename").item(0).getTextContent();

                    Path filePath = sourceBasePath.resolve(filename).normalize();
                    String instanceId = remediation.getAttribute("instanceId");
                    logger.debug("Processing remediation {} file change for '{}' resolved to '{}'", instanceId, filename, filePath);

                    if (!filePath.startsWith(sourceBasePath)) {
                        logger.error("Skipping file '{}' as it resolves to a path outside the source directory (potential path traversal attack)", filename);
                        continue;
                    }

                    if (!isFilePresent(filePath)) {
                        logger.error("Source code file not present at: {}", filePath.toString());
                        throw new AviatorTechnicalException("Source code file not present at: " + filePath.toString());
                    }

                    String fileHash = fileChanges.getElementsByTagNameNS(NAMESPACE_URI, "Hash").item(0).getTextContent();
                    Charset sourceEncoding = getRequiredSourceEncoding(filename, fvdlMetadata);

                    NodeList changesNodes = fileChanges.getElementsByTagNameNS(NAMESPACE_URI, "Change");
                    logger.debug("Remediation {} has {} change(s) for '{}' using FVDL encoding {}", instanceId, changesNodes.getLength(), filename,
                            sourceEncoding.name());
                    for (int k = 0; k < changesNodes.getLength(); k++) {
                        Element change = (Element) changesNodes.item(k);


                        String originalContent = readSourceFile(filePath, filename, sourceEncoding);
                        String lineSeparator = detectLineSeparator(originalContent);
                        String content = normalizeLineEndings(originalContent);

                        List<String> originalLines = Arrays.asList(content.split("\n", -1));
                        logger.debug("Decoded '{}' using {}; lineSeparator={}, normalizedLines={}", filename, sourceEncoding.name(),
                                describeLineSeparator(lineSeparator), originalLines.size());

                        int lineFrom = Integer.parseInt(change.getElementsByTagNameNS(NAMESPACE_URI, "LineFrom").item(0).getTextContent());
                        int lineTo = Integer.parseInt(change.getElementsByTagNameNS(NAMESPACE_URI, "LineTo").item(0).getTextContent());
                        logger.debug("Remediation {} change {} for '{}' targets lines {}-{}", instanceId, k + 1, filename, lineFrom, lineTo);


                        String calculatedHash = calculateHashBase64(content, "SHA-256");
                        boolean fileHashMatches = calculatedHash.equals(fileHash);
                        logger.debug("Remediation {} hash check for '{}': {}", instanceId, filename, fileHashMatches ? "matched" : "mismatched");
                        if (!fileHashMatches) {
                            logger.debug("File hash mismatch for remediation {} in {}; searching changed source content", instanceId, filename);
                            Element contextElem = (Element) change.getElementsByTagNameNS(NAMESPACE_URI, "Context").item(0);
                            String contextText = contextElem.getTextContent();

                            //spliting a string into a list of lines, using both Unix (\n) and Windows (\r\n) line endings.
                            List<String> contextLine = Arrays.asList(contextText.split("\\r?\\n"));
                            int contextLineFrom = FuzzyContextSearcher.fuzzySearchContext(originalLines, contextLine, 0) ;
                            if(contextLineFrom==-1) {
                                logger.debug("Context search failed for remediation {} in {}; context lines={}, source lines={}", instanceId, filename,
                                        contextLine.size(), originalLines.size());
                                logger.info("File content has changed. Context Lines not found. Remediation not possible for {}", instanceId);
                                continue;
                            }
                            logger.debug("Context for remediation {} in {} matched at line {}", instanceId, filename, contextLineFrom + 1);
                            Element OriginalCodeElem = (Element) change.getElementsByTagNameNS(NAMESPACE_URI, "OriginalCode").item(0);
                            String OriginalCodeText = OriginalCodeElem.getTextContent();

                            //spliting a string into a list of lines, using both Unix (\n) and Windows (\r\n) line endings.
                            List<String> OriginalCodeLine = Arrays.asList(OriginalCodeText.split("\\r?\\n"));

                            int[] lineFromTo = FuzzyContextSearcher.fuzzySearchOriginalCode(originalLines, OriginalCodeLine, 0, contextLineFrom);
                            if(lineFromTo[0]==-1 || lineFromTo[1]==-1) {
                                logger.debug("Original code search failed for remediation {} in {}; context line={}, original code lines={}, source lines={}",
                                        instanceId, filename, contextLineFrom + 1, OriginalCodeLine.size(), originalLines.size());
                                logger.info("File content has changed. Original Code lines not found. Remediation not possible for {}", instanceId);
                                continue;
                            }
                            lineFrom = lineFromTo[0]+1; //Adding 1 for 1-based indexing
                            lineTo = lineFromTo[1] + 1; //Adding 1 for 1-based indexing
                            logger.debug("Original code for remediation {} in {} matched at lines {}-{}", instanceId, filename, lineFrom, lineTo);
                        }


                        //File hash is matched i,e the file has not been changed

                        String newCodeRaw = change.getElementsByTagNameNS(NAMESPACE_URI, "NewCode").item(0).getTextContent();

                        List<String> newCodeLines = Arrays.asList(newCodeRaw.split("\n"));


                        // Replace lines
                        List<String> updatedLines = new ArrayList<>();
                        updatedLines.addAll(originalLines.subList(0, lineFrom - 1));
                        updatedLines.addAll(newCodeLines);
                        updatedLines.addAll(originalLines.subList(lineTo, originalLines.size()));
                        byte[] updatedBytes = encodeStrict(String.join(lineSeparator, updatedLines), sourceEncoding, filename);
                        logger.debug("Writing remediation {} to '{}' using FVDL encoding {}; updatedLines={}, encodedBytes={}", instanceId, filename,
                            sourceEncoding.name(), updatedLines.size(), updatedBytes.length);
                        Files.write(filePath, updatedBytes);
                        modifiedFiles.add(filename);
                        logger.info("Remediation applied for {} in file {}", instanceId, filename);
                        if(!remediationAppliedOnIssue) {
                            remediationAppliedOnIssue = true;
                            appliedRemediations++;
                        }
                    }

                }
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.error("Error parsing remediations.xml file: {}", remediationPath, e);
            throw new AviatorTechnicalException("Error processing remediation.xml file.", e);
        } catch (AviatorTechnicalException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error processing remediation.xml: {}", remediationPath, e);
            throw new AviatorTechnicalException("Unexpected error processing remediations.xml.", e);
        }
        return new RemediationMetric(totalRemediations, appliedRemediations, totalRemediations-appliedRemediations, modifiedFiles);
    }

    private boolean isFilePresent(Path path) {
        return Files.exists(path) && Files.isRegularFile(path);
    }

    private FVDLMetadata loadFvdlMetadata() {
        if (!Files.exists(fprHandle.getPath("/audit.fvdl"))) {
            throw new AviatorTechnicalException("FVDL file '/audit.fvdl' is missing; cannot determine source file encodings for applying remediations");
        }

        try (ZipFile zipFile = new ZipFile(fprHandle.getFprPath().toFile())) {
            logger.debug("Loading FVDL build metadata from '{}' to resolve source encodings", fprHandle.getFprPath());
            StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle);
            processor.parseBuildMetadata(zipFile, "audit.fvdl");
            logger.debug("Loaded FVDL build metadata from '{}'", fprHandle.getFprPath());
            return processor.getFvdlMetadata();
        } catch (Exception e) {
            throw new AviatorTechnicalException("Error reading source file encodings from audit.fvdl", e);
        }
    }

    private Charset getRequiredSourceEncoding(String filename, FVDLMetadata fvdlMetadata) {
        String encoding = fvdlMetadata.findSourceFileEncodingForFileName(filename);
        if (encoding == null || encoding.isBlank()) {
            logger.debug("FVDL source encoding lookup failed for '{}'", filename);
            throw new AviatorTechnicalException("FVDL does not declare a source encoding for file '" + filename + "'; cannot safely apply remediation");
        }

        try {
            Charset charset = Charset.forName(encoding);
            logger.debug("FVDL source encoding for '{}' resolved to '{}'", filename, charset.name());
            return charset;
        } catch (Exception e) {
            throw new AviatorTechnicalException("FVDL declares unsupported source encoding '" + encoding + "' for file '" + filename + "'", e);
        }
    }

    private String readSourceFile(Path filePath, String filename, Charset sourceEncoding) {
        try {
            byte[] sourceBytes = Files.readAllBytes(filePath);
            String decodedContent = decodeStrict(sourceBytes, sourceEncoding);
            logger.debug("Strict decoded '{}' using {}; sourceBytes={}, decodedChars={}", filename, sourceEncoding.name(), sourceBytes.length,
                    decodedContent.length());
            return decodedContent;
        } catch (CharacterCodingException e) {
            throw new AviatorTechnicalException("FVDL declares source encoding '" + sourceEncoding.name() + "' for file '" + filename + "', but the source file cannot be decoded using that encoding", e);
        } catch (IOException e) {
            throw new AviatorTechnicalException("Error reading source code file '" + filePath + "'", e);
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
            throw new AviatorTechnicalException("Remediation content for file '" + filename + "' cannot be encoded using FVDL source encoding '" + charset.name() + "'", e);
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


}
