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
package com.fortify.cli.common.fpr.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.util.SecureXmlParserFactory;

import lombok.Getter;

/**
 * Represents an open FPR file using Java's Zip File System Provider.
 * This class centralizes access to the FPR's contents without physical extraction,
 * and ensures resources are properly managed via the AutoCloseable interface.
 * All file access within the FPR should go through this class.
 */
public final class FprHandle implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FprHandle.class);
    private static final Pattern PROPERTIES_DOCTYPE_PATTERN = Pattern.compile(
        "<!DOCTYPE\\s+properties\\s+SYSTEM\\s+(['\"])http://java\\.sun\\.com/dtd/properties\\.dtd\\1\\s*>"
    );

    private final FileSystem zipfs;
    /**
     * -- GETTER --
     *  Returns the original path of the FPR file on the host file system.
     *
     * @return The Path to the source .fpr file.
     */
    @Getter
    private final Path fprPath;
    private volatile Map<String, String> sourceFileMap;


    /**
     * Opens an FPR file as a virtual file system. Should be used in a try-with-resources block.
     * @param fprPath The path to the .fpr file.
     * @throws FcliTechnicalException if the file cannot be opened as a zip file system.
     */
    public FprHandle(Path fprPath) {
        if (fprPath == null || !Files.exists(fprPath)) {
            throw new FcliTechnicalException("FPR file path is null or does not exist: " + fprPath);
        }
        this.fprPath = fprPath;
        try {
            // Using null for the ClassLoader is important to avoid potential conflicts.
            this.zipfs = FileSystems.newFileSystem(fprPath, (ClassLoader) null);
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to open FPR as a zip file system: " + fprPath, e);
        }
    }

    /**
     * Gets a Path object for a file or directory inside the FPR.
     * @param internalPath The root path inside the zip file (e.g., "/audit.xml", "/src-archive/").
     * @return A Path object representing the internal file or directory.
     */
    public Path getPath(String internalPath) {
        return zipfs.getPath(internalPath);
    }

    /**
     * Validates that the opened FPR contains the necessary data for SAST processing.
     * Checks for audit.fvdl and the presence of source code.
     * @throws FcliSimpleException if the FPR is invalid for processing.
     */
    public void validate() {
        if (!Files.exists(getPath("/audit.fvdl"))) {
            throw new FcliSimpleException("Invalid FPR: The scan results do not contain 'audit.fvdl' and do not appear to be valid SAST scan results.");
        }
        if (!hasSource()) {
            throw new FcliSimpleException("Invalid FPR: Source code is missing or incomplete. The 'src-archive' directory must contain 'index.xml' and at least one source file.");
        }
        getSourceFileMap();
        LOG.info("FPR validation successful for: {}", this.fprPath);
    }

    /**
     * Validates that the opened FPR contains DAST (WebInspect) scan results.
     * @throws FcliSimpleException if the FPR does not contain webinspect.xml.
     */
    public void validateDast() {
        if (!Files.exists(getPath("/webinspect.xml"))) {
            throw new FcliSimpleException("Invalid FPR: The scan results do not contain 'webinspect.xml' and do not appear to be valid DAST (WebInspect) scan results.");
        }
    }

    public boolean hasSource() {
        Path srcArchiveDir = getPath("/src-archive");

        if (!Files.exists(srcArchiveDir) || !Files.exists(srcArchiveDir.resolve("index.xml"))) {
            LOG.warn("FPR is missing 'src-archive/index.xml'. Source code may not be included correctly. File: {}", this.fprPath);
            return false;
        }

        try (Stream<Path> stream = Files.list(srcArchiveDir)) {
            boolean hasSourceFile = stream.anyMatch(p -> !p.getFileName().toString().equals("index.xml") && Files.isRegularFile(p));

            if (!hasSourceFile) {
                LOG.warn("No source code files were found inside the 'src-archive' directory. File: {}", this.fprPath);
            }
            return hasSourceFile;

        } catch (IOException e) {
            throw new FcliTechnicalException("Error reading src-archive directory in FPR", e);
        }
    }

    /**
     * Checks if the FPR contains a remediations.xml file.
     * @return true if the file exists.
     */
    public boolean hasRemediations() {
        return Files.exists(getPath("/remediations.xml"));
    }

    @Override
    public void close() throws IOException {
        if (zipfs != null && zipfs.isOpen()) {
            zipfs.close();
        }
    }

    /**
     * Returns the map of relative source file paths to their paths within the FPR archive.
     * @return A map of source file paths.
     */
    public synchronized Map<String, String> getSourceFileMap() {
        if (sourceFileMap == null) {
            //03358915/OCTCR11A2429097 fix for audit issue
            boolean dastOnly = Files.exists(zipfs.getPath("/webinspect.xml"))
                && !Files.exists(zipfs.getPath("/audit.fvdl"));
            sourceFileMap = dastOnly
                ? new ConcurrentHashMap<>()
                : loadSourceFileMap();
            //03358915/OCTCR11A2429097 fix for audit issue
        }
        return sourceFileMap;
    }

    /**
     * Loads the source file mapping from src-archive/index.xml within the FPR.
     * This is called by the constructor.
     */
    private Map<String, String> loadSourceFileMap() {
        Map<String, String> map = new ConcurrentHashMap<>();
        Path indexPath = getPath("/src-archive/index.xml");
        if (!Files.exists(indexPath)) {
            LOG.warn("FPR is missing 'src-archive/index.xml'. Source file lookups will fail.");
            return map;
        }

        try {
            byte[] indexBytes = Files.readAllBytes(indexPath);
            String xmlForValidation = new String(indexBytes, java.nio.charset.StandardCharsets.ISO_8859_1);

            validateDoctype(xmlForValidation, indexPath);

            if (containsDoctype(xmlForValidation)) {
                loadPropertiesFormat(indexBytes, map);
            } else {
                loadEntryXmlFormat(indexBytes, map);
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new FcliTechnicalException("Failed to parse src-archive/index.xml in FPR", e);
        }
        return map;
    }

    private void loadEntryXmlFormat(byte[] indexBytes, Map<String, String> map)
            throws ParserConfigurationException, IOException, SAXException {
            var factory = SecureXmlParserFactory.newDocumentBuilderFactoryWithoutDoctype(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document indexDoc = builder.parse(new ByteArrayInputStream(indexBytes));

        NodeList entryNodes = indexDoc.getElementsByTagName("entry");
        for (int i = 0; i < entryNodes.getLength(); i++) {
            Element entry = (Element) entryNodes.item(i);
            String key = entry.getAttribute("key");
            String value = entry.getTextContent();
            map.put(key, value);
        }
    }

    private void loadPropertiesFormat(byte[] indexBytes, Map<String, String> map) throws IOException {
        Properties properties = new Properties();
        properties.loadFromXML(new ByteArrayInputStream(indexBytes));

        for (String key : properties.stringPropertyNames()) {
            map.put(key, properties.getProperty(key));
        }
    }

    private void validateDoctype(String xmlContent, Path indexPath) throws IOException {
        if (xmlContent.contains("<!ENTITY")) {
            throw new IOException("index.xml contains ENTITY declarations, which are not supported: " + indexPath);
        }

        int doctypeStart = xmlContent.indexOf("<!DOCTYPE");
        if (doctypeStart < 0) {
            return;
        }

        int doctypeEnd = xmlContent.indexOf('>', doctypeStart);
        if (doctypeEnd < 0) {
            throw new IOException("index.xml contains an unterminated DOCTYPE declaration: " + indexPath);
        }

        String declaration = xmlContent.substring(doctypeStart, doctypeEnd + 1).trim();
        if (declaration.contains("[")) {
            throw new IOException("index.xml contains an internal DTD subset, which is not supported: " + indexPath);
        }

        if (!isSupportedPropertiesDoctype(declaration)) {
            throw new IOException("index.xml contains an unsupported DOCTYPE declaration: " + indexPath);
        }
    }

    private boolean isSupportedPropertiesDoctype(String declaration) {
        return PROPERTIES_DOCTYPE_PATTERN.matcher(declaration).matches();
    }

    private boolean containsDoctype(String xmlContent) {
        return xmlContent.contains("<!DOCTYPE");
    }
}
