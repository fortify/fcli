package com.fortify.cli.aviator.fpr.model;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.FilterTemplate;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class FPRInfo {
    private String uuid;
    private String buildId;
    private String FPRName;
    private String sourceBasePath;
    private int numberOfFiles;
    private int scanTime;
    private FilterTemplate filterTemplate;
    private FilterSet defaultEnabledFilterSet;
    private String resultsTag;

    Logger logger = LoggerFactory.getLogger(FPRInfo.class);

    public FPRInfo(FprHandle fprHandle) {
        FPRName = String.valueOf(fprHandle.getFprPath().getFileName());
        try {
            extractInfoFromAuditFvdl(fprHandle);
        } catch (Exception e) {
            // It's better to wrap this in a specific runtime exception
            throw new RuntimeException("Failed to extract info from audit.fvdl", e);
        }
    }

    private void extractInfoFromAuditFvdl(FprHandle fprHandle) throws Exception {
        Path auditPath = fprHandle.getPath("/audit.fvdl");

        if (!Files.exists(auditPath)) {
            throw new IllegalStateException("audit.fvdl not found in FPR: " + fprHandle.getFprPath());
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document auditDoc;
        try (InputStream auditStream = Files.newInputStream(auditPath)) {
            auditDoc = builder.parse(auditStream);
        }

        NodeList uuidNodes = auditDoc.getElementsByTagName("UUID");
        if (uuidNodes.getLength() > 0) {
            this.uuid = uuidNodes.item(0).getTextContent();
        }

        NodeList buildNodes = auditDoc.getElementsByTagName("Build");
        if (buildNodes.getLength() > 0) {
            Element buildElement = (Element) buildNodes.item(0);
            this.buildId = getFirstElementContent(buildElement, "BuildID", "");
            this.sourceBasePath = getFirstElementContent(buildElement, "SourceBasePath", "");
            this.numberOfFiles = parseIntegerContent(getFirstElementContent(buildElement, "NumberFiles", "0"));
            this.scanTime = parseIntegerContent(getFirstElementContent(buildElement, "ScanTime", "0"));
        }
    }

    private String getFirstElementContent(Element parent, String tagName, String defaultValue) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null) {
            return nodes.item(0).getTextContent();
        }
        return defaultValue;
    }

    private int parseIntegerContent(String content) {

        if (StringUtil.isEmpty(content)) {
            return 0;
        }

        try {
            return Integer.parseInt(content);
        } catch (NumberFormatException e) {
            logger.warn("WARN: Error parsing integer: {}", content);
            return 0;
        }
    }

    public Optional<FilterSet> getDefaultEnabledFilterSet() {
        if (filterTemplate == null || filterTemplate.getFilterSets() == null) {
            return Optional.empty();
        }

        return filterTemplate.getFilterSets().stream()
                .filter(FilterSet::isEnabled)
                .findFirst();
    }
}