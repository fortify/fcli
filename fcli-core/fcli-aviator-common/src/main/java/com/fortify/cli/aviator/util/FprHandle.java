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
package com.fortify.cli.aviator.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

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

import lombok.Getter;

/**
 * Represents an open FPR file using Java's Zip File System Provider.
 * This class centralizes access to the FPR's contents without physical extraction,
 * and ensures resources are properly managed via the AutoCloseable interface.
 * All file access within the FPR should go through this class.
 */
public final class FprHandle implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FprHandle.class);

    private final FileSystem zipfs;
    /**
     * -- GETTER --
     *  Returns the original path of the FPR file on the host file system.
     *
     * @return The Path to the source .fpr file.
     */
    @Getter
    private final Path fprPath;
    private final Map<String, String> sourceFileMap;


    /**
     * Opens an FPR file as a virtual file system. Should be used in a try-with-resources block.
     * @param fprPath The path to the .fpr file.
     * @throws AviatorTechnicalException if the file cannot be opened as a zip file system.
     */
    public FprHandle(Path fprPath) {
        if (fprPath == null || !Files.exists(fprPath)) {
            throw new AviatorTechnicalException("FPR file path is null or does not exist: " + fprPath);
        }
        this.fprPath = fprPath;
        try {
            // Using null for the ClassLoader is important to avoid potential conflicts.
            this.zipfs = FileSystems.newFileSystem(fprPath, (ClassLoader) null);
        } catch (IOException e) {
            throw new AviatorTechnicalException("Failed to open FPR as a zip file system: " + fprPath, e);
        }
        this.sourceFileMap = loadSourceFileMap();
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
     * @throws AviatorSimpleException if the FPR is invalid for processing.
     */
    public void validate() {
        if (!Files.exists(getPath("/audit.fvdl"))) {
            if (Files.exists(getPath("/webinspect.xml"))) {
                throw new AviatorSimpleException("Invalid FPR: The provided file is a DAST (WebInspect) scan result. Fortify Aviator requires an FPR from a SAST scan.");
            }
            throw new AviatorSimpleException("Invalid FPR: The file does not contain 'audit.fvdl' and does not appear to be a valid SAST scan result.");
        }
        if (!hasSource()) {
            throw new AviatorSimpleException("Invalid FPR: Source code is missing or incomplete. The 'src-archive' directory must contain 'index.xml' and at least one source file.");
        }
        LOG.info("FPR validation successful for: {}", this.fprPath);
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
            throw new AviatorTechnicalException("Error reading src-archive directory in FPR", e);
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
    public Map<String, String> getSourceFileMap() {
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

        try (InputStream indexStream = Files.newInputStream(indexPath)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document indexDoc = builder.parse(indexStream);

            NodeList entryNodes = indexDoc.getElementsByTagName("entry");
            for (int i = 0; i < entryNodes.getLength(); i++) {
                Element entry = (Element) entryNodes.item(i);
                String key = entry.getAttribute("key");
                String value = entry.getTextContent();
                map.put(key, value);
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new AviatorTechnicalException("Failed to parse src-archive/index.xml in FPR", e);
        }
        return map;
    }
}