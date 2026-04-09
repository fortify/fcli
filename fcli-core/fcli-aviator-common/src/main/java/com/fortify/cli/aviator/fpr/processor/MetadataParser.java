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

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void parseEngineData(XMLStreamReader reader, Map<String, Map<String, String>> ruleMetadata)
            throws XMLStreamException {
        logger.debug("Streaming EngineData parsing started");
        int depth = 1;
        String currentRuleId = null;
        Map<String, String> currentMetadata = null;

        while (reader.hasNext() && depth > 0) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                if ("Rule".equals(localName)) {
                    currentRuleId = reader.getAttributeValue(null, "id");
                    currentMetadata = new HashMap<>();
                } else if ("Group".equals(localName) && currentMetadata != null) {
                    String groupName = reader.getAttributeValue(null, "name");
                    String groupValue = readElementText(reader);
                    if (groupName != null && groupValue != null) {
                        currentMetadata.put(groupName, groupValue);
                    }
                }
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
                if ("Rule".equals(reader.getLocalName()) && currentRuleId != null && currentMetadata != null) {
                    ruleMetadata.put(currentRuleId, currentMetadata);
                    currentRuleId = null;
                    currentMetadata = null;
                }
            }
        }
        logger.debug("Streaming Engine Data parsing completed");
    }
}
