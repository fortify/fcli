/*
 * Copyright 2021-2025 Open Text.
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
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

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
    public record RemediationMetric(int totalRemediations, int appliedRemediations, int skippedRemediations){}

    public RemediationProcessor(FprHandle fprHandle, String sourceCodeDirectory) {
        this.fprHandle = fprHandle;
        this.sourceCodeDirectory = sourceCodeDirectory;
    }

    public RemediationMetric processRemediationXML() {
        Path remediationPath = fprHandle.getPath("/remediations.xml");
        Document remediationDoc;
        int totalRemediations;
        int appliedRemediations;

        // Sanitize and normalize the base source directory path once.
        String trimmedSourceDir = sourceCodeDirectory.trim();
        if (trimmedSourceDir.length() > 1 && 
            ((trimmedSourceDir.startsWith("\"") && trimmedSourceDir.endsWith("\"")) ||
             (trimmedSourceDir.startsWith("'") && trimmedSourceDir.endsWith("'")))) {
            trimmedSourceDir = trimmedSourceDir.substring(1, trimmedSourceDir.length() - 1);
        }
        final Path sourceBasePath = Paths.get(trimmedSourceDir).toAbsolutePath().normalize();

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
            appliedRemediations=0;
            for (int i = 0; i < remediationNodes.getLength(); i++) {
                Element remediation = (Element) remediationNodes.item(i);
                NodeList fileChangesNodes = remediation.getElementsByTagNameNS(NAMESPACE_URI, "FileChanges");
                boolean remediationAppliedOnIssue = false;
                for (int j = 0; j < fileChangesNodes.getLength(); j++) {
                    Element fileChanges = (Element) fileChangesNodes.item(j);
                    String filename = fileChanges.getElementsByTagNameNS(NAMESPACE_URI, "Filename").item(0).getTextContent();

                    Path filePath = sourceBasePath.resolve(filename).normalize();

                    if (!filePath.startsWith(sourceBasePath)) {
                        logger.error("Skipping file '{}' as it resolves to a path outside the source directory (potential path traversal attack)", filename);
                        continue;
                    }

                    if (!isFilePresent(filePath)) {
                        logger.error("Source code file not present at: {}", filePath.toString());
                        throw new AviatorTechnicalException("Source code file not present at: " + filePath.toString());
                    }

                    String fileHash = fileChanges.getElementsByTagNameNS(NAMESPACE_URI, "Hash").item(0).getTextContent();
                    String instanceId = remediation.getAttribute("instanceId");

                    NodeList changesNodes = fileChanges.getElementsByTagNameNS(NAMESPACE_URI, "Change");
                    for (int k = 0; k < changesNodes.getLength(); k++) {
                        Element change = (Element) changesNodes.item(k);


                        String content = Files.readString(filePath, StandardCharsets.UTF_8).replace("\r\n", "\n");

                        List<String> originalLines = Arrays.asList(content.split("\n"));

                        int lineFrom = Integer.parseInt(change.getElementsByTagNameNS(NAMESPACE_URI, "LineFrom").item(0).getTextContent());
                        int lineTo = Integer.parseInt(change.getElementsByTagNameNS(NAMESPACE_URI, "LineTo").item(0).getTextContent());


                        if (!calculateHashBase64(content, "SHA-256").equals(fileHash)) {
                            int diff = lineTo-lineFrom;
                            Element contextElem = (Element) change.getElementsByTagNameNS(NAMESPACE_URI, "Context").item(0);
                            String contextText = contextElem.getTextContent();

                            //spliting a string into a list of lines, using both Unix (\n) and Windows (\r\n) line endings.
                            List<String> contextLine = Arrays.asList(contextText.split("\\r?\\n"));
                            int before = Integer.parseInt(contextElem.getAttribute("before"));
                            int contextLineFrom = FuzzyContextSearcher.fuzzySearchContext(originalLines, contextLine, 0) ;
                            if(contextLineFrom==-1) {
                                logger.info("File content has changed. Context Lines not found. Remediation not possible for {}", instanceId);
                                continue;
                            }
                            Element OriginalCodeElem = (Element) change.getElementsByTagNameNS(NAMESPACE_URI, "OriginalCode").item(0);
                            String OriginalCodeText = OriginalCodeElem.getTextContent();

                            //spliting a string into a list of lines, using both Unix (\n) and Windows (\r\n) line endings.
                            List<String> OriginalCodeLine = Arrays.asList(OriginalCodeText.split("\\r?\\n"));

                            int[] lineFromTo = FuzzyContextSearcher.fuzzySearchOriginalCode(originalLines, OriginalCodeLine, 0, contextLineFrom);
                            if(lineFromTo[0]==-1 || lineFromTo[1]==-1) {
                                logger.info("File content has changed. Original Code lines not found. Remediation not possible for {}", instanceId);
                                continue;
                            }
                            lineFrom = lineFromTo[0]+1; //Adding 1 for 1-based indexing
                            lineTo = lineFromTo[1] + 1; //Adding 1 for 1-based indexing
                        }


                        //File hash is matched i,e the file has not been changed

                        String newCodeRaw = change.getElementsByTagNameNS(NAMESPACE_URI, "NewCode").item(0).getTextContent();

                        List<String> newCodeLines = Arrays.asList(newCodeRaw.split("\n"));


                        // Replace lines
                        List<String> updatedLines = new ArrayList<>();
                        updatedLines.addAll(originalLines.subList(0, lineFrom - 1));
                        updatedLines.addAll(newCodeLines);
                        updatedLines.addAll(originalLines.subList(lineTo, originalLines.size()));
                        Files.write(filePath, updatedLines);
                        logger.info("Remediation applied for {}", instanceId);
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
        return new RemediationMetric(totalRemediations, appliedRemediations, totalRemediations-appliedRemediations);
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
            throw new RuntimeException(algorithm + " algorithm not found", e);
        }
    }

    private List<String> readFileWithFallback(Path filePath) throws IOException {
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            String content = new String(fileBytes);
            return Arrays.asList(content.split("\\r?\\n"));
        } catch (MalformedInputException e) {
            //logger.error("Malformed input reading file: {}", path, e);
            throw new RuntimeException(new IOException("Unable to read file due to encoding: ", e));
        } catch (IOException e) {
            //logger.error("Failed to read file: {}", path, e);
            throw new RuntimeException(new IOException("Unable to read file: ", e));
        }

    }
}
