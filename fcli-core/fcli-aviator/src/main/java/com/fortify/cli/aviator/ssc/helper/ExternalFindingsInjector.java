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
package com.fortify.cli.aviator.ssc.helper;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fortify.cli.aviator.grpc.CorrelatedPair;
import com.fortify.cli.aviator.util.FprHandle;

import lombok.SneakyThrows;

/**
 * Injects {@code <ExternalFindings>} elements into a DAST FPR's
 * {@code webinspect.xml} so that SSC can display SAST–DAST correlation links.
 *
 * <p>After injection the modified FPR can be uploaded to SSC; the existing
 * upload/parse pipeline will read the {@code <ExternalFindings>} and create
 * correlation records without any SSC code changes.
 */
public class ExternalFindingsInjector {
    private static final Logger LOG = LoggerFactory.getLogger(ExternalFindingsInjector.class);

    /**
     * Injects correlation data into the DAST FPR and returns the path to
     * the modified file (which is the same file, modified in-place inside
     * the zip filesystem).
     *
     * @param dastFprPath    path to the downloaded DAST FPR
     * @param confirmedPairs all confirmed correlated pairs (new + previous)
     * @return the same {@code dastFprPath}, now containing injected data
     */
    @SneakyThrows
    public Path injectAndRepackage(Path dastFprPath, List<CorrelatedPair> confirmedPairs) {
        if (confirmedPairs == null || confirmedPairs.isEmpty()) {
            LOG.info("No correlated pairs to inject; returning DAST FPR unchanged.");
            return dastFprPath;
        }

        // Group pairs by DAST issue ID for efficient lookup
        Map<String, List<CorrelatedPair>> pairsByDastId = groupByDastId(confirmedPairs);

        try (FprHandle fprHandle = new FprHandle(dastFprPath)) {
            Path webinspectPath = fprHandle.getPath("/webinspect.xml");
            if (!Files.exists(webinspectPath)) {
                LOG.warn("DAST FPR does not contain webinspect.xml; skipping injection.");
                return dastFprPath;
            }

            Document doc = parseXml(webinspectPath);
            int injectedCount = injectAll(doc, pairsByDastId);
            writeXml(doc, webinspectPath);

            LOG.info("Injected ExternalFindings for {} DAST issues ({} total pairs)",
                injectedCount, confirmedPairs.size());
        }

        return dastFprPath;
    }

    private int injectAll(Document doc, Map<String, List<CorrelatedPair>> pairsByDastId) {
        NodeList issueNodes = doc.getElementsByTagName("Issue");
        int injectedCount = 0;

        for (int i = 0; i < issueNodes.getLength(); i++) {
            if (!(issueNodes.item(i) instanceof Element issue)) continue;

            String issueId = issue.getAttribute("id");
            List<CorrelatedPair> pairs = pairsByDastId.get(issueId);
            if (pairs == null || pairs.isEmpty()) continue;

            // Remove any existing ExternalFindings to avoid duplicates on re-run
            removeExistingExternalFindings(issue);

            Element externalFindings = doc.createElement("ExternalFindings");
            String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            for (CorrelatedPair pair : pairs) {
                Element ef = doc.createElement("ExternalFinding");
                ef.setAttribute("Origin", "SCA");

                appendChildElement(doc, ef, "OriginID", pair.scanGuid());
                appendChildElement(doc, ef, "OriginFindingID", pair.sastInstanceId());
                appendChildElement(doc, ef, "OriginDateTime", timestamp);

                externalFindings.appendChild(ef);
            }

            issue.appendChild(externalFindings);
            injectedCount++;
        }

        return injectedCount;
    }

    private void removeExistingExternalFindings(Element issue) {
        NodeList existing = issue.getElementsByTagName("ExternalFindings");
        // Collect first, then remove (to avoid ConcurrentModificationException)
        List<org.w3c.dom.Node> toRemove = new ArrayList<>();
        for (int i = 0; i < existing.getLength(); i++) {
            if (existing.item(i).getParentNode() == issue) {
                toRemove.add(existing.item(i));
            }
        }
        for (org.w3c.dom.Node node : toRemove) {
            issue.removeChild(node);
        }
    }

    private void appendChildElement(Document doc, Element parent, String name, String value) {
        Element child = doc.createElement(name);
        child.setTextContent(value != null ? value : "");
        parent.appendChild(child);
    }

    private Map<String, List<CorrelatedPair>> groupByDastId(List<CorrelatedPair> pairs) {
        Map<String, List<CorrelatedPair>> map = new LinkedHashMap<>();
        for (CorrelatedPair pair : pairs) {
            map.computeIfAbsent(pair.dastIssueId(), k -> new ArrayList<>()).add(pair);
        }
        return map;
    }

    @SneakyThrows
    private Document parseXml(Path path) {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(Files.newInputStream(path));
    }

    @SneakyThrows
    private void writeXml(Document doc, Path path) {
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        try (OutputStream os = Files.newOutputStream(path)) {
            transformer.transform(new DOMSource(doc), new StreamResult(os));
        }
    }
}
