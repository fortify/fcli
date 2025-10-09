package com.fortify.cli.aviator.fpr.processor;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
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
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.util.FprHandle;
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


public class AuditProcessor {

    Logger logger = LoggerFactory.getLogger(AuditProcessor.class);
    private static final String AUDIT_NAMESPACE_URI = "xmlns://www.fortify.com/schema/audit";
    private static final String REMEDIATIONS_NAMESPACE_URI = "xmlns://www.fortify.com/schema/remediations";

    private static final String REMEDIATIONS_XSD_PATH = "/remediations.xsd";
    private static volatile Schema remediationsSchema;
    private static final String HASHING_ALGORITHM_SHA_256 = "SHA-256";

    private Document auditDoc;
    @Setter
    private Document filterTemplateDoc;
    private Document remediationsDoc;

    private final Map<String, AuditIssue> auditIssueMap = new HashMap<>();
    private final FprHandle fprHandle;

    public AuditProcessor(FprHandle fprHandle) {
        this.fprHandle = fprHandle;
    }

    public Map<String, AuditIssue> processAuditXML() throws AviatorTechnicalException {
        Path auditPath = fprHandle.getPath("/audit.xml");

        try {
            if (!Files.exists(auditPath)) {
                logger.debug("audit.xml not found. Creating a default audit.xml.");
                auditDoc = createDefaultAuditXml();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            try (InputStream auditStream = Files.newInputStream(auditPath)) {
                auditDoc = builder.parse(auditStream);
            }
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

    private Document createDefaultAuditXml() throws AviatorTechnicalException {
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

            return doc;

        } catch (ParserConfigurationException e) {
            throw new AviatorTechnicalException("Failed to create default audit.xml document.", e);
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

            boolean isSilentlySkipped = "SKIPPED".equalsIgnoreCase(response.getStatus()) &&
                    (response.getAuditResult() == null ||
                            response.getAuditResult().getComment() == null ||
                            response.getAuditResult().getComment().trim().isEmpty());

            if (isSilentlySkipped) {
                logger.debug("Issue {} was skipped by Aviator. No changes will be made to audit.xml for this issue.", instanceId);
                continue;
            }

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
            logger.warn("WARN: Cannot add comment to XML, issue element not found for instance ID: {}. If this is a skipped new issue, addSkippedIssueElement should be used.", instanceId);
        }
    }

    public void addSkippedIssueElement(String instanceId, String comment) {
        if (auditDoc == null) {
            logger.error("Cannot add skipped issue element, auditDoc is not initialized.");
            return;
        }
        if (findIssueElement(instanceId) != null) {
            logger.warn("WARN: Attempted to add skipped issue element for {}, but it already exists in audit.xml.", instanceId);
            addCommentToIssueXml(instanceId, comment, Constants.USER_NAME);
            return;
        }

        Element issueList = (Element) auditDoc.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "IssueList").item(0);
        if (issueList == null) {
            logger.error("Cannot add skipped issue element, <IssueList> not found in audit.xml.");
            issueList = auditDoc.createElementNS(AUDIT_NAMESPACE_URI, "IssueList");
            if (auditDoc.getDocumentElement() != null) {
                auditDoc.getDocumentElement().appendChild(issueList);
                logger.warn("WARN: Created missing <IssueList> element.");
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
            logger.warn("WARN: Could not format timestamp for comment: {}", e.getMessage());
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
        // Step 1: Update the in-memory audit.xml document. This returns timestamps needed for remediations.
        Map<String, String> remediationCommentTimestamps = updateAuditXml(auditResponses, tagMappingConfig);

        // Step 2: Check if there are any remediations to generate.
        boolean hasRemediations = auditResponses.values().stream()
                .anyMatch(ar -> ar.getAuditResult() != null &&
                        ar.getAuditResult().getAutoremediation() != null &&
                        ar.getAuditResult().getAutoremediation().getChanges() != null &&
                        !ar.getAuditResult().getAutoremediation().getChanges().isEmpty());

        // Step 3: Generate the in-memory remediations.xml document if needed.
        if (hasRemediations && !remediationCommentTimestamps.isEmpty()) {
            this.remediationsDoc = generateRemediationsXml(auditResponses, remediationCommentTimestamps, fprInfo, fvdlProcessor);
        } else {
            this.remediationsDoc = null;
            if (hasRemediations) {
                logger.info("WARN: Remediation data found, but could not associate timestamps for all. remediations.xml will not be generated.");
            }
        }

        // Step 4: Write the modified XML documents directly back into the open FPR file system.
        try {
            if (auditDoc != null) {
                // Get the path to 'audit.xml' *inside* the zip and overwrite it.
                try (OutputStream os = Files.newOutputStream(fprHandle.getPath("/audit.xml"))) {
                    transformDomToStream(auditDoc, os);
                }
            }
            if (remediationsDoc != null) {
                // Get the path to 'remediations.xml' *inside* the zip and create/overwrite it.
                try (OutputStream os = Files.newOutputStream(fprHandle.getPath("/remediations.xml"))) {
                    transformDomToStream(remediationsDoc, os);
                }
            }
            if (filterTemplateDoc != null) {
                // Get the path to 'filtertemplate.xml' *inside* the zip and overwrite it.
                try (OutputStream os = Files.newOutputStream(fprHandle.getPath("/filtertemplate.xml"))) {
                    transformDomToStream(filterTemplateDoc, os);
                }
            }
        } catch (Exception e) {
            throw new AviatorTechnicalException("Failed to write updated audit/remediation data back into the FPR file.", e);
        }

        return fprHandle.getFprPath().toFile();
    }

    private Document generateRemediationsXml(Map<String, AuditResponse> auditResponses,
                                             Map<String, String> remediationCommentTimestamps,
                                             FPRInfo fprInfo,
                                             FVDLProcessor fvdlProcessor) throws AviatorTechnicalException {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document finalDoc = docBuilder.newDocument();
            Element rootElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "Remediations");
            finalDoc.appendChild(rootElement);

            Element projectInfoElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "ProjectInfo");
            Element projectNameElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "Name");
            projectNameElement.setTextContent(fprInfo.getBuildId() != null ? fprInfo.getBuildId() : "UnknownProject");
            Element projectWriteDateElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "WriteDate");
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            projectWriteDateElement.setTextContent(dateTimeFormat.format(new Date()));
            projectInfoElement.appendChild(projectNameElement);
            projectInfoElement.appendChild(projectWriteDateElement);
            rootElement.appendChild(projectInfoElement);

            Element remediationListElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "RemediationList");
            rootElement.appendChild(remediationListElement);

            int validRemediationCount = 0;

            for (Map.Entry<String, AuditResponse> entry : auditResponses.entrySet()) {
                String instanceId = entry.getKey();
                AuditResponse auditResponse = entry.getValue();

                if (auditResponse.getAuditResult() != null &&
                        auditResponse.getAuditResult().getAutoremediation() != null &&
                        auditResponse.getAuditResult().getAutoremediation().getChanges() != null &&
                        !auditResponse.getAuditResult().getAutoremediation().getChanges().isEmpty() &&
                        remediationCommentTimestamps.containsKey(instanceId)) {

                    Element remediationElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "Remediation");
                    remediationElement.setAttribute("instanceId", instanceId);
                    remediationElement.setAttribute("writeDate", remediationCommentTimestamps.get(instanceId));

                    Element auditCommentElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "AuditComment");
                    String auditComment = auditResponse.getAuditResult().getComment() != null ? auditResponse.getAuditResult().getComment() : "";
                    auditCommentElement.appendChild(finalDoc.createCDATASection(auditComment));
                    remediationElement.appendChild(auditCommentElement);

                    Map<String, List<com.fortify.cli.aviator.audit.model.Change>> changesByFile =
                            auditResponse.getAuditResult().getAutoremediation().getChanges().stream()
                                    .collect(Collectors.groupingBy(com.fortify.cli.aviator.audit.model.Change::getFile));

                    for (Map.Entry<String, List<com.fortify.cli.aviator.audit.model.Change>> fileChangeEntry : changesByFile.entrySet()) {
                        String filename = fileChangeEntry.getKey();
                        List<com.fortify.cli.aviator.audit.model.Change> fileSpecificChanges = fileChangeEntry.getValue();

                        Element fileChangesElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "FileChanges");
                        Element filenameElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "Filename");
                        filenameElement.setTextContent(filename);
                        fileChangesElement.appendChild(filenameElement);

                        String originalFileContent = "";
                        try {
                            originalFileContent = String.valueOf(fvdlProcessor.getSourceFileContent(filename));
                        } catch (RuntimeException ex){
                            logger.error("Could not get source for autoremediation {}", ex.getMessage());
                            continue;
                        }

                        Element hashElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "Hash");
                        hashElement.setAttribute("type", HASHING_ALGORITHM_SHA_256);
                        hashElement.setTextContent(calculateHashBase64(originalFileContent, HASHING_ALGORITHM_SHA_256));
                        fileChangesElement.appendChild(hashElement);

                        String[] allLines = originalFileContent.split("\\r?\\n|\\r|\\n");

                        for (com.fortify.cli.aviator.audit.model.Change change : fileSpecificChanges) {
                            try {
                                Element changeElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "Change");
                                int lineFromNum = parseLineNumber(change.getFromLine(), filename, instanceId, "FromLine");
                                int lineToNum = parseLineNumber(change.getToLine(), filename, instanceId, "ToLine");

                                Element lineFromElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "LineFrom");
                                lineFromElement.setTextContent(String.valueOf(lineFromNum));
                                changeElement.appendChild(lineFromElement);

                                Element lineToElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "LineTo");
                                lineToElement.setTextContent(String.valueOf(lineToNum));
                                changeElement.appendChild(lineToElement);

                                StringBuilder originalCodeSb = new StringBuilder();
                                if (lineFromNum >= 1 && lineToNum >= lineFromNum && lineFromNum <= allLines.length) {
                                    for (int k = lineFromNum - 1; k < Math.min(lineToNum, allLines.length); k++) {
                                        originalCodeSb.append(allLines[k]);
                                        if (k < Math.min(lineToNum, allLines.length) - 1) {
                                            originalCodeSb.append('\n');
                                        }
                                    }
                                }
                                Element originalCodeElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "OriginalCode");
                                originalCodeElement.appendChild(finalDoc.createCDATASection(originalCodeSb.toString()));
                                changeElement.appendChild(originalCodeElement);

                                Element newCodeElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "NewCode");
                                newCodeElement.appendChild(finalDoc.createCDATASection(change.getReplaceWith() != null ? change.getReplaceWith() : ""));
                                changeElement.appendChild(newCodeElement);

                                final int CONTEXT_LINES = 3;
                                int contextStartLineIndex = Math.max(0, lineFromNum - 1 - CONTEXT_LINES);
                                if (lineFromNum == 0) contextStartLineIndex = 0;
                                int contextEndLineIndex = Math.min(allLines.length - 1, lineToNum - 1 + CONTEXT_LINES);
                                if (lineToNum == 0) contextEndLineIndex = -1;
                                int actualBeforeLines = (lineFromNum > 0) ? (lineFromNum - 1 - contextStartLineIndex) : 0;
                                int actualAfterLines = (lineToNum > 0) ? (contextEndLineIndex - (lineToNum - 1)) : 0;

                                StringBuilder contextSb = new StringBuilder();
                                for (int i = contextStartLineIndex; i <= contextEndLineIndex; i++) {
                                    contextSb.append(allLines[i]);
                                    if (i < contextEndLineIndex) {
                                        contextSb.append('\n');
                                    }
                                }
                                Element contextElement = finalDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "Context");
                                contextElement.setAttribute("before", String.valueOf(actualBeforeLines));
                                contextElement.setAttribute("after", String.valueOf(actualAfterLines));
                                contextElement.appendChild(finalDoc.createCDATASection(contextSb.toString()));
                                changeElement.appendChild(contextElement);

                                fileChangesElement.appendChild(changeElement);
                            } catch (NumberFormatException e) {
                                logger.error("Skipping change for issue {} due to invalid line number format. Details: {}", instanceId, e.getMessage());
                            }
                        }
                        if (fileChangesElement.getElementsByTagNameNS(REMEDIATIONS_NAMESPACE_URI, "Change").getLength() > 0) {
                            remediationElement.appendChild(fileChangesElement);
                        }
                    }

                    // Check if any FileChanges were actually added to the remediation element.
                    // This prevents validation errors if all changes for an issue were skipped.
                    if (remediationElement.getElementsByTagNameNS(REMEDIATIONS_NAMESPACE_URI, "FileChanges").getLength() > 0) {
                        if (isRemediationElementValid(remediationElement, fprInfo)) {
                            remediationListElement.appendChild(remediationElement);
                            validRemediationCount++;
                        } else {
                            logger.warn("WARN: Skipping structurally invalid remediation for issue instanceId: {}", instanceId);
                        }
                    } else {
                        logger.warn("WARN: Skipping remediation for instanceId '{}' because all of its proposed changes were invalid and could not be processed.", instanceId);
                    }

                }
            }

            return validRemediationCount > 0 ? finalDoc : null;
        } catch (ParserConfigurationException e) {
            throw new AviatorTechnicalException("Error creating XML document for remediations", e);
        }
    }

    private String calculateHashBase64(String content, String algorithm) {
        if (content == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(algorithm + " algorithm not found", e);
        }
    }

    private int parseLineNumber(String lineStr, String filePath, String instanceId, String changeType) {
        if (lineStr == null || lineStr.trim().isEmpty()) {
            logger.warn("WARN: Line number string is null or empty for file '{}', instanceId '{}', changeType '{}'. Defaulting to 0.", filePath, instanceId, changeType);
            return 0;
        }

        String cleanedLineStr = lineStr.replace(",", "");

        try {
            return Integer.parseInt(cleanedLineStr);
        } catch (NumberFormatException e) {
            logger.error("Error parsing {} line number string: '{}' (original: '{}') for file '{}', instanceId '{}'.",
                    changeType, cleanedLineStr, lineStr, filePath, instanceId, e);
            throw e;
        }
    }

    private static Schema getRemediationsSchema() throws SAXException, IOException {
        if (remediationsSchema == null) {
            synchronized (AuditProcessor.class) {
                if (remediationsSchema == null) {
                    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                    try (InputStream schemaStream = AuditProcessor.class.getResourceAsStream(REMEDIATIONS_XSD_PATH)) {
                        if (schemaStream == null) {
                            throw new IOException("Cannot find resource: " + REMEDIATIONS_XSD_PATH + ". Is it in the JAR?");
                        }
                        remediationsSchema = factory.newSchema(new javax.xml.transform.stream.StreamSource(schemaStream));
                    }
                }
            }
        }
        return remediationsSchema;
    }

    private void validateRemediationsDocument(Document doc) throws SAXException, IOException {
        Schema schema = getRemediationsSchema();
        Validator validator = schema.newValidator();

        validator.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void error(org.xml.sax.SAXParseException e) throws SAXException {
                logger.error("Schema Validation Error: Line {}, Column {}: {}", e.getLineNumber(), e.getColumnNumber(), e.getMessage());
                throw e;
            }
            @Override
            public void fatalError(org.xml.sax.SAXParseException e) throws SAXException {
                logger.error("Schema Validation Fatal Error: Line {}, Column {}: {}", e.getLineNumber(), e.getColumnNumber(), e.getMessage());
                throw e;
            }
            @Override
            public void warning(org.xml.sax.SAXParseException e) {
                logger.warn("WARN: Schema Validation Warning: Line {}, Column {}: {}", e.getLineNumber(), e.getColumnNumber(), e.getMessage());
            }
        });

        validator.validate(new DOMSource(doc));
    }

    private boolean isRemediationElementValid(Element remediationElement, FPRInfo fprInfo) {
        String instanceId = remediationElement.getAttribute("instanceId");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document tempDoc = builder.newDocument();

            Element root = tempDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "Remediations");
            tempDoc.appendChild(root);

            Element projectInfo = tempDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "ProjectInfo");
            Element name = tempDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "Name");
            name.setTextContent(fprInfo.getBuildId() != null ? fprInfo.getBuildId() : "UnknownProject");
            Element date = tempDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "WriteDate");
            date.setTextContent(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
            projectInfo.appendChild(name);
            projectInfo.appendChild(date);
            root.appendChild(projectInfo);

            Element list = tempDoc.createElementNS(REMEDIATIONS_NAMESPACE_URI, "RemediationList");
            root.appendChild(list);

            org.w3c.dom.Node importedNode = tempDoc.importNode(remediationElement, true);
            list.appendChild(importedNode);

            // Validate the temporary document
            validateRemediationsDocument(tempDoc);
            return true;

        } catch (SAXException e) {
            String xmlContent = domElementToString(remediationElement);
            logger.error("Validation failed for remediation of issue instanceId: {}. It will be excluded. Reason: {}\n--- Invalid XML Snippet ---\n{}\n--- End Snippet ---",
                    instanceId, e.getMessage(), xmlContent);
            return false;
        } catch (Exception e) {
            logger.error("An unexpected error occurred during validation for issue instanceId: {}. It will be excluded.", instanceId, e);
            return false;
        }
    }

    /**
     * Converts a DOM Element to its XML string representation for logging.
     * @param element The element to convert.
     * @return A formatted XML string of the element.
     */
    private String domElementToString(Element element) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            java.io.StringWriter writer = new java.io.StringWriter();
            transformer.transform(new DOMSource(element), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (Exception e) {
            logger.error("Failed to serialize XML element for logging", e);
            return "Error converting element to string: " + e.getMessage();
        }
    }

    private void transformDomToStream(Document doc, OutputStream os) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (TransformerConfigurationException e) {
            logger.warn("WARN: Security feature not fully supported by TransformerFactory.", e);
        }
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(os);
        transformer.transform(source, result);
    }

    /**
     * A wrapper around an OutputStream that ignores the close() call.
     * This is essential when passing a ZipOutputStream to a utility like a Transformer
     * that would otherwise prematurely close the entire archive stream.
     */
    private static class NonClosingOutputStream extends FilterOutputStream {
        public NonClosingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }

        @Override
        public void close() {
        }
    }
}