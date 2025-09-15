package com.fortify.cli.aviator.fpr.processor;

import com.fortify.cli.aviator.fpr.filter.Filter;
import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.FilterTemplate;
import com.fortify.cli.aviator.fpr.filter.FolderDefinition;
import com.fortify.cli.aviator.fpr.filter.PrimaryTag;
import com.fortify.cli.aviator.fpr.filter.TagDefinition;
import com.fortify.cli.aviator.fpr.filter.TagValue;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.FprHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FilterTemplateParser {

    private final FprHandle fprHandle;
    private static final Logger logger = LoggerFactory.getLogger(FilterTemplateParser.class);

    private Document doc;
    private AuditProcessor auditProcessor;

    public FilterTemplateParser(FprHandle fprHandle, AuditProcessor auditProcessor) {
        this.fprHandle = fprHandle;
        this.auditProcessor = auditProcessor;
    }

    public Optional<FilterTemplate> parseFilterTemplate() {
        try {
            Path filterTemplatePath = fprHandle.getPath("/filtertemplate.xml");

            if (!Files.exists(filterTemplatePath)) {
                logger.info("filtertemplate.xml not found in FPR");
                return Optional.empty();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setValidating(false);
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();

            try (InputStream templateStream = Files.newInputStream(filterTemplatePath)) {
                doc = builder.parse(templateStream);
            }

            Element root = doc.getDocumentElement();
            if (root == null) {
                logger.error("Root element not found in filtertemplate.xml.");
                return Optional.empty();
            }

            FilterTemplate filterTemplate = new FilterTemplate();
            filterTemplate.setVersion(root.getAttribute("version"));
            filterTemplate.setDisableEdit(Boolean.parseBoolean(root.getAttribute("disableEdit")));
            filterTemplate.setId(root.getAttribute("id"));
            filterTemplate.setName(getElementContent(root, "Name").orElse(null));
            filterTemplate.setDescription(getElementContent(root, "Description").orElse(null));

            // 1. Parse all folder definitions from the template first.
            List<FolderDefinition> allFolderDefinitions = parseFolderDefinitions(root);
            filterTemplate.setFolderDefinitions(allFolderDefinitions);

            // 2. Now, parse the filter sets, PASSING the folder definitions to them.
            filterTemplate.setFilterSets(parseFilterSets(root, allFolderDefinitions));

            filterTemplate.setDefaultFolder(getDefaultFolder(root).orElse(null));
            filterTemplate.setTagDefinitions(parseTagDefinitions(root));
            filterTemplate.setPrimaryTag(parsePrimaryTag(root).orElse(null));

            addMissingTagDefinitions(filterTemplate, doc);
            auditProcessor.setFilterTemplateDoc(doc);

            return Optional.of(filterTemplate);
        } catch (Exception e) {
            logger.error("Error parsing filtertemplate.xml", e);
            throw new RuntimeException("Failed to parse filtertemplate.xml", e);
        }
    }

    private boolean addMissingTagDefinitions(FilterTemplate filterTemplate, Document doc) {
        String namespaceURI = doc.getDocumentElement().getNamespaceURI();
        Element rootElement = doc.getDocumentElement();

        NodeList tagDefNodes = rootElement.getElementsByTagNameNS(namespaceURI != null ? namespaceURI : "", "TagDefinition");
        Node lastTagDefNode = tagDefNodes.getLength() > 0 ? tagDefNodes.item(tagDefNodes.getLength() - 1) : null;

        boolean needsUpdate = ensureTagDefinitionPresent(filterTemplate, Constants.AVIATOR_PREDICTION_TAG_ID, "Aviator prediction", Arrays.asList(Constants.AVIATOR_NOT_AN_ISSUE, Constants.AVIATOR_REMEDIATION_REQUIRED, Constants.AVIATOR_UNSURE, Constants.AVIATOR_EXCLUDED, Constants.AVIATOR_LIKELY_TP, Constants.AVIATOR_LIKELY_FP), doc, namespaceURI, lastTagDefNode);
        needsUpdate |= ensureTagDefinitionPresent(filterTemplate, Constants.AVIATOR_STATUS_TAG_ID, "Aviator status", Arrays.asList(Constants.PROCESSED_BY_AVIATOR), doc, namespaceURI, lastTagDefNode);
        needsUpdate |= ensureTagDefinitionPresent(filterTemplate, Constants.FOD_TAG_ID, "FoD", Arrays.asList(Constants.PENDING_REVIEW, Constants.FALSE_POSITIVE, Constants.EXPLOITABLE, Constants.SUSPICIOUS, Constants.SANITIZED), doc, namespaceURI, lastTagDefNode);
        needsUpdate |= ensureTagDefinitionPresent(filterTemplate, Constants.AUDITOR_STATUS_TAG_ID, "Auditor Status", Arrays.asList(Constants.PENDING_REVIEW, Constants.NOT_AN_ISSUE, Constants.UNSURE, Constants.REMEDIATION_REQUIRED, Constants.PROPOSED_NOT_AN_ISSUE, Constants.SUSPICIOUS), doc, namespaceURI, lastTagDefNode);

        return needsUpdate;
    }

    private boolean ensureTagDefinitionPresent(FilterTemplate filterTemplate, String tagId, String tagName, List<String> tagValues, Document doc, String namespaceURI, Node insertAfterNode) {
        Optional<TagDefinition> existingTag = filterTemplate.getTagDefinitions().stream().filter(t -> tagId.equalsIgnoreCase(t.getId())).findFirst();

        if (!existingTag.isPresent()) {
            TagDefinition newTagDef = createTagDefinition(tagId, tagName, tagValues);
            filterTemplate.getTagDefinitions().add(newTagDef);

            Element rootElement = doc.getDocumentElement();
            Element newTagDefElement = doc.createElementNS(namespaceURI, "TagDefinition");
            newTagDefElement.setAttribute("id", newTagDef.getId());
            newTagDefElement.setAttribute("type", "user");
            newTagDefElement.setAttribute("extensible", "false");
            newTagDefElement.setAttribute("hidden", "false");
            newTagDefElement.setAttribute("objectVersion", "0");
            newTagDefElement.setAttribute("valueType", "LIST");

            Element nameElement = doc.createElementNS(namespaceURI, "name");
            nameElement.setTextContent(newTagDef.getName());
            newTagDefElement.appendChild(nameElement);

            Element descriptionElement = doc.createElementNS(namespaceURI, "Description");
            newTagDefElement.appendChild(descriptionElement);

            for (String value : newTagDef.getTagValuesAsString()) {
                Element valueElement = doc.createElementNS(namespaceURI, "value");
                valueElement.setAttribute("id", String.valueOf(newTagDef.getTagValuesAsString().indexOf(value)));
                valueElement.setAttribute("hidden", "false");
                valueElement.setTextContent(value);
                newTagDefElement.appendChild(valueElement);
            }
            if (insertAfterNode != null) {
                rootElement.insertBefore(newTagDefElement, insertAfterNode.getNextSibling());
            } else {
                rootElement.appendChild(newTagDefElement);
            }
            return true;
        }
        return false;
    }

    private TagDefinition createTagDefinition(String id, String name, List<String> values) {
        TagDefinition tagDefinition = new TagDefinition();
        tagDefinition.setId(id);
        tagDefinition.setName(name);
        tagDefinition.setValues(values.stream().map(value -> {
            TagValue tagValue = new TagValue();
            tagValue.setValue(value);
            tagValue.setId(String.valueOf(values.indexOf(value)));
            tagValue.setHidden(false);
            return tagValue;
        }).collect(Collectors.toList()));
        return tagDefinition;
    }

    private Optional<Path> findFilterTemplatePath(Path extractedPath) {
        try {
            return Files.walk(extractedPath).filter(path -> path.getFileName().toString().equals("filtertemplate.xml")).findFirst();
        } catch (Exception e) {
            logger.error("Error finding filtertemplate.xml: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private void saveDocument(Document doc, File outputFile) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = transformerFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            if (doc.getDoctype() != null) {
                transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doc.getDoctype().getPublicId());
                transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doc.getDoctype().getSystemId());
            }

            if (outputFile.exists()) {
                File backupFile = new File(outputFile.getParent(), outputFile.getName() + ".backup");
                Files.copy(outputFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(outputFile);
            transformer.transform(source, result);

            logger.info("Successfully saved updated filtertemplate.xml");
        } catch (Exception e) {
            logger.error("Error saving XML document: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save XML document", e);
        }
    }

    private Optional<String> getElementContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return Optional.ofNullable(nodeList.item(0).getTextContent());
        }
        return Optional.empty();
    }

    private List<FolderDefinition> parseFolderDefinitions(Element root) {
        List<FolderDefinition> folderDefinitions = new ArrayList<>();
        NodeList folderDefNodes = root.getElementsByTagName("FolderDefinition");
        for (int i = 0; i < folderDefNodes.getLength(); i++) {
            Element folderDefElement = (Element) folderDefNodes.item(i);
            FolderDefinition folderDef = new FolderDefinition();
            folderDef.setId(folderDefElement.getAttribute("id"));
            folderDef.setColor(folderDefElement.getAttribute("color"));
            folderDef.setName(getElementContent(folderDefElement, "name").orElse(null));
            folderDef.setDescription(getElementContent(folderDefElement, "description").orElse(null));
            folderDefinitions.add(folderDef);
        }
        return folderDefinitions;
    }

    private Optional<String> getDefaultFolder(Element root) {
        NodeList defaultFolderNode = root.getElementsByTagName("DefaultFolder");
        if (defaultFolderNode.getLength() > 0) {
            Element defaultFolderElement = (Element) defaultFolderNode.item(0);
            return Optional.of(defaultFolderElement.getAttribute("folderID"));
        }
        return Optional.empty();
    }

    private List<TagDefinition> parseTagDefinitions(Element root) {
        List<TagDefinition> tagDefinitions = new ArrayList<>();
        NodeList tagDefNodes = root.getElementsByTagName("TagDefinition");
        for (int i = 0; i < tagDefNodes.getLength(); i++) {
            Element tagDefElement = (Element) tagDefNodes.item(i);
            TagDefinition tagDef = new TagDefinition();
            tagDef.setId(tagDefElement.getAttribute("id"));
            tagDef.setType(tagDefElement.getAttribute("type"));
            tagDef.setExtensible(Boolean.parseBoolean(tagDefElement.getAttribute("extensible")));
            tagDef.setHidden(Boolean.parseBoolean(tagDefElement.getAttribute("hidden")));
            tagDef.setRestriction(tagDefElement.getAttribute("restriction"));
            tagDef.setObjectVersion(parseIntAttribute(tagDefElement, "objectVersion", 0));
            tagDef.setValueType(tagDefElement.getAttribute("valueType"));
            tagDef.setName(getElementContent(tagDefElement, "name").orElse(null));
            tagDef.setDescription(getElementContent(tagDefElement, "Description").orElse(null));
            tagDef.setValues(parseTagValues(tagDefElement));
            tagDefinitions.add(tagDef);
        }
        return tagDefinitions;
    }

    private List<TagValue> parseTagValues(Element tagDefElement) {
        List<TagValue> tagValues = new ArrayList<>();
        NodeList valueNodes = tagDefElement.getElementsByTagName("value");
        if (valueNodes != null) {
            for (int j = 0; j < valueNodes.getLength(); j++) {
                Element valueElement = (Element) valueNodes.item(j);
                TagValue tagValue = new TagValue();
                tagValue.setDefault(Boolean.parseBoolean(valueElement.getAttribute("isDefault")));
                tagValue.setId(valueElement.getAttribute("id"));
                tagValue.setDescription(valueElement.getAttribute("Description"));
                tagValue.setHidden(Boolean.parseBoolean(valueElement.getAttribute("hidden")));
                tagValue.setValue(valueElement.getTextContent());
                tagValues.add(tagValue);
            }
        }
        return tagValues;
    }

    private Optional<PrimaryTag> parsePrimaryTag(Element root) {
        NodeList primaryTagNode = root.getElementsByTagName("PrimaryTag");
        if (primaryTagNode.getLength() > 0) {
            Element primaryTagElement = (Element) primaryTagNode.item(0);
            PrimaryTag primaryTag = new PrimaryTag();
            primaryTag.setPrimaryTagGUID(getElementContent(primaryTagElement, "primaryTagGUID").orElse(null));
            primaryTag.setNeutralWeight(parseIntContent(getElementContent(primaryTagElement, "neutralWeight").orElse("0")));
            primaryTag.setOpenRange(getElementContent(primaryTagElement, "openRange").orElse(null));
            primaryTag.setNaiRange(getElementContent(primaryTagElement, "naiRange").orElse(null));
            return Optional.of(primaryTag);
        }
        return Optional.empty();
    }

    private List<FilterSet> parseFilterSets(Element root, List<FolderDefinition> allFolderDefinitions) {
        List<FilterSet> filterSets = new ArrayList<>();
        NodeList filterSetNodes = root.getElementsByTagName("FilterSet");
        for (int i = 0; i < filterSetNodes.getLength(); i++) {
            Element filterSetElement = (Element) filterSetNodes.item(i);
            FilterSet filterSet = new FilterSet();
            filterSet.setType(filterSetElement.getAttribute("type"));
            filterSet.setId(filterSetElement.getAttribute("id"));
            filterSet.setEnabled(Boolean.parseBoolean(filterSetElement.getAttribute("enabled")));
            filterSet.setDisableEdit(Boolean.parseBoolean(filterSetElement.getAttribute("disableEdit")));
            filterSet.setTitle(getElementContent(filterSetElement, "Title").orElse(null));
            filterSet.setDescription(getElementContent(filterSetElement, "Description").orElse(null));
            filterSet.setEnabledFolders(parseEnabledFolders(filterSetElement));
            filterSet.setFilters(parseFilters(filterSetElement));
            filterSet.setFolderDefinitions(allFolderDefinitions);

            filterSets.add(filterSet);
        }
        return filterSets;
    }

    private List<String> parseEnabledFolders(Element filterSetElement) {
        List<String> enabledFolders = new ArrayList<>();
        NodeList enabledFolderNodes = filterSetElement.getElementsByTagName("EnabledFolders");
        for (int j = 0; j < enabledFolderNodes.getLength(); j++) {
            enabledFolders.add(enabledFolderNodes.item(j).getTextContent());
        }
        return enabledFolders;
    }

    private List<Filter> parseFilters(Element filterSetElement) {
        List<Filter> filters = new ArrayList<>();
        NodeList filterNodes = filterSetElement.getElementsByTagName("Filter");
        for (int j = 0; j < filterNodes.getLength(); j++) {
            Element filterElement = (Element) filterNodes.item(j);
            Filter filter = new Filter();
            filter.setActionParam(getElementContent(filterElement, "actionParam").orElse(null));
            filter.setQuery(getElementContent(filterElement, "query").orElse(null));
            filter.setAction(getElementContent(filterElement, "action").orElse(null));
            filters.add(filter);
        }
        return filters;
    }

    private int parseIntAttribute(Element element, String attributeName, int defaultValue) {
        String attributeValue = element.getAttribute(attributeName);
        if (attributeValue != null && !attributeValue.isEmpty()) {
            try {
                return Integer.parseInt(attributeValue);
            } catch (NumberFormatException e) {
                logger.error("Error parsing integer attribute {}: {}", attributeName, e.getMessage());
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private int parseIntContent(String content) {
        try {
            return Integer.parseInt(content);
        } catch (NumberFormatException e) {
            logger.error("Error parsing integer content: {}", e.getMessage());
            return 0;
        }
    }
}