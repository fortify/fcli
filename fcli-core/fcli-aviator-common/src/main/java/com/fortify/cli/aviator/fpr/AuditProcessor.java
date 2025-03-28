package com.fortify.cli.aviator.fpr;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.core.model.AuditResponse;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.StringUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


public class AuditProcessor {

    Logger logger = LoggerFactory.getLogger(AuditProcessor.class);
    private static final String NAMESPACE_URI = "xmlns://www.fortify.com/schema/audit";


    private Document auditDoc;
    private Document filterTemplateDoc;

    public void setFilterTemplateDoc(Document doc) {
        this.filterTemplateDoc = doc;
    }

    private final Map<String, AuditIssue> auditIssueMap = new HashMap<>();
    private final String fprFilePath;

    public AuditProcessor(Path extractedPath, String fprFilePath) {
        this.extractedPath = extractedPath;
        this.fprFilePath = fprFilePath;
    }

    @Getter
    private Path extractedPath;

    public void saveFilterTemplateXml(Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        DOMSource source = new DOMSource(doc);
        Path filterTemplatePath = extractedPath.resolve("filtertemplate.xml");
        StreamResult result = new StreamResult(filterTemplatePath.toFile());
        transformer.transform(source, result);
        logger.debug("Updated filtertemplate.xml saved to: {}", filterTemplatePath);
    }

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

            NodeList issueNodes = auditDoc.getElementsByTagNameNS(NAMESPACE_URI, "Issue");
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

            Element rootElement = doc.createElementNS(NAMESPACE_URI, "ns2:Audit");
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

            Element projectInfoElement = doc.createElementNS(NAMESPACE_URI, "ns2:ProjectInfo");
            Element projectNameElement = doc.createElementNS(NAMESPACE_URI, "ns2:Name");
            projectNameElement.setTextContent("Unknown Project");
            Element projectVersionIdElement = doc.createElementNS(NAMESPACE_URI, "ns2:ProjectVersionId");
            projectVersionIdElement.setTextContent("-1");
            Element writeDateElement = doc.createElementNS(NAMESPACE_URI, "ns2:WriteDate");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            writeDateElement.setTextContent(dateFormat.format(new Date()));

            projectInfoElement.appendChild(projectNameElement);
            projectInfoElement.appendChild(projectVersionIdElement);
            projectInfoElement.appendChild(writeDateElement);
            rootElement.appendChild(projectInfoElement);

            Element issueListElement = doc.createElementNS(NAMESPACE_URI, "ns2:IssueList");
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
        NodeList tagNodes = issueElement.getElementsByTagNameNS(NAMESPACE_URI, "Tag");
        for (int j = 0; j < tagNodes.getLength(); j++) {
            Element tagElement = (Element) tagNodes.item(j);
            String tagId = tagElement.getAttribute("id");
            String tagValue = Optional.ofNullable(getTagValue(tagElement)).orElse("");
            tags.put(tagId, tagValue);
        }
        auditIssueBuilder.tags(tags);

        List<AuditIssue.Comment> threadedComments = new ArrayList<>();
        NodeList commentNodes = issueElement.getElementsByTagNameNS(NAMESPACE_URI, "Comment");
        for (int j = 0; j < commentNodes.getLength(); j++) {
            Element commentElement = (Element) commentNodes.item(j);
            AuditIssue.Comment comment = AuditIssue.Comment.builder()
                    .content(Optional.ofNullable(getFirstElementContentNS(commentElement, NAMESPACE_URI, "Content", "")).orElse(""))
                    .username(Optional.ofNullable(getFirstElementContentNS(commentElement, NAMESPACE_URI, "Username", "")).orElse(""))
                    .timestamp(Optional.ofNullable(getFirstElementContentNS(commentElement, NAMESPACE_URI, "Timestamp", "")).orElse(""))
                    .build();
            threadedComments.add(comment);
        }
        auditIssueBuilder.threadedComments(threadedComments);

        return auditIssueBuilder.build();
    }


    private String getTagValue(Element tagElement) {
        NodeList valueNodes = tagElement.getElementsByTagNameNS(NAMESPACE_URI, "Value");
        if (valueNodes.getLength() > 0) {
            return valueNodes.item(0).getTextContent();
        }
        return "";
    }

    private String getFirstElementContentNS(Element parent, String namespace, String tagName, String defaultValue) {
        NodeList nodes = parent.getElementsByTagNameNS(namespace, tagName);
        if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null) {
            return nodes.item(0).getTextContent();
        }
        return defaultValue;
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

    private void updateAuditXml(Map<String, AuditResponse> auditResponses, String resultsTagId) throws AviatorTechnicalException {
        for (Map.Entry<String, AuditResponse> entry : auditResponses.entrySet()) {
            String instanceId = entry.getKey();
            AuditResponse response = entry.getValue();
            Element issueElement = findIssueElement(instanceId);
            resultsTagId = StringUtil.isEmpty(resultsTagId) ? Constants.AUDITOR_STATUS_TAG_ID : resultsTagId;

            if (response.getTier() != null) {
                if (issueElement != null) {
                    updateIssueElement(issueElement, response, resultsTagId);
                } else {
                    addNewIssueElement(instanceId, response, resultsTagId);
                }
            } else {
                logger.debug("Issue is Skipped {}", response.getIssueId());
            }
        }
    }

    public Element findIssueElement(String instanceId) {
        NodeList issueNodes = auditDoc.getElementsByTagNameNS(NAMESPACE_URI, "Issue");
        for (int i = 0; i < issueNodes.getLength(); i++) {
            Element issueElement = (Element) issueNodes.item(i);
            if (issueElement.getAttribute("instanceId").equals(instanceId)) {
                return issueElement;
            }
        }
        return null;
    }

    public void updateIssueElement(Element issueElement, AuditResponse response,String resultsTagId) {
        int revision = Integer.parseInt(issueElement.getAttribute("revision"));
        issueElement.setAttribute("revision", String.valueOf(++revision));


        if (response != null && response.getAuditResult() != null) {
            String tagValue = response.getAuditResult().tagValue;
            String tier = response.getTier();
            if (tier != null && tier.equalsIgnoreCase("GOLD")) {
                if (Constants.NOT_AN_ISSUE.equalsIgnoreCase(tagValue)) {
                    updateOrAddTag(issueElement, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_NOT_AN_ISSUE);
                    updateOrAddTag(issueElement, resultsTagId, Constants.NOT_AN_ISSUE);
                    issueElement.setAttribute("suppressed", "true");
                } else if (Constants.EXPLOITABLE.equalsIgnoreCase(tagValue)) {
                    updateOrAddTag(issueElement, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_REMEDIATION_REQUIRED);
                    updateOrAddTag(issueElement, resultsTagId, Constants.EXPLOITABLE);
                } else if (Constants.UNSURE.equalsIgnoreCase(tagValue)) {
                    updateOrAddTag(issueElement, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_UNSURE);
                    updateOrAddTag(issueElement, resultsTagId, Constants.SUSPICIOUS);
                }
            } else {
                if (Constants.NOT_AN_ISSUE.equalsIgnoreCase(tagValue)) {
                    updateOrAddTag(issueElement, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_LIKELY_FP);
                    updateOrAddTag(issueElement, resultsTagId, Constants.RELIABILITY_ISSUE);
                } else if (Constants.EXPLOITABLE.equalsIgnoreCase(tagValue)) {
                    updateOrAddTag(issueElement, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_LIKELY_TP);
                    updateOrAddTag(issueElement, resultsTagId, Constants.SUSPICIOUS);
                } else if (Constants.UNSURE.equalsIgnoreCase(tagValue)) {
                    updateOrAddTag(issueElement, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_UNSURE);
                    updateOrAddTag(issueElement, resultsTagId, Constants.SUSPICIOUS);
                }
            }
        }

        updateOrAddTag(issueElement, Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR);

        updateOrAddComment(issueElement, response.getAuditResult().comment);

        updateClientAuditTrail(issueElement, response, resultsTagId);
    }

    private void updateClientAuditTrail(Element issueElement, AuditResponse response, String resultsTagId) {
        Element clientAuditTrail = getClientAuditTrailElement(issueElement);

        if (response != null && response.getAuditResult() != null) {
            String tagValue = response.getAuditResult().tagValue;
            String tier = response.getTier();
            if (tier != null && tier.equalsIgnoreCase("GOLD")) {
                if (Constants.NOT_AN_ISSUE.equalsIgnoreCase(tagValue)) {
                    addTagHistory(clientAuditTrail, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_NOT_AN_ISSUE);
                    addTagHistory(clientAuditTrail, resultsTagId, Constants.NOT_AN_ISSUE);
                    issueElement.setAttribute("suppressed", "true");
                } else if (Constants.EXPLOITABLE.equalsIgnoreCase(tagValue)) {
                    addTagHistory(clientAuditTrail, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_REMEDIATION_REQUIRED);
                    addTagHistory(clientAuditTrail, resultsTagId, Constants.EXPLOITABLE);
                } else if (Constants.UNSURE.equalsIgnoreCase(tagValue)) {
                    addTagHistory(clientAuditTrail, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_UNSURE);
                    addTagHistory(clientAuditTrail, resultsTagId, Constants.SUSPICIOUS);
                }
            } else {
                if (Constants.NOT_AN_ISSUE.equalsIgnoreCase(tagValue)) {
                    addTagHistory(clientAuditTrail, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_LIKELY_FP);
                    addTagHistory(clientAuditTrail, resultsTagId, Constants.RELIABILITY_ISSUE);
                } else if (Constants.EXPLOITABLE.equalsIgnoreCase(tagValue)) {
                    addTagHistory(clientAuditTrail, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_LIKELY_TP);
                    addTagHistory(clientAuditTrail, resultsTagId, Constants.SUSPICIOUS);
                } else if (Constants.UNSURE.equalsIgnoreCase(tagValue)) {
                    addTagHistory(clientAuditTrail, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_UNSURE);
                    addTagHistory(clientAuditTrail, resultsTagId, Constants.SUSPICIOUS);
                }
            }
        }
        addTagHistory(clientAuditTrail, Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR);

    }

    private Element getClientAuditTrailElement(Element issueElement) {
        NodeList clientAuditTrailNodes = issueElement.getElementsByTagNameNS(NAMESPACE_URI, "ClientAuditTrail");
        Element clientAuditTrail;
        if (clientAuditTrailNodes.getLength() > 0) {
            clientAuditTrail = (Element) clientAuditTrailNodes.item(0);
        } else {
            clientAuditTrail = auditDoc.createElementNS(NAMESPACE_URI, "ClientAuditTrail");
            issueElement.appendChild(clientAuditTrail);
        }
        return clientAuditTrail;
    }

    private void addTagHistory(Element clientAuditTrail, String tagId, String tagValue) {
        Element tagHistory = auditDoc.createElementNS(NAMESPACE_URI, "TagHistory");

        Element tag = auditDoc.createElementNS(NAMESPACE_URI, "Tag");
        tag.setAttribute("id", tagId);
        Element value = auditDoc.createElementNS(NAMESPACE_URI, "Value");
        value.setTextContent(tagValue);
        tag.appendChild(value);
        tagHistory.appendChild(tag);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        Element editTime = auditDoc.createElementNS(NAMESPACE_URI, "EditTime");
        editTime.setTextContent(dateFormat.format(new Date()));
        tagHistory.appendChild(editTime);

        Element username = auditDoc.createElementNS(NAMESPACE_URI, "Username");
        username.setTextContent("Fortify Aviator");
        tagHistory.appendChild(username);

        clientAuditTrail.appendChild(tagHistory);
    }

    private void updateOrAddTag(Element issueElement, String tagId, String tagValue) {
        NodeList tagNodes = issueElement.getElementsByTagNameNS(NAMESPACE_URI, "Tag");
        Element tagElement = null;

        for (int i = 0; i < tagNodes.getLength(); i++) {
            Element currentTag = (Element) tagNodes.item(i);
            if (currentTag.getAttribute("id").equalsIgnoreCase(tagId)) {
                tagElement = currentTag;
                break;
            }
        }

        if (tagElement == null) {
            tagElement = auditDoc.createElementNS(NAMESPACE_URI, "Tag");
            tagElement.setAttribute("id", tagId);
            issueElement.appendChild(tagElement);
        }

        NodeList valueNodes = tagElement.getElementsByTagNameNS(NAMESPACE_URI, "Value");
        Element valueElement;
        if (valueNodes.getLength() > 0) {
            valueElement = (Element) valueNodes.item(0);
        } else {
            valueElement = auditDoc.createElementNS(NAMESPACE_URI, "Value");
            tagElement.appendChild(valueElement);
        }

        valueElement.setTextContent(tagValue);
    }

    private void updateOrAddComment(Element issueElement, String commentText) {
        NodeList threadedCommentsNodes = issueElement.getElementsByTagNameNS(NAMESPACE_URI, "ThreadedComments");
        Element threadedCommentsElement;

        if (threadedCommentsNodes.getLength() > 0) {
            threadedCommentsElement = (Element) threadedCommentsNodes.item(0);
        } else {
            threadedCommentsElement = auditDoc.createElementNS(NAMESPACE_URI, "ThreadedComments");
            issueElement.appendChild(threadedCommentsElement);
        }

        Element commentElement = auditDoc.createElementNS(NAMESPACE_URI, "Comment");

        Element contentElement = auditDoc.createElementNS(NAMESPACE_URI, "Content");
        contentElement.setTextContent(commentText);
        commentElement.appendChild(contentElement);

        Element usernameElement = auditDoc.createElementNS(NAMESPACE_URI, "Username");
        usernameElement.setTextContent("Fortify Aviator");
        commentElement.appendChild(usernameElement);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        Element timestampElement = auditDoc.createElementNS(NAMESPACE_URI, "Timestamp");
        timestampElement.setTextContent(dateFormat.format(new Date()));
        commentElement.appendChild(timestampElement);

        threadedCommentsElement.appendChild(commentElement);
    }

    public void addNewIssueElement(String instanceId, AuditResponse response, String resultsTagId) {
        Element issueList = (Element) auditDoc.getElementsByTagNameNS(NAMESPACE_URI, "IssueList").item(0);
        if (issueList == null) {
            issueList = auditDoc.createElementNS(NAMESPACE_URI, "IssueList");
            auditDoc.getDocumentElement().appendChild(issueList);
        }



        Element newIssue = auditDoc.createElementNS(NAMESPACE_URI, "Issue");
        newIssue.setAttribute("instanceId", instanceId);
        newIssue.setAttribute("revision", "0");

        if (response != null && response.getAuditResult() != null) {
            String tagValue = response.getAuditResult().tagValue;
            String tier = response.getTier();
            if (tier != null && tier.equalsIgnoreCase("GOLD")) {
                if (Constants.NOT_AN_ISSUE.equalsIgnoreCase(tagValue)) {
                    updateOrAddTag(newIssue, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_NOT_AN_ISSUE);
                    updateOrAddTag(newIssue, resultsTagId, Constants.NOT_AN_ISSUE);
                    newIssue.setAttribute("suppressed", "true");
                } else if (Constants.EXPLOITABLE.equalsIgnoreCase(tagValue)) {
                    updateOrAddTag(newIssue, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_REMEDIATION_REQUIRED);
                    updateOrAddTag(newIssue, resultsTagId, Constants.EXPLOITABLE);
                } else if (Constants.UNSURE.equalsIgnoreCase(tagValue)) {
                    updateOrAddTag(newIssue, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_UNSURE);
                    updateOrAddTag(newIssue, resultsTagId, Constants.SUSPICIOUS);
                }
            } else {
                if (Constants.NOT_AN_ISSUE.equalsIgnoreCase(tagValue)) {
                    updateOrAddTag(newIssue, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_LIKELY_FP);
                    updateOrAddTag(newIssue, resultsTagId, Constants.RELIABILITY_ISSUE);
                } else if (Constants.EXPLOITABLE.equalsIgnoreCase(tagValue)) {
                    updateOrAddTag(newIssue, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_LIKELY_TP);
                    updateOrAddTag(newIssue, resultsTagId, Constants.SUSPICIOUS);
                } else if (Constants.UNSURE.equalsIgnoreCase(tagValue)) {
                    updateOrAddTag(newIssue, Constants.AVIATOR_PREDICTION_TAG_ID, Constants.AVIATOR_UNSURE);
                    updateOrAddTag(newIssue, resultsTagId, Constants.SUSPICIOUS);
                }
            }
        }
        updateOrAddTag(newIssue, Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR);


        if (response != null && response.getAuditResult() != null) {
            updateOrAddComment(newIssue, response.getAuditResult().comment);
        }

        updateClientAuditTrail(newIssue, response, resultsTagId);

        issueList.appendChild(newIssue);
    }

    public File updateAndSaveAuditXml(Map<String, AuditResponse> auditResponses, String resultsTagId) throws AviatorTechnicalException {
        updateAuditXml(auditResponses, resultsTagId);
        File updatedFile = updateContentInOriginalFpr();
        return updatedFile;
    }

    private File updateContentInOriginalFpr() throws AviatorTechnicalException {
        String originalFprPath = fprFilePath;
        String tempFprPath = originalFprPath + ".tmp";
        Path tempPath = Paths.get(tempFprPath);

        try (ZipFile zipFile = new ZipFile(originalFprPath)) {
        } catch (IOException e) {
            logger.error("Input FPR file is invalid or cannot be read: {}", originalFprPath, e);
            throw new AviatorTechnicalException("Invalid or unreadable input FPR file.", e);
        }

        try {
            Files.copy(Paths.get(originalFprPath), tempPath);

            try (ZipFile zipFile = new ZipFile(tempFprPath);
                 FileOutputStream fos = new FileOutputStream(originalFprPath);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                AtomicBoolean auditXmlExists = new AtomicBoolean(false);
                AtomicBoolean filterTemplateXmlExists = new AtomicBoolean(false);

                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while(entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    ZipEntry newEntry = new ZipEntry(entryName);

                    try {
                        if (entryName.equals("audit.xml")) {
                            auditXmlExists.set(true);
                            zos.putNextEntry(newEntry);
                            transformDomToStream(auditDoc, zos);
                            zos.closeEntry();
                        } else if (filterTemplateDoc != null && entryName.equals("filtertemplate.xml")) {
                            filterTemplateXmlExists.set(true);
                            zos.putNextEntry(newEntry);
                            transformDomToStream(filterTemplateDoc, zos);
                            zos.closeEntry();
                        } else {
                            zos.putNextEntry(newEntry);
                            if (!entry.isDirectory()) {
                                try (InputStream is = zipFile.getInputStream(entry)) {
                                    byte[] buffer = new byte[4096];
                                    int len;
                                    while ((len = is.read(buffer)) > 0) {
                                        zos.write(buffer, 0, len);
                                    }
                                }
                            }
                            zos.closeEntry();
                        }
                    } catch (TransformerException | IOException e) {
                        logger.error("Error processing zip entry: {}", entryName, e);
                        throw new AviatorTechnicalException("Error processing zip entry: " + entryName, e);
                    }
                }

                if (!auditXmlExists.get()) {
                    zos.putNextEntry(new ZipEntry("audit.xml"));
                    transformDomToStream(auditDoc, zos);
                    zos.closeEntry();
                }

                if (filterTemplateDoc != null && !filterTemplateXmlExists.get()) {
                    zos.putNextEntry(new ZipEntry("filtertemplate.xml"));
                    transformDomToStream(filterTemplateDoc, zos);
                    zos.closeEntry();
                }

                zos.finish();
            }

        } catch (IOException | TransformerException e) {
            logger.error("Error updating content in original FPR", e);
            try {
                Path path = Paths.get(originalFprPath);
                Files.deleteIfExists(path);
                Files.move(tempPath, path);
                logger.info("Restored original FPR from backup due to error.");
            } catch(IOException restoreEx) {
                logger.error("Failed to restore original FPR from backup at {}: {}", tempFprPath, restoreEx.getMessage());
            }
            throw new AviatorTechnicalException("Error updating FPR content.", e);
        } finally {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException e) {
                logger.warn("Failed to delete temporary FPR file: {}", tempFprPath, e);
            }
        }
        return new File(originalFprPath);
    }

    private void transformDomToStream(Document doc, ZipOutputStream zos) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (TransformerConfigurationException e) {
            logger.warn("Security feature {} not supported by TransformerFactory.", javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, e);
        }
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(zos);
        transformer.transform(source, result);
    }
}