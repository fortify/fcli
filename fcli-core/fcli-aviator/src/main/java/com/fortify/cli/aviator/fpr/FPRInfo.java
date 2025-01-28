package com.fortify.cli.aviator.fpr;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.FilterTemplate;
import com.fortify.cli.aviator.util.StringUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;


@Getter
@Setter
public class FPRInfo {
    private String uuid;
    private String buildId;
    private String sourceBasePath;
    private int numberOfFiles;
    private int scanTime;
    private FilterTemplate filterTemplate;
    private FilterSet defaultEnabledFilterSet;
    private String resultsTag;

    Logger logger = LoggerFactory.getLogger(FPRInfo.class);

    public FPRInfo(Path extractedPath) {
        try {
            extractInfoFromAuditFvdl(extractedPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void extractInfoFromAuditFvdl(Path extractedPath) throws Exception {
        Path auditPath = extractedPath.resolve("audit.fvdl");

        if (!Files.exists(auditPath)) {
            throw new IllegalStateException("audit.fvdl not found in " + extractedPath);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document auditDoc = builder.parse(auditPath.toFile());

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
            logger.warn("Error parsing integer: {}", content);
            return 0;
        }
    }
}