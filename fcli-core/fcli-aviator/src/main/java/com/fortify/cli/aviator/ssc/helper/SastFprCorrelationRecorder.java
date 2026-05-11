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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
 * Reads and writes the {@code DAST_CORRELATION_STATUS} custom tag inside
 * {@code audit.xml} of the SAST FPR.
 *
 * <p>The tag persists the outcome of each `(SAST instanceId, DAST issueId)` pairing —
 * either CORRELATED or REJECTED — so that subsequent runs can skip pairs that have
 * already been tried.
 *
 * <p>Tag value format (500-char SSC limit):
 * <pre>CORRELATED::DAST-1,DAST-2|REJECTED::DAST-3,DAST-4</pre>
 *
 * <ul>
 *   <li>Status groups separated by {@code |}</li>
 *   <li>Each group: {@code STATUS::dastId1,dastId2,...}</li>
 *   <li>Possible statuses: {@code CORRELATED}, {@code REJECTED}</li>
 * </ul>
 */
public final class SastFprCorrelationRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(SastFprCorrelationRecorder.class);

    /** Fixed UUID used as the tag ID in audit.xml for all DAST correlation status entries. */
    static final String DAST_CORRELATION_TAG_ID = "7A3B5C9D-1E2F-4A8B-9C0D-E1F2A3B4C5D6";

    private static final String STATUS_CORRELATED = "CORRELATED";
    private static final String STATUS_REJECTED    = "REJECTED";
    private static final String NS_AUDIT = "xmlns://www.fortify.com/schema/audit";

    /** SSC tag value length limit. */
    private static final int MAX_TAG_VALUE_LENGTH = 500;

    private SastFprCorrelationRecorder() {}

    // ─── Read ──────────────────────────────────────────────────────────────────

    /**
     * Reads all tried pair keys — both CORRELATED and REJECTED — from the
     * {@code DAST_CORRELATION_STATUS} tag in {@code audit.xml} of the SAST FPR.
     *
     * @param sastFprPath path to the downloaded SAST FPR file
     * @return set of {@code "sastInstanceId::dastIssueId"} keys for all already-tried pairs;
     *         empty set if no tags are present or the audit.xml cannot be read
     */
    public static Set<String> readTriedPairKeys(Path sastFprPath) {
        Set<String> keys = new HashSet<>();
        try (FprHandle fprHandle = new FprHandle(sastFprPath)) {
            Path auditPath = fprHandle.getPath("/audit.xml");
            if (!Files.exists(auditPath)) {
                LOG.debug("audit.xml not found in SAST FPR; returning empty tried-pair set.");
                return keys;
            }
            Document doc = parseXml(auditPath);
            NodeList issueNodes = doc.getElementsByTagNameNS(NS_AUDIT, "Issue");

            for (int i = 0; i < issueNodes.getLength(); i++) {
                if (!(issueNodes.item(i) instanceof Element issue)) continue;
                String instanceId = issue.getAttribute("instanceId");
                if (instanceId == null || instanceId.isEmpty()) continue;

                String tagValue = findCorrelationTagValue(issue);
                if (tagValue == null || tagValue.isEmpty()) continue;

                parseTagValue(tagValue).forEach((dastId, status) ->
                    keys.add(instanceId + "::" + dastId));
            }
        } catch (Exception e) {
            LOG.warn("Could not read DAST_CORRELATION_STATUS tags from SAST FPR — pairs will be retried: {}", e.getMessage());
        }
        LOG.debug("Read {} already-tried pair keys from SAST FPR audit.xml", keys.size());
        return keys;
    }

    // ─── Write ─────────────────────────────────────────────────────────────────

    /**
     * Merges the new correlation run results into the {@code DAST_CORRELATION_STATUS}
     * tags in {@code audit.xml} and writes the updated file back into the SAST FPR ZIP.
     *
     * <p>Merge rule: {@code CORRELATED} is sticky — a pair confirmed in any run
     * cannot be downgraded to {@code REJECTED} by a later run.
     *
     * @param sastFprPath    path to the SAST FPR file (modified in-place inside the ZIP FS)
     * @param confirmedPairs pairs confirmed by this run's gRPC validation
     * @param rejectedPairs  pairs rejected by this run's gRPC validation
     */
    @SneakyThrows
    public static void writeCorrelationTags(Path sastFprPath,
                                            List<CorrelatedPair> confirmedPairs,
                                            List<CorrelatedPair> rejectedPairs) {
        if (confirmedPairs.isEmpty() && rejectedPairs.isEmpty()) {
            LOG.debug("No new correlation results to write; skipping audit.xml update.");
            return;
        }

        Map<String, Map<String, String>> incoming = buildIncomingMap(confirmedPairs, rejectedPairs);
        logIncomingMap(incoming);

        try (FprHandle fprHandle = new FprHandle(sastFprPath)) {
            Path auditPath = fprHandle.getPath("/audit.xml");
            if (!Files.exists(auditPath)) {
                LOG.warn("audit.xml not found in SAST FPR; cannot write DAST_CORRELATION_STATUS tags.");
                return;
            }

            Document doc = parseXml(auditPath);
            Set<String> patchedIds = patchExistingIssues(doc, incoming);
            int createdCount = createNewIssueEntries(doc, incoming, patchedIds);

            writeXml(doc, auditPath);
            LOG.info("writeCorrelationTags complete: patched={} existing, created={} new <Issue> entries (incoming={})",
                patchedIds.size(), createdCount, incoming.size());
        }
    }

    private static void logIncomingMap(Map<String, Map<String, String>> incoming) {
        LOG.info("writeCorrelationTags: incoming map has {} SAST entries", incoming.size());
        incoming.forEach((sastId, dastMap) -> {
            LOG.info("  SAST instanceId: {}", sastId);
            dastMap.forEach((dastId, status) -> LOG.info("    {} = {}", dastId, status));
        });
    }

    private static Set<String> patchExistingIssues(Document doc, Map<String, Map<String, String>> incoming) {
        NodeList issueNodes = doc.getElementsByTagNameNS(NS_AUDIT, "Issue");
        LOG.info("writeCorrelationTags: found {} existing <Issue> nodes in audit.xml", issueNodes.getLength());

        Set<String> patchedIds = new HashSet<>();
        for (int i = 0; i < issueNodes.getLength(); i++) {
            if (!(issueNodes.item(i) instanceof Element issue)) continue;
            String instanceId = issue.getAttribute("instanceId");
            if (instanceId == null || instanceId.isEmpty()) continue;
            patchedIds.add(instanceId);

            Map<String, String> newEntries = incoming.get(instanceId);
            if (newEntries == null || newEntries.isEmpty()) continue;

            String newValue = mergeAndBuildTagValue(instanceId, findCorrelationTagValue(issue), newEntries);
            upsertCorrelationTag(doc, issue, newValue);
            LOG.info("writeCorrelationTags: patched existing <Issue> instanceId='{}' → value='{}'", instanceId, newValue);
        }
        return patchedIds;
    }

    private static int createNewIssueEntries(Document doc, Map<String, Map<String, String>> incoming,
                                              Set<String> existingInstanceIds) {
        Element issueList = (Element) doc.getElementsByTagNameNS(NS_AUDIT, "IssueList").item(0);
        if (issueList == null) {
            issueList = (Element) doc.getElementsByTagName("IssueList").item(0);
        }

        int createdCount = 0;
        for (var entry : incoming.entrySet()) {
            String instanceId = entry.getKey();
            if (existingInstanceIds.contains(instanceId)) continue;
            if (issueList == null) {
                LOG.warn("writeCorrelationTags: <IssueList> not found in audit.xml; cannot create entry for instanceId='{}'", instanceId);
                continue;
            }

            String newValue = mergeAndBuildTagValue(instanceId, null, entry.getValue());
            Element newIssue = createIssueElement(doc, issueList, instanceId);
            upsertCorrelationTag(doc, newIssue, newValue);
            issueList.appendChild(newIssue);
            LOG.info("writeCorrelationTags: created new <Issue> instanceId='{}' → value='{}'", instanceId, newValue);
            createdCount++;
        }
        return createdCount;
    }

    private static String mergeAndBuildTagValue(String instanceId, String existingValue, Map<String, String> newEntries) {
        Map<String, String> merged = parseTagValue(existingValue);
        mergeEntries(merged, newEntries);
        String value = buildTagValue(merged);
        if (value.length() > MAX_TAG_VALUE_LENGTH) {
            LOG.warn("DAST_CORRELATION_STATUS tag value for SAST {} exceeds {} chars; truncating.", instanceId, MAX_TAG_VALUE_LENGTH);
            value = truncateTagValue(merged);
        }
        return value;
    }

    private static Element createIssueElement(Document doc, Element issueList, String instanceId) {
        String ns = issueList.getNamespaceURI();
        Element issue = (ns != null) ? doc.createElementNS(ns, "Issue") : doc.createElement("Issue");
        issue.setAttribute("instanceId", instanceId);
        issue.setAttribute("revision", "0");
        issue.setAttribute("suppressed", "false");
        return issue;
    }

    // ─── Merge helpers ─────────────────────────────────────────────────────────

    private static Map<String, Map<String, String>> buildIncomingMap(
            List<CorrelatedPair> confirmed, List<CorrelatedPair> rejected) {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        for (CorrelatedPair p : confirmed) {
            map.computeIfAbsent(p.sastInstanceId(), k -> new LinkedHashMap<>())
               .put(p.dastIssueId(), STATUS_CORRELATED);
        }
        for (CorrelatedPair p : rejected) {
            map.computeIfAbsent(p.sastInstanceId(), k -> new LinkedHashMap<>())
               .put(p.dastIssueId(), STATUS_REJECTED);
        }
        return map;
    }

    /**
     * Merges {@code newEntries} into {@code existing}. CORRELATED is sticky:
     * a CORRELATED entry is never overwritten by REJECTED.
     */
    private static void mergeEntries(Map<String, String> existing, Map<String, String> newEntries) {
        for (var entry : newEntries.entrySet()) {
            String dastId = entry.getKey();
            String newStatus = entry.getValue();
            String oldStatus = existing.get(dastId);
            if (STATUS_CORRELATED.equals(oldStatus)) continue; // sticky — never downgrade
            existing.put(dastId, newStatus);
        }
    }

    // ─── Tag value encode/decode ────────────────────────────────────────────────

    /**
     * Parses {@code "CORRELATED::D1,D2|REJECTED::D3"} into a {@code Map<dastId, status>}.
     * Returns an empty map if {@code value} is null or empty.
     */
    static Map<String, String> parseTagValue(String value) {
        Map<String, String> map = new LinkedHashMap<>();
        if (value == null || value.isEmpty()) return map;
        for (String group : value.split("\\|")) {
            int sep = group.indexOf("::");
            if (sep < 0) continue;
            String status = group.substring(0, sep).trim();
            String ids = group.substring(sep + 2).trim();
            for (String dastId : ids.split(",")) {
                String trimmed = dastId.trim();
                if (!trimmed.isEmpty()) map.put(trimmed, status);
            }
        }
        return map;
    }

    /**
     * Encodes a {@code Map<dastId, status>} into
     * {@code "CORRELATED::D1,D2|REJECTED::D3"} format.
     */
    static String buildTagValue(Map<String, String> dastIdToStatus) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        grouped.put(STATUS_CORRELATED, new ArrayList<>());
        grouped.put(STATUS_REJECTED, new ArrayList<>());
        dastIdToStatus.forEach((dastId, status) ->
            grouped.computeIfAbsent(status, k -> new ArrayList<>()).add(dastId));

        return grouped.entrySet().stream()
            .filter(e -> !e.getValue().isEmpty())
            .map(e -> e.getKey() + "::" + String.join(",", e.getValue()))
            .collect(Collectors.joining("|"));
    }

    /**
     * Truncates the merged map so the resulting tag value fits within
     * {@link #MAX_TAG_VALUE_LENGTH} characters. Removes entries from the end
     * of the REJECTED list first (CORRELATED entries are preserved).
     */
    private static String truncateTagValue(Map<String, String> merged) {
        var working = new LinkedHashMap<>(merged);
        while (true) {
            String candidate = buildTagValue(working);
            if (candidate.length() <= MAX_TAG_VALUE_LENGTH) return candidate;
            // Remove last REJECTED entry to shrink
            String lastRejectedKey = null;
            for (var e : working.entrySet()) {
                if (STATUS_REJECTED.equals(e.getValue())) lastRejectedKey = e.getKey();
            }
            if (lastRejectedKey == null) {
                // Fallback: just hard-truncate (should not normally happen)
                return candidate.substring(0, MAX_TAG_VALUE_LENGTH);
            }
            working.remove(lastRejectedKey);
        }
    }

    // ─── DOM helpers ────────────────────────────────────────────────────────────

    private static String findCorrelationTagValue(Element issue) {
        NodeList tagNodes = issue.getElementsByTagNameNS(NS_AUDIT, "Tag");
        for (int i = 0; i < tagNodes.getLength(); i++) {
            if (!(tagNodes.item(i) instanceof Element tag)) continue;
            if (DAST_CORRELATION_TAG_ID.equalsIgnoreCase(tag.getAttribute("id"))) {
                NodeList values = tag.getElementsByTagNameNS(NS_AUDIT, "Value");
                if (values.getLength() > 0) return values.item(0).getTextContent().trim();
            }
        }
        return null;
    }

    /**
     * Upserts the {@code <Tag id="DAST_CORRELATION_TAG_ID">} element on the issue.
     * If the tag already exists, its {@code <Value>} is updated in-place.
     * If not, a new {@code <Tag>} element is appended to the issue.
     */
    private static void upsertCorrelationTag(Document doc, Element issue, String value) {
        // Try to find and update existing tag
        LOG.info("Upserting Correlation Tag");
        NodeList tagNodes = issue.getElementsByTagNameNS(NS_AUDIT, "Tag");
        for (int i = 0; i < tagNodes.getLength(); i++) {
            if (!(tagNodes.item(i) instanceof Element tag)) continue;
            if (DAST_CORRELATION_TAG_ID.equalsIgnoreCase(tag.getAttribute("id"))) {
                NodeList values = tag.getElementsByTagNameNS(NS_AUDIT, "Value");
                if (values.getLength() > 0) {
                    values.item(0).setTextContent(value);
                } else {
                    Element val = doc.createElementNS(NS_AUDIT, "Value");
                    val.setTextContent(value);
                    tag.appendChild(val);
                }
                return;
            }
        }
        // Not found — append new tag with correct namespace
        Element tag = doc.createElementNS(NS_AUDIT, "Tag");
        tag.setAttribute("id", DAST_CORRELATION_TAG_ID);
        Element val = doc.createElementNS(NS_AUDIT, "Value");
        val.setTextContent(value);
        tag.appendChild(val);
        issue.appendChild(tag);
    }

    @SneakyThrows
    private static Document parseXml(Path path) {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);   // must be true to find ns0:Issue / ns0:Tag by local name
        return factory.newDocumentBuilder().parse(Files.newInputStream(path));
    }

    @SneakyThrows
    private static void writeXml(Document doc, Path path) {
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        try (OutputStream os = Files.newOutputStream(path)) {
            transformer.transform(new DOMSource(doc), new StreamResult(os));
        }
    }
}
