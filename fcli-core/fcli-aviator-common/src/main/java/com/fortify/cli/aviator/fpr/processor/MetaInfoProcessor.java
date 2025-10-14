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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.fpr.jaxb.EngineData;
import com.fortify.cli.aviator.fpr.jaxb.MetaInfo;

/**
 * Processor for extracting and caching ALL meta information from FVDL EngineData/RuleInfo.
 * This class now stores a complete map of every <Group> tag for each rule,
 * enabling filtering on custom tags like OWASP, PCI, etc.
 */
public class MetaInfoProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MetaInfoProcessor.class);
    // The value is now a Map<String, String> to hold all <Group> name/value pairs for a given rule ID.
    private final Map<String, Map<String, String>> ruleMetadataCache = new ConcurrentHashMap<>();

    /**
     * Processes the <EngineData> section of the FVDL to build the metadata cache.
     * @param engineData The JAXB EngineData object.
     */
    public void process(EngineData engineData) {
        if (engineData == null || engineData.getRuleInfo() == null) {
            logger.debug("No RuleInfo found in EngineData, metadata cache will be empty.");
            return;
        }

        for (EngineData.RuleInfo.Rule rule : engineData.getRuleInfo().getRule()) {
            String ruleId = rule.getId();
            if (ruleId == null || ruleId.isEmpty()) {
                logger.warn("Found a rule with a missing or empty ID, skipping.");
                continue;
            }

            Map<String, String> metadata = new HashMap<>();
            MetaInfo metaInfoElem = rule.getMetaInfo();
            if (metaInfoElem != null && metaInfoElem.getGroup() != null) {
                for (MetaInfo.Group group : metaInfoElem.getGroup()) {
                    // Store every group, using the group's name as the key.
                    // This will capture "Accuracy", "Impact", "altcategoryOWASP2021", etc.
                    String groupName = group.getName();
                    if (groupName != null && group.getValue() != null) {
                        metadata.put(groupName, group.getValue().trim());
                    }
                }
            }
            ruleMetadataCache.put(ruleId, metadata);
        }
        logger.debug("Processed {} rules into the metadata cache.", ruleMetadataCache.size());
    }

    /**
     * Gets the complete metadata map for a given rule ID.
     *
     * @param ruleId The ID of the rule.
     * @return A map of all metadata for that rule, or an empty map if not found.
     */
    public Map<String, String> getMetadataForRule(String ruleId) {
        return ruleMetadataCache.getOrDefault(ruleId, new HashMap<>());
    }
}