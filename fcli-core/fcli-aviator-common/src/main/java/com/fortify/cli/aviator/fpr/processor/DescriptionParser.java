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

import static com.fortify.cli.aviator.fpr.processor.XmlParserUtils.readElementContentWithMarkup;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.fpr.model.*;

/**
 * Parses Description elements from FVDL XML.
 * Handles Abstract and Explanation content with nested markup.
 */
public class DescriptionParser {
    private static final Logger logger = LoggerFactory.getLogger(DescriptionParser.class);

    /**
     * Parse a single Description element.
     *
     * @param reader XMLStreamReader positioned at Description start element
     * @param classID The classID attribute value
     * @return StreamedDescription object or null
     */
    public StreamedDescription parseDescription(XMLStreamReader reader, String classID) throws XMLStreamException {
        StreamedDescription.StreamedDescriptionBuilder builder = StreamedDescription.builder();
        builder.classID(classID);

        String abstractText = null;
        String explanationText = null;

        int depth = 1;
        while (reader.hasNext() && depth > 0) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                if ("Abstract".equals(localName)) {
                    abstractText = readElementContentWithMarkup(reader, "Abstract");
                    continue;

                } else if ("Explanation".equals(localName)) {
                    explanationText = readElementContentWithMarkup(reader, "Explanation");
                    continue;
                }
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
            }
        }

        builder.abstractText(abstractText);
        builder.explanation(explanationText);

        return builder.build();
    }

    /**
     * Parse the entire Descriptions section.
     */
    public void parseDescriptions(XMLStreamReader reader, java.util.Map<String, StreamedDescription> descriptionCache)
            throws XMLStreamException {
        logger.debug("Streaming parsing of Descriptions");

        String classID = reader.getAttributeValue(null, "classID");
        if (classID != null && !classID.isEmpty()) {
            StreamedDescription description = parseDescription(reader, classID);
            if (description != null) {
                descriptionCache.put(classID, description);
            }
        } else {
            logger.warn("Description missing classID, skipping");
        }
    }
}
