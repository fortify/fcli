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

import static com.fortify.cli.aviator.fpr.processor.XmlParserUtils.readElementText;
import static com.fortify.cli.aviator.fpr.processor.XmlParserUtils.skipSection;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.fpr.model.FVDLMetadata;

/**
 * Parses metadata sections from FVDL XML.
 * Handles EngineData with rule metadata.
 */
public class MetadataParser {
    private static final Logger logger = LoggerFactory.getLogger(MetadataParser.class);

    /**
     * Parse EngineData section for rule metadata.
     * Collects Rule metadata with format: ruleId -> Map<groupName, groupValue>
     */
    public void parseEngineData(XMLStreamReader reader, FVDLMetadata fvdlMetadata)
            throws XMLStreamException {
        logger.debug("Streaming EngineData parsing started");
        String currentRuleId = null;
        Map<String, String> currentMetadata = null;
        Map<String, Map<String, String>> ruleMetadata = fvdlMetadata.getRuleMetadata();

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                switch (localName) {
                    case "EngineVersion":
                        fvdlMetadata.setEngineVersion(readElementText(reader));
                        break;
                    case "Properties":
                        skipSection(reader, localName);
                        break;
                    case "Rule":
                        currentRuleId = reader.getAttributeValue(null, "id");
                        currentMetadata = new HashMap<>();
                        break;
                    case "Group":
                        if (currentMetadata != null) {
                            String groupName = reader.getAttributeValue(null, "name");
                            String groupValue = readElementText(reader);
                            if (groupName != null && groupValue != null) {
                                currentMetadata.put(groupName, groupValue);
                            }
                        }
                        break;
                    default:
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String localName = reader.getLocalName();
                if ("Rule".equals(localName) && currentRuleId != null && currentMetadata != null) {
                    ruleMetadata.put(currentRuleId, currentMetadata);
                    currentRuleId = null;
                    currentMetadata = null;
                } else if ("EngineData".equals(localName)) {
                    logger.debug("Streaming Engine Data parsing completed");
                    return;
                }
            }
        }
        logger.debug("Streaming Engine Data parsing completed");
    }
}
