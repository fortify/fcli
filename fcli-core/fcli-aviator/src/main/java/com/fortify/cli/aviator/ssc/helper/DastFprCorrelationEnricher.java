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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
public class DastFprCorrelationEnricher {
    private static final Logger LOG = LoggerFactory.getLogger(DastFprCorrelationEnricher.class);
    private static final String AI_CORRELATION_SESSION_ID = "AI_CORRELATION_METADATA";
    private static final DateTimeFormatter HTTP_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);

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
            upsertCorrelationMetadataSession(doc);
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

    /**
     * Appends a synthetic {@code <Session requestId="AI_CORRELATION_METADATA">} at the end
     * of the document root if one does not already exist, or updates its Date header if it does.
     * This session acts as a lightweight metadata marker for when the AI correlation was run,
     * allowing SSC to merge without requiring deletion of the prior DAST scan.
     */
    private void upsertCorrelationMetadataSession(Document doc) {
        String nowHttpDate = OffsetDateTime.now(ZoneOffset.UTC).format(HTTP_DATE_FORMATTER);
        Element root = doc.getDocumentElement();

        // Check if the metadata session already exists — update if so
        NodeList sessions = root.getElementsByTagName("Session");
        for (int i = 0; i < sessions.getLength(); i++) {
            if (!(sessions.item(i) instanceof Element s)) continue;
            if (AI_CORRELATION_SESSION_ID.equals(s.getAttribute("requestId"))) {
                updateDateHeader(s, nowHttpDate);
                LOG.info("Updated existing AI_CORRELATION_METADATA session Date header to: {}", nowHttpDate);
                return;
            }
        }

        // Not found — append a new synthetic session at the end of the root element
        root.appendChild(buildCorrelationMetadataSession(doc, nowHttpDate));
        LOG.info("Appended new AI_CORRELATION_METADATA session with Date: {}", nowHttpDate);
    }

    private void updateDateHeader(Element sessionElement, String newDate) {
        NodeList headers = sessionElement.getElementsByTagName("Header");
        for (int i = 0; i < headers.getLength(); i++) {
            if (!(headers.item(i) instanceof Element header)) continue;
            NodeList names = header.getElementsByTagName("Name");
            if (names.getLength() > 0 && "Date".equals(names.item(0).getTextContent())) {
                NodeList values = header.getElementsByTagName("Value");
                if (values.getLength() > 0) {
                    values.item(0).setTextContent(newDate);
                }
                return;
            }
        }
    }

    private Element buildCorrelationMetadataSession(Document doc, String httpDate) {
        Element session = doc.createElement("Session");
        session.setAttribute("requestId", AI_CORRELATION_SESSION_ID);

        // Empty structural elements required by the WebInspect schema
        for (String tag : new String[]{"URL", "Scheme", "Host", "Port", "AttackParamDescriptor", "Issues", "RawResponse"}) {
            session.appendChild(doc.createElement(tag));
        }

        // RawRequest with matching requestId
        Element rawRequest = doc.createElement("RawRequest");
        rawRequest.setAttribute("id", AI_CORRELATION_SESSION_ID);
        session.appendChild(rawRequest);

        // Request with all required empty children
        Element request = doc.createElement("Request");
        for (String tag : new String[]{"Method", "Path", "File", "Ext", "PageMark", "HTTPVersion",
                "FullQuery", "FullPostData", "XMLPostData", "MultiPartPostData",
                "RawASCIIPostData", "Cookie", "Queries", "Headers", "Cookies"}) {
            request.appendChild(doc.createElement(tag));
        }
        session.appendChild(request);

        // Response with Date header
        Element response = doc.createElement("Response");
        for (String tag : new String[]{"HTTPVersion", "StatusCode", "StatusDescription", "SetCookie"}) {
            response.appendChild(doc.createElement(tag));
        }
        Element responseHeaders = doc.createElement("Headers");
        Element header = doc.createElement("Header");
        appendChildElement(doc, header, "Name", "Date");
        appendChildElement(doc, header, "Value", httpDate);
        responseHeaders.appendChild(header);
        response.appendChild(responseHeaders);
        response.appendChild(doc.createElement("SetCookies"));
        response.appendChild(doc.createElement("Forms"));
        session.appendChild(response);

        return session;
    }
}
