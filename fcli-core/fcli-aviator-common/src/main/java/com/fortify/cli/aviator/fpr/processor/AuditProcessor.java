package com.fortify.cli.aviator.fpr.processor;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.config.TagMappingConfig;

import lombok.Getter;


public class AuditProcessor {

    Logger logger = LoggerFactory.getLogger(AuditProcessor.class);
    private static final String AUDIT_NAMESPACE_URI = "xmlns://www.fortify.com/schema/audit";
    private static final String REMEDIATIONS_NAMESPACE_URI = "xmlns://www.fortify.com/schema/remediations";


    private Document auditDoc;
    @Setter
    private Document filterTemplateDoc;
    private Document remediationsDoc;

    private final Map<String, AuditIssue> auditIssueMap = new HashMap<>();
    private final String fprFilePath;

    public AuditProcessor(Path extractedPath, String fprFilePath) {
        this.extractedPath = extractedPath;
        this.fprFilePath = fprFilePath;
    }

    @Getter
    private final Path extractedPath;

    public Map<String, AuditIssue> processAuditXML() throws AviatorTechnicalException {
        Path auditPath = extractedPath.resolve("audit.xml");

        try {
            if (!Files.exists(auditPath)) {
                logger.debug("audit.xml not found. Creating a default audit.xml.");
                createDefaultAuditXml(auditPath);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            auditDoc = builder.parse(auditPath.toFile());

            NodeList issueNodes = auditDoc.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Issue");
            for (int i = 0; i < issueNodes.getLength(); i++) {
                Element issueElement = (Element) issueNodes.item(i);
                AuditIssue auditIssue = processAuditIssue(issueElement);
                auditIssueMap.put(auditIssue.getInstanceId(), auditIssue);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.error("Error parsing audit.xml file: {}", auditPath, e);
            throw new AviatorTechnicalException("Error processing audit.xml file.", e);
        } catch (AviatorTechnicalException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error processing audit.xml: {}", auditPath, e);
            throw new AviatorTechnicalException("Unexpected error processing audit.xml.", e);
        }

        return auditIssueMap;
    }

    private void createDefaultAuditXml(Path auditPath) throws AviatorTechnicalException {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            docFactory.setNamespaceAware(true);

            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElementNS(AUDIT_NAMESPACE_URI, "ns2:Audit");
            doc.appendChild(rootElement);
            rootElement.setPrefix("ns2");

            rootElement.setAttribute("xmlns:ns3", "xmlns://www.fortifysoftware.com/schema/activitytemplate");
            rootElement.setAttribute("xmlns:ns4", "xmlns://www.fortifysoftware.com/schema/wsTypes");
            rootElement.setAttribute("xmlns:ns5", "xmlns://www.fortify.com/schema/issuemanagement");
            rootElement.setAttribute("xmlns:ns6", "http://www.fortify.com/schema/fws");
            rootElement.setAttribute("xmlns:ns7", "xmlns://www.fortifysoftware.com/schema/runtime");
            rootElement.setAttribute("xmlns:ns8", "xmlns://www.fortifysoftware.com/schema/seed");
            rootElement.setAttribute("xmlns:ns9", "xmlns://www.fortify.com/schema/attachments");
            rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            rootElement.setAttribute("version", "4.4");

            Element projectInfoElement = doc.createElementNS(AUDIT_NAMESPACE_URI, "ns2:ProjectInfo");
            Element projectNameElement = doc.createElementNS(AUDIT_NAMESPACE_URI, "ns2:Name");
            projectNameElement.setTextContent("Unknown Project");
            Element projectVersionIdElement = doc.createElementNS(AUDIT_NAMESPACE_URI, "ns2:ProjectVersionId");
            projectVersionIdElement.setTextContent("-1");
            Element writeDateElement = doc.createElementNS(AUDIT_NAMESPACE_URI, "ns2:WriteDate");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            writeDateElement.setTextContent(dateFormat.format(new Date()));

            projectInfoElement.appendChild(projectNameElement);
            projectInfoElement.appendChild(projectVersionIdElement);
            projectInfoElement.appendChild(writeDateElement);
            rootElement.appendChild(projectInfoElement);

            Element issueListElement = doc.createElementNS(AUDIT_NAMESPACE_URI, "ns2:IssueList");
            rootElement.appendChild(issueListElement);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            try (FileOutputStream fos = new FileOutputStream(auditPath.toFile())) {
                StreamResult result = new StreamResult(fos);
                transformer.transform(source, result);
            }
        } catch (ParserConfigurationException | TransformerException | IOException e) {
            logger.error("Failed to create default audit.xml at {}", auditPath, e);
            throw new AviatorTechnicalException("Failed to create default audit.xml.", e);
        } catch (Exception e) {
            logger.error("Unexpected error creating default audit.xml at {}", auditPath, e);
            throw new AviatorTechnicalException("Unexpected error creating default audit.xml.", e);
        }
    }

    private AuditIssue processAuditIssue(Element issueElement) {
        AuditIssue.AuditIssueBuilder auditIssueBuilder = AuditIssue.builder();

        auditIssueBuilder.instanceId(issueElement.getAttribute("instanceId"));
        auditIssueBuilder.suppressed(Boolean.parseBoolean(issueElement.getAttribute("suppressed")));

        String revisionStr = issueElement.getAttribute("revision");
        auditIssueBuilder.revision(Optional.of(revisionStr).filter(str -> !str.isEmpty()).map(Integer::parseInt).orElse(0));

        Map<String, String> tags = new HashMap<>();
        NodeList tagNodes = issueElement.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Tag");
        for (int j = 0; j < tagNodes.getLength(); j++) {
            Element tagElement = (Element) tagNodes.item(j);
            String tagId = tagElement.getAttribute("id");
            String tagValue = Optional.ofNullable(getTagValue(tagElement)).orElse("");
            tags.put(tagId, tagValue);
        }
        auditIssueBuilder.tags(tags);

        List<AuditIssue.Comment> threadedComments = new ArrayList<>();
        NodeList commentNodes = issueElement.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Comment");
        for (int j = 0; j < commentNodes.getLength(); j++) {
            Element commentElement = (Element) commentNodes.item(j);
            AuditIssue.Comment comment = AuditIssue.Comment.builder()
                    .content(Optional.ofNullable(getFirstElementContentNS(commentElement, "Content")).orElse(""))
                    .username(Optional.ofNullable(getFirstElementContentNS(commentElement, "Username")).orElse(""))
                    .timestamp(Optional.ofNullable(getFirstElementContentNS(commentElement, "Timestamp")).orElse(""))
                    .build();
            threadedComments.add(comment);
        }
        auditIssueBuilder.threadedComments(threadedComments);

        return auditIssueBuilder.build();
    }


    private String getTagValue(Element tagElement) {
        NodeList valueNodes = tagElement.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Value");
        if (valueNodes.getLength() > 0) {
            return valueNodes.item(0).getTextContent();
        }
        return "";
    }

    private String getFirstElementContentNS(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagNameNS(AuditProcessor.AUDIT_NAMESPACE_URI, tagName);
        if (nodes.getLength() > 0 && nodes.item(0) != null) {
            return nodes.item(0).getTextContent();
        }
        return "";
    }

    public void updateIssueTag(AuditIssue auditIssue, String tagId, String tagValue) {
        if (auditIssue == null || tagId == null || tagId.isEmpty() || tagValue == null) {
            logger.error("Invalid input parameters for updateIssueTag.");
            return;
        }

        Element issueElement = findIssueElement(auditIssue.getInstanceId());

        if (issueElement == null) {
            logger.error("Issue element not found for instance ID: {}", auditIssue.getInstanceId());
            return;
        }

        updateOrAddTag(issueElement, tagId, tagValue);
    }

    private Map<String, String> updateAuditXml(Map<String, AuditResponse> auditResponses, TagMappingConfig tagMappingConfig) throws AviatorTechnicalException {
        Map<String, String> remediationCommentTimestamps = new HashMap<>();
        for (Map.Entry<String, AuditResponse> entry : auditResponses.entrySet()) {
            String instanceId = entry.getKey();
            AuditResponse response = entry.getValue();
            Element issueElement = findIssueElement(instanceId);
            String commentTimestamp;

            if (response.getAuditResult() != null) {
                if (issueElement != null) {
                    commentTimestamp = updateIssueElement(issueElement, response, tagMappingConfig);
                } else {
                    commentTimestamp = addNewIssueElement(instanceId, response, tagMappingConfig);
                }
                if (commentTimestamp != null &&
                        response.getAuditResult().getAutoremediation() != null &&
                        response.getAuditResult().getAutoremediation().getChanges() != null &&
                        !response.getAuditResult().getAutoremediation().getChanges().isEmpty()) {
                    remediationCommentTimestamps.put(instanceId, commentTimestamp);
                }
            } else {
                logger.debug("Issue {} skipped or no audit result provided.", response.getIssueId());
            }
        }
        return remediationCommentTimestamps;
    }
    public Element findIssueElement(String instanceId) {
        NodeList issueNodes = auditDoc.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Issue");
        for (int i = 0; i < issueNodes.getLength(); i++) {
            Element issueElement = (Element) issueNodes.item(i);
            if (issueElement.getAttribute("instanceId").equals(instanceId)) {
                return issueElement;
            }
        }
        return null;
    }

    public String updateIssueElement(Element issueElement, AuditResponse response, TagMappingConfig tagMappingConfig) {
        int revision = Integer.parseInt(issueElement.getAttribute("revision"));
        issueElement.setAttribute("revision", String.valueOf(++revision));
        String commentTimestamp = null;

        if (response != null && response.getAuditResult() != null) {
            String tagValue = response.getAuditResult().tagValue;
            String tier = response.getTier();
            TagMappingConfig.Tier tierConfig = tier != null && tier.equalsIgnoreCase("GOLD")
                    ? tagMappingConfig.getMapping().getTier_1()
                    : tagMappingConfig.getMapping().getTier_2();
            TagMappingConfig.Result resultConfig;

            if (Constants.NOT_AN_ISSUE.equalsIgnoreCase(tagValue)) {
                resultConfig = tierConfig.getFp();
                updateOrAddTag(issueElement, Constants.AVIATOR_PREDICTION_TAG_ID,
                        tier != null && tier.equalsIgnoreCase("GOLD") ? Constants.AVIATOR_NOT_AN_ISSUE : Constants.AVIATOR_LIKELY_FP);
            } else if (Constants.EXPLOITABLE.equalsIgnoreCase(tagValue)) {
                resultConfig = tierConfig.getTp();
                updateOrAddTag(issueElement, Constants.AVIATOR_PREDICTION_TAG_ID,
                        tier != null && tier.equalsIgnoreCase("GOLD") ? Constants.AVIATOR_REMEDIATION_REQUIRED : Constants.AVIATOR_LIKELY_TP);
            } else if (Constants.UNSURE.equalsIgnoreCase(tagValue)) {
                resultConfig = tierConfig.getUnsure();
                updateOrAddTag(issueElement, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_UNSURE);
            } else {
                resultConfig = null;
            }

            if (resultConfig != null && resultConfig.getValue() != null && !resultConfig.getValue().isEmpty()) {
                updateOrAddTag(issueElement, tagMappingConfig.getTag_id(), resultConfig.getValue());
            }
            if (resultConfig != null && resultConfig.getSuppress()) {
                issueElement.setAttribute("suppressed", "true");
            }
        }

        updateOrAddTag(issueElement, Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR);

        if (response.getAuditResult() != null) {
            commentTimestamp = updateOrAddComment(issueElement, response.getAuditResult().comment);
        }

        updateClientAuditTrail(issueElement, response, tagMappingConfig);

        return commentTimestamp;
    }

    private void updateClientAuditTrail(Element issueElement, AuditResponse response, TagMappingConfig tagMappingConfig) {
        Element clientAuditTrail = getClientAuditTrailElement(issueElement);

        if (response != null && response.getAuditResult() != null) {
            String tagValue = response.getAuditResult().tagValue;
            String tier = response.getTier();
            TagMappingConfig.Tier tierConfig = tier != null && tier.equalsIgnoreCase("GOLD")
                    ? tagMappingConfig.getMapping().getTier_1()
                    : tagMappingConfig.getMapping().getTier_2();
            TagMappingConfig.Result resultConfig;

            if (Constants.NOT_AN_ISSUE.equalsIgnoreCase(tagValue)) {
                resultConfig = tierConfig.getFp();
                addTagHistory(clientAuditTrail, Constants.AVIATOR_PREDICTION_TAG_ID,
                        tier != null && tier.equalsIgnoreCase("GOLD") ? Constants.AVIATOR_NOT_AN_ISSUE : Constants.AVIATOR_LIKELY_FP);
            } else if (Constants.EXPLOITABLE.equalsIgnoreCase(tagValue)) {
                resultConfig = tierConfig.getTp();
                addTagHistory(clientAuditTrail, Constants.AVIATOR_PREDICTION_TAG_ID,
                        tier != null && tier.equalsIgnoreCase("GOLD") ? Constants.AVIATOR_REMEDIATION_REQUIRED : Constants.AVIATOR_LIKELY_TP);
            } else if (Constants.UNSURE.equalsIgnoreCase(tagValue)) {
                resultConfig = tierConfig.getUnsure();
                addTagHistory(clientAuditTrail, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_UNSURE);
            } else {
                resultConfig = null;
            }

            if (resultConfig != null && resultConfig.getValue() != null && !resultConfig.getValue().isEmpty()) {
                addTagHistory(clientAuditTrail, tagMappingConfig.getTag_id(), resultConfig.getValue());
            }
            if (resultConfig != null && resultConfig.getSuppress()) {
                issueElement.setAttribute("suppressed", "true");
            }
        }
        addTagHistory(clientAuditTrail, Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR);
    }

    private Element getClientAuditTrailElement(Element issueElement) {
        NodeList clientAuditTrailNodes = issueElement.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "ClientAuditTrail");
        Element clientAuditTrail;
        if (clientAuditTrailNodes.getLength() > 0) {
            clientAuditTrail = (Element) clientAuditTrailNodes.item(0);
        } else {
            clientAuditTrail = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "ClientAuditTrail");
            issueElement.appendChild(clientAuditTrail);
        }
        return clientAuditTrail;
    }

    private void addTagHistory(Element clientAuditTrail, String tagId, String tagValue) {
        Element tagHistory = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "TagHistory");

        Element tag = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Tag");
        tag.setAttribute("id", tagId);
        Element value = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Value");
        value.setTextContent(tagValue);
        tag.appendChild(value);
        tagHistory.appendChild(tag);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        Element editTime = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "EditTime");
        editTime.setTextContent(dateFormat.format(new Date()));
        tagHistory.appendChild(editTime);

        Element username = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Username");
        username.setTextContent("Fortify Aviator");
        tagHistory.appendChild(username);

        clientAuditTrail.appendChild(tagHistory);
    }

    private void updateOrAddTag(Element issueElement, String tagId, String tagValue) {
        NodeList tagNodes = issueElement.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Tag");
        Element tagElement = null;

        for (int i = 0; i < tagNodes.getLength(); i++) {
            Element currentTag = (Element) tagNodes.item(i);
            if (currentTag.getAttribute("id").equalsIgnoreCase(tagId)) {
                tagElement = currentTag;
                break;
            }
        }

        if (tagElement == null) {
            tagElement = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Tag");
            tagElement.setAttribute("id", tagId);
            issueElement.appendChild(tagElement);
        }

        NodeList valueNodes = tagElement.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Value");
        Element valueElement;
        if (valueNodes.getLength() > 0) {
            valueElement = (Element) valueNodes.item(0);
        } else {
            valueElement = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Value");
            tagElement.appendChild(valueElement);
        }

        valueElement.setTextContent(tagValue);
    }

    private String updateOrAddComment(Element issueElement, String commentText) {
        NodeList threadedCommentsNodes = issueElement.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "ThreadedComments");
        Element threadedCommentsElement;

        if (threadedCommentsNodes.getLength() > 0) {
            threadedCommentsElement = (Element) threadedCommentsNodes.item(0);
        } else {
            threadedCommentsElement = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "ThreadedComments");
            issueElement.appendChild(threadedCommentsElement);
        }

        Element commentElement = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Comment");

        Element contentElement = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Content");
        contentElement.setTextContent(commentText);
        commentElement.appendChild(contentElement);

        Element usernameElement = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Username");
        usernameElement.setTextContent("Fortify Aviator");
        commentElement.appendChild(usernameElement);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        String timestamp = dateFormat.format(new Date());
        Element timestampElement = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Timestamp");
        timestampElement.setTextContent(timestamp);
        commentElement.appendChild(timestampElement);

        threadedCommentsElement.appendChild(commentElement);
        return timestamp;
    }

    public String addNewIssueElement(String instanceId, AuditResponse response, TagMappingConfig tagMappingConfig) {
        Element issueList = (Element) auditDoc.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "IssueList").item(0);
        if (issueList == null) {
            issueList = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "IssueList");
            auditDoc.getDocumentElement().appendChild(issueList);
        }

        Element newIssue = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Issue");
        newIssue.setAttribute("instanceId", instanceId);
        newIssue.setAttribute("revision", "0");
        String commentTimestamp = null;

        if (response != null && response.getAuditResult() != null) {
            String tagValue = response.getAuditResult().tagValue;
            String tier = response.getTier();
            TagMappingConfig.Tier tierConfig = tier != null && tier.equalsIgnoreCase("GOLD")
                    ? tagMappingConfig.getMapping().getTier_1()
                    : tagMappingConfig.getMapping().getTier_2();
            TagMappingConfig.Result resultConfig;

            if (Constants.NOT_AN_ISSUE.equalsIgnoreCase(tagValue)) {
                resultConfig = tierConfig.getFp();
                updateOrAddTag(newIssue, Constants.AVIATOR_PREDICTION_TAG_ID,
                        tier != null && tier.equalsIgnoreCase("GOLD") ? Constants.AVIATOR_NOT_AN_ISSUE : Constants.AVIATOR_LIKELY_FP);
            } else if (Constants.EXPLOITABLE.equalsIgnoreCase(tagValue)) {
                resultConfig = tierConfig.getTp();
                updateOrAddTag(newIssue, Constants.AVIATOR_PREDICTION_TAG_ID,
                        tier != null && tier.equalsIgnoreCase("GOLD") ? Constants.AVIATOR_REMEDIATION_REQUIRED : Constants.AVIATOR_LIKELY_TP);
            } else if (Constants.UNSURE.equalsIgnoreCase(tagValue)) {
                resultConfig = tierConfig.getUnsure();
                updateOrAddTag(newIssue, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_UNSURE);
            } else {
                resultConfig = null;
            }

            if (resultConfig != null && resultConfig.getValue() != null && !resultConfig.getValue().isEmpty()) {
                updateOrAddTag(newIssue, tagMappingConfig.getTag_id(), resultConfig.getValue());
            }
            if (resultConfig != null && resultConfig.getSuppress()) {
                newIssue.setAttribute("suppressed", "true");
            }
        }

        updateOrAddTag(newIssue, Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR);

        if (response != null && response.getAuditResult() != null) {
            commentTimestamp = updateOrAddComment(newIssue, response.getAuditResult().comment);
        }

        updateClientAuditTrail(newIssue, response, tagMappingConfig);

        issueList.appendChild(newIssue);
        return commentTimestamp;
    }

    public void addCommentToIssueXml(String instanceId, String commentText, String username) {
        if (auditDoc == null) {
            logger.error("Cannot add comment, auditDoc is not initialized.");
            return;
        }
        Element issueElement = findIssueElement(instanceId);
        if (issueElement != null) {
            addCommentToIssueElement(issueElement, commentText, username);
            logger.debug("Added comment via XML update for issue: {}", instanceId);
        } else {
            logger.warn("Cannot add comment to XML, issue element not found for instance ID: {}. If this is a skipped new issue, addSkippedIssueElement should be used.", instanceId);
        }
    }

    public void addSkippedIssueElement(String instanceId, String comment) {
        if (auditDoc == null) {
            logger.error("Cannot add skipped issue element, auditDoc is not initialized.");
            return;
        }
        if (findIssueElement(instanceId) != null) {
            logger.warn("Attempted to add skipped issue element for {}, but it already exists in audit.xml.", instanceId);
            addCommentToIssueXml(instanceId, comment, Constants.USER_NAME);
            return;
        }

        Element issueList = (Element) auditDoc.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "IssueList").item(0);
        if (issueList == null) {
            logger.error("Cannot add skipped issue element, <IssueList> not found in audit.xml.");
            issueList = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "IssueList");
            if (auditDoc.getDocumentElement() != null) {
                auditDoc.getDocumentElement().appendChild(issueList);
                logger.warn("Created missing <IssueList> element.");
            } else {
                logger.error("Cannot add skipped issue element, document root is null.");
                return;
            }
        }

        Element newIssue = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Issue");
        newIssue.setAttribute("instanceId", instanceId);
        newIssue.setAttribute("revision", "0");
        newIssue.setAttribute("suppressed", "false");

        updateOrAddTag(newIssue, Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR);
        updateOrAddTag(newIssue, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_EXCLUDED);

        addCommentToIssueElement(newIssue, comment, Constants.USER_NAME);

        issueList.appendChild(newIssue);
        logger.debug("Added skipped issue element to audit.xml for instance ID: {}", instanceId);

        if (!auditIssueMap.containsKey(instanceId)) {
            AuditIssue skippedAuditIssue = AuditIssue.builder()
                    .instanceId(instanceId)
                    .revision(0)
                    .suppressed(false)
                    .tags(Map.of(
                            Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR,
                            Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_EXCLUDED
                    ))
                    .threadedComments(List.of(
                            AuditIssue.Comment.builder()
                                    .content(comment)
                                    .username(Constants.USER_NAME)
                                    .timestamp(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()))
                                    .build()
                    ))
                    .build();
            auditIssueMap.put(instanceId, skippedAuditIssue);
            logger.debug("Added skipped issue {} to in-memory auditIssueMap.", instanceId);
        }
    }

    private String addCommentToIssueElement(Element issueElement, String commentText, String username) {
        NodeList threadedCommentsNodes = issueElement.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "ThreadedComments");
        Element threadedCommentsElement;

        if (threadedCommentsNodes.getLength() > 0) {
            threadedCommentsElement = (Element) threadedCommentsNodes.item(0);
        } else {
            threadedCommentsElement = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "ThreadedComments");
            issueElement.appendChild(threadedCommentsElement);
        }

        Element commentElement = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Comment");

        Element contentElement = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Content");
        contentElement.setTextContent(commentText != null ? commentText : "");
        commentElement.appendChild(contentElement);

        Element usernameElement = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Username");
        usernameElement.setTextContent(username != null ? username : "Unknown User");
        commentElement.appendChild(usernameElement);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        String timestamp = "";
        try {
            timestamp = dateFormat.format(new Date());
        } catch (Exception e) {
            logger.warn("Could not format timestamp for comment: {}", e.getMessage());
        }
        Element timestampElement = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "Timestamp");
        timestampElement.setTextContent(timestamp);
        commentElement.appendChild(timestampElement);

        threadedCommentsElement.appendChild(commentElement);
        return timestamp;
    }

    public File updateAndSaveAuditAndRemediationsXml(Map<String, AuditResponse> auditResponses,
                                                     TagMappingConfig tagMappingConfig,
                                                     FPRInfo fprInfo,
                                                     FVDLProcessor fvdlProcessor) throws AviatorTechnicalException {
        Map<String, String> remediationCommentTimestamps = updateAuditXml(auditResponses, tagMappingConfig);

        boolean hasRemediations = auditResponses.values().stream()
                .anyMatch(ar -> ar.getAuditResult() != null &&
                        ar.getAuditResult().getAutoremediation() != null &&
                        ar.getAuditResult().getAutoremediation().getChanges() != null &&
                        !ar.getAuditResult().getAutoremediation().getChanges().isEmpty());

        if (hasRemediations && !remediationCommentTimestamps.isEmpty()) {
            this.remediationsDoc = generateRemediationsXml(auditResponses, remediationCommentTimestamps, fprInfo, fvdlProcessor);
        } else {
            this.remediationsDoc = null;
            if (hasRemediations) {
                logger.warn("Remediation data found, but could not associate timestamps for all. remediations.xml will not be generated.");
            }
        }

        return updateContentInOriginalFpr();
    }

    private Document generateRemediationsXml(Map<String, AuditResponse> auditResponses,
                                             Map<String, String> remediationCommentTimestamps,
                                             FPRInfo fprInfo, FVDLProcessor fvdlProcessor) throws AviatorTechnicalException {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "Remediations");
            doc.appendChild(rootElement);

            // ProjectInfo
            Element projectInfoElement = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "ProjectInfo");
            Element projectNameElement = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "Name");
            projectNameElement.setTextContent(fprInfo.getBuildId() != null ? fprInfo.getBuildId() : "UnknownProject");
            Element projectWriteDateElement = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "WriteDate");
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            projectWriteDateElement.setTextContent(dateTimeFormat.format(new Date()));
            projectInfoElement.appendChild(projectNameElement);
            projectInfoElement.appendChild(projectWriteDateElement);
            rootElement.appendChild(projectInfoElement);

            // RemediationList
            Element remediationListElement = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "RemediationList");
            rootElement.appendChild(remediationListElement);

            for (Map.Entry<String, AuditResponse> entry : auditResponses.entrySet()) {
                String instanceId = entry.getKey();
                AuditResponse auditResponse = entry.getValue();

                if (auditResponse.getAuditResult() != null &&
                        auditResponse.getAuditResult().getAutoremediation() != null &&
                        auditResponse.getAuditResult().getAutoremediation().getChanges() != null &&
                        !auditResponse.getAuditResult().getAutoremediation().getChanges().isEmpty() &&
                        remediationCommentTimestamps.containsKey(instanceId)) {

                    Element remediationElement = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "Remediation");
                    remediationElement.setAttribute("instanceId", instanceId);
                    remediationElement.setAttribute("writeDate", remediationCommentTimestamps.get(instanceId));

                    Element auditCommentElement = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "AuditComment");
                    String auditComment = auditResponse.getAuditResult().getComment() != null ? auditResponse.getAuditResult().getComment() : "";
                    auditCommentElement.appendChild(doc.createCDATASection(auditComment));
                    remediationElement.appendChild(auditCommentElement);

                    Map<String, List<com.fortify.cli.aviator.audit.model.Change>> changesByFile =
                            auditResponse.getAuditResult().getAutoremediation().getChanges().stream()
                                    .collect(Collectors.groupingBy(com.fortify.cli.aviator.audit.model.Change::getFile));

                    for (Map.Entry<String, List<com.fortify.cli.aviator.audit.model.Change>> fileChangeEntry : changesByFile.entrySet()) {
                        String filename = fileChangeEntry.getKey();
                        List<com.fortify.cli.aviator.audit.model.Change> fileSpecificChanges = fileChangeEntry.getValue();

                        Element fileChangesElement = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "FileChanges");

                        Element filenameElement = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "Filename");
                        filenameElement.setTextContent(filename);
                        fileChangesElement.appendChild(filenameElement);

                        String originalFileContent = fvdlProcessor.getSourceFileContent(filename)
                                .orElseThrow(() -> new AviatorTechnicalException("Could not get original content for file: " + filename + " for MD5 calculation."));
                        Element fileMD5Element = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "FileMD5");
                        fileMD5Element.setTextContent(calculateMD5Base64(originalFileContent));
                        fileChangesElement.appendChild(fileMD5Element);

                        for (com.fortify.cli.aviator.audit.model.Change change : fileSpecificChanges) {
                            Element changeElement = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "Change");

                            Element lineFromElement = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "LineFrom");
                            lineFromElement.setTextContent(String.valueOf(parseLineNumber(change.getFromLine(), filename, instanceId, "FromLine")));
                            changeElement.appendChild(lineFromElement);

                            Element lineToElement = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "LineTo");
                            lineToElement.setTextContent(String.valueOf(parseLineNumber(change.getToLine(), filename, instanceId, "ToLine")));
                            changeElement.appendChild(lineToElement);

                            Element originalCodeElement = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "OriginalCode");
                            int lineFromNum = parseLineNumber(change.getFromLine(), filename, instanceId, "FromLine (for OriginalCode)");
                            int lineToNum = parseLineNumber(change.getToLine(), filename, instanceId, "ToLine (for OriginalCode)");
                            String[] allLines = originalFileContent.split("\\r?\\n|\\n|\\r");
                            StringBuilder originalCodeSb = new StringBuilder();
                            if (lineFromNum >= 1 && lineToNum >= lineFromNum && lineFromNum <= allLines.length) {
                                for (int k = lineFromNum - 1; k < Math.min(lineToNum, allLines.length); k++) {
                                    originalCodeSb.append(allLines[k]);
                                    if (k < Math.min(lineToNum, allLines.length) - 1) {
                                        originalCodeSb.append(System.lineSeparator());
                                    }
                                }
                            } else if (lineFromNum == 0 && lineToNum == 0) {
                                // Insertion at top, no original code.
                            } else {
                                logger.warn("Invalid line numbers for original code extraction: file='{}', instanceId='{}', from={}, to={}. Max lines: {}. Original FromLine: '{}', Original ToLine: '{}'",
                                        filename, instanceId, lineFromNum, lineToNum, allLines.length, change.getFromLine(), change.getToLine());
                            }
                            originalCodeElement.appendChild(doc.createCDATASection(originalCodeSb.toString()));
                            changeElement.appendChild(originalCodeElement);


                            Element newCodeElement = doc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "NewCode");
                            String newCode = change.getReplaceWith() != null ? change.getReplaceWith() : "";
                            // *** CHANGE: Use CDATA Section for NewCode ***
                            newCodeElement.appendChild(doc.createCDATASection(newCode));
                            changeElement.appendChild(newCodeElement);

                            fileChangesElement.appendChild(changeElement);
                        }
                        remediationElement.appendChild(fileChangesElement);
                    }
                    remediationListElement.appendChild(remediationElement);
                }
            }
            return doc;
        } catch (ParserConfigurationException e) {
            throw new AviatorTechnicalException("Error creating XML document for remediations", e);
        } catch (NumberFormatException e) {
            throw new AviatorTechnicalException("Error processing remediations: " + e.getMessage(), e);
        }
    }

    private static String calculateMD5Base64(String content) {
        if (content == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    private int parseLineNumber(String lineStr, String filePath, String instanceId, String changeType) {
        if (lineStr == null || lineStr.trim().isEmpty()) {
            logger.warn("Line number string is null or empty for file '{}', instanceId '{}', changeType '{}'. Defaulting to 0.", filePath, instanceId, changeType);
            return 0;
        }
        // Remove any commas that might be present
        String cleanedLineStr = lineStr.replace(",", "");
        try {
            return Integer.parseInt(cleanedLineStr);
        } catch (NumberFormatException e) {
            // Enhanced logging to include context
            logger.error("Error parsing {} line number string: '{}' (original: '{}') for file '{}', instanceId '{}'.",
                    changeType, cleanedLineStr, lineStr, filePath, instanceId, e);
            throw e; // Re-throw to be caught by the calling method's try-catch
        }
    }

    private File updateContentInOriginalFpr() throws AviatorTechnicalException {
        String originalFprPath = fprFilePath;
        String tempFprPath = originalFprPath + ".tmp";
        Path tempPath = Paths.get(tempFprPath);

        logger.debug("Starting update of FPR file: {}", originalFprPath);

        try (ZipFile zipFile = new ZipFile(originalFprPath)) {
            // This block is just for a quick check that the file is a valid zip
        } catch (IOException e) {
            logger.error("Input FPR file is invalid or cannot be read: {}", originalFprPath, e);
            throw new AviatorTechnicalException("Invalid or unreadable input FPR file.", e);
        }

        try {
            Files.copy(Paths.get(originalFprPath), tempPath, StandardCopyOption.REPLACE_EXISTING);

            try (ZipFile zipFile = new ZipFile(tempPath.toFile());
                 FileOutputStream fos = new FileOutputStream(originalFprPath);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                AtomicBoolean auditXmlExists = new AtomicBoolean(false);
                AtomicBoolean filterTemplateXmlExists = new AtomicBoolean(false);
                AtomicBoolean remediationsXmlExists = new AtomicBoolean(false);

                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    try {
                        if (entryName.equals("audit.xml")) {
                            auditXmlExists.set(true);
                            if (auditDoc != null) {
                                zos.putNextEntry(new ZipEntry(entryName));
                                transformDomToStream(auditDoc, zos);
                                zos.closeEntry();
                            } else {
                                logger.warn("auditDoc is null, copying original audit.xml");
                                copyEntryContents(zipFile, entry, zos);
                            }
                        } else if (entryName.equals("filtertemplate.xml")) {
                            filterTemplateXmlExists.set(true);
                            if (filterTemplateDoc != null) {
                                zos.putNextEntry(new ZipEntry(entryName));
                                transformDomToStream(filterTemplateDoc, zos);
                                zos.closeEntry();
                            } else {
                                copyEntryContents(zipFile, entry, zos);
                            }
                        } else if (entryName.equals("remediations.xml")) {
                            remediationsXmlExists.set(true);
                            if (remediationsDoc != null) {
                                zos.putNextEntry(new ZipEntry(entryName));
                                transformDomToStream(remediationsDoc, zos);
                                zos.closeEntry();
                            } else {
                                logger.debug("remediationsDoc is null, remediations.xml will not be included.");
                            }
                        } else {
                            copyEntryContents(zipFile, entry, zos);
                        }
                    } catch (TransformerException | IOException e) {
                        logger.error("Error processing zip entry: {}", entryName, e);
                        throw new AviatorTechnicalException("Error processing zip entry: " + entryName, e);
                    }
                }

                if (auditDoc != null && !auditXmlExists.get()) {
                    logger.debug("Adding new audit.xml file to FPR.");
                    zos.putNextEntry(new ZipEntry("audit.xml"));
                    transformDomToStream(auditDoc, zos);
                    zos.closeEntry();
                }

                if (filterTemplateDoc != null && !filterTemplateXmlExists.get()) {
                    logger.debug("Adding new filtertemplate.xml file to FPR.");
                    zos.putNextEntry(new ZipEntry("filtertemplate.xml"));
                    transformDomToStream(filterTemplateDoc, zos);
                    zos.closeEntry();
                }

                if (remediationsDoc != null && !remediationsXmlExists.get()) {
                    logger.debug("Adding new remediations.xml file to FPR.");
                    zos.putNextEntry(new ZipEntry("remediations.xml"));
                    transformDomToStream(remediationsDoc, zos);
                    zos.closeEntry();
                }

                zos.finish();
                logger.info("Successfully updated FPR file: {}", originalFprPath);
            }

        } catch (IOException | TransformerException e) {
            logger.error("Error updating content in original FPR, attempting to restore from backup.", e);
            try {
                Path path = Paths.get(originalFprPath);
                Files.deleteIfExists(path); // Delete the partially written/corrupted original
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Restored original FPR from backup: {}", originalFprPath);
            } catch (IOException restoreEx) {
                logger.error("FATAL: Failed to restore original FPR from backup at {}: {}", tempFprPath, restoreEx.getMessage());
                e.addSuppressed(restoreEx);
            }
            throw new AviatorTechnicalException("Error updating FPR content, rollback may have been required.", e);
        } finally {
            try {
                Files.deleteIfExists(tempPath);
                logger.debug("Deleted temporary FPR file: {}", tempFprPath);
            } catch (IOException e) {
                logger.warn("Failed to delete temporary FPR file: {}", tempFprPath, e);
            }
        }
        return new File(originalFprPath);
    }

    private void copyEntryContents(ZipFile sourceZipFile, ZipEntry sourceEntry, ZipOutputStream targetZos) throws IOException {
        ZipEntry newEntry = new ZipEntry(sourceEntry.getName());
        // Preserve metadata for STORED entries, which is crucial for some zip tools
        if (sourceEntry.getMethod() == ZipEntry.STORED) {
            newEntry.setMethod(ZipEntry.STORED);
            newEntry.setSize(sourceEntry.getSize());
            newEntry.setCompressedSize(sourceEntry.getCompressedSize());
            newEntry.setCrc(sourceEntry.getCrc());
        }

        targetZos.putNextEntry(newEntry);

        if (!sourceEntry.isDirectory()) {
            try (InputStream is = sourceZipFile.getInputStream(sourceEntry)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    targetZos.write(buffer, 0, len);
                }
            } catch (EOFException | ZipException e) {
                logger.warn("Content of zip entry '{}' appears corrupted ({}). An empty placeholder will be written.",
                        sourceEntry.getName(), e.getMessage());
            }
        }
        targetZos.closeEntry();
    }

    private void transformDomToStream(Document doc, ZipOutputStream zos) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (TransformerConfigurationException e) {
            logger.warn("Security feature {} not supported by TransformerFactory.", javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, e);
        }
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(zos);
        transformer.transform(source, result);
    }
}