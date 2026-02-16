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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class IndexXMLProcessor {
    private static final Logger logger = LoggerFactory.getLogger(IndexXMLProcessor.class);
    private final Map<String, String> sourceFileMap;
    private final Path extractedPath;


    public IndexXMLProcessor(Path extractedPath, Map<String, String> sourceFileMap) {
        this.extractedPath = extractedPath;
        this.sourceFileMap = sourceFileMap;
    }

    /**
     * Loads the source file map from FVDL.
     */
    public void loadSourceFileMap() throws Exception {
        Path srcArchiveDir = extractedPath.resolve("src-archive");
        Path indexPath = null;

        if (directoryContainsSourceFiles(srcArchiveDir)) {
            indexPath = srcArchiveDir.resolve("index.xml");
        }

        if (indexPath == null) {
            throw new NoSuchFileException("'src-archive' contained no source files under " + extractedPath);
        } else if (!Files.exists(indexPath)) {
            throw new NoSuchFileException("A source directory was found, but its 'index.xml' is missing at: " + indexPath);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document indexDoc = builder.parse(indexPath.toFile());

        NodeList entryNodes = indexDoc.getElementsByTagName("entry");
        for (int i = 0; i < entryNodes.getLength(); i++) {
            Element entry = (Element) entryNodes.item(i);
            String key = entry.getAttribute("key");
            String value = entry.getTextContent();
            sourceFileMap.put(key, value);
        }
    }

    private boolean directoryContainsSourceFiles(Path dirPath) throws IOException {
        if (!Files.isDirectory(dirPath)) {
            return false;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path path : stream) {
                boolean isRegularFile = Files.isRegularFile(path);
                boolean isNotIndexXml = !path.getFileName().toString().equals("index.xml");

                if (isRegularFile && isNotIndexXml) {
                    return true;
                }
            }
        }

        return false;
    }

}
