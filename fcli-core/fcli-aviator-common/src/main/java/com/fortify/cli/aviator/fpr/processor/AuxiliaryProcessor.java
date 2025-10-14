/*
 * Copyright 2021-2025 Open Text.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.fpr.jaxb.AuxiliaryData;
import com.fortify.cli.aviator.fpr.jaxb.Entry;
import com.fortify.cli.aviator.fpr.jaxb.ExternalEntries;
import com.fortify.cli.aviator.fpr.jaxb.ExternalID;
import com.fortify.cli.aviator.fpr.jaxb.Field;
import com.fortify.cli.aviator.fpr.jaxb.SourceLocationType;
import com.fortify.cli.aviator.fpr.jaxb.Vulnerability;

/**
 * Processor for AuxiliaryData and ExternalEntries in FVDL Vulnerability.
 * Populates auxiliaryData and externalEntries in the custom Vulnerability model.
 */
public class AuxiliaryProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AuxiliaryProcessor.class);

    /**
     * Processes AuxiliaryData and ExternalEntries for a vulnerability.
     *
     * @param vulnJAXB JAXB Vulnerability object
     * @param vulnCustom Custom Vulnerability model to populate
     */
    public void process(Vulnerability vulnJAXB, com.fortify.cli.aviator.fpr.Vulnerability vulnCustom) {
        // Process AuxiliaryData
        List<Map<String, String>> auxData = new ArrayList<>();
        for (AuxiliaryData aux : vulnJAXB.getAuxiliaryData()) {
            Map<String, String> auxMap = new HashMap<>();
            auxMap.put("contentType", aux.getContentType() != null ? aux.getContentType() : "");
            if (aux.getAuxField() != null) {
                for (AuxiliaryData.AuxField field : aux.getAuxField()) {
                    auxMap.put(field.getName(), field.getValue() != null ? field.getValue() : "");
                    SourceLocationType loc = field.getSourceLocation();
                    if (loc != null) {
                        auxMap.put("locPath", loc.getPath() != null ? loc.getPath() : "");
                        auxMap.put("locLine", loc.getLine() != null ? loc.getLine().toString() : "0");
                        auxMap.put("locColStart", loc.getColStart() != null ? loc.getColStart().toString() : "0");
                        auxMap.put("locColEnd", loc.getColEnd() != null ? loc.getColEnd().toString() : "0");
                    }
                }
            }
            auxData.add(auxMap);
        }
        vulnCustom.setAuxiliaryData(auxData);

        // Process ExternalEntries
        ExternalEntries externalEntriesJAXB = vulnJAXB.getExternalEntries();
        List<com.fortify.cli.aviator.fpr.model.Entry> entries = new ArrayList<>();
        if (externalEntriesJAXB != null) {
            for (Entry entryJAXB : externalEntriesJAXB.getEntry()) {
                com.fortify.cli.aviator.fpr.model.Entry customEntry = new com.fortify.cli.aviator.fpr.model.Entry();
                customEntry.setUrl(entryJAXB.getURL() != null ? entryJAXB.getURL() : "");
                customEntry.setFunction(entryJAXB.getFunction()); // Function object
                customEntry.setLocation(entryJAXB.getSourceLocation());

                // Map to custom fields
                List<com.fortify.cli.aviator.fpr.model.Entry.Field> customFields = new ArrayList<>();
                if (entryJAXB.getFields() != null && entryJAXB.getFields().getField() != null) {
                    for (Field fieldJAXB : entryJAXB.getFields().getField()) {
                        com.fortify.cli.aviator.fpr.model.Entry.Field customField = new com.fortify.cli.aviator.fpr.model.Entry.Field();
                        customField.setName(fieldJAXB.getName() != null ? fieldJAXB.getName() : "");
                        customField.setValue(fieldJAXB.getValue() != null ? fieldJAXB.getValue() : "");
                        customField.setType(fieldJAXB.getType() != null ? fieldJAXB.getType() : "");
                        customField.setVulnTag(fieldJAXB.getVulnTag() != null ? fieldJAXB.getVulnTag() : "");
                        customFields.add(customField);
                    }
                }
                customEntry.setFields(customFields);
                entries.add(customEntry);
            }
        }
        vulnCustom.setExternalEntries(entries);

        // Process ExternalID (add to knowledge)
        for (ExternalID externalID : vulnJAXB.getExternalID()) {
            vulnCustom.getKnowledge().put("externalID." + externalID.getName(), externalID.getValue() != null ? externalID.getValue() : "");
        }
    }
}