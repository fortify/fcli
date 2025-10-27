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

import com.fortify.cli.aviator.fpr.jaxb.ReplacementDefinitions;
import com.fortify.cli.aviator.fpr.jaxb.SourceLocationType;
import com.fortify.cli.aviator.fpr.model.ReplacementData;

/**
 * Parses the <ReplacementDefinitions> section of a vulnerability's AnalysisInfo
 * and populates the clean ReplacementData model. This class acts as the bridge
 * between the raw JAXB format and the application's internal data structure.
 */
public final class ReplacementParser {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ReplacementParser() {}

    /**
     * Parses the JAXB definitions into a populated ReplacementData object.
     *
     * @param definitions The raw JAXB object from the FVDL, which may be null.
     * @return A non-null, fully populated (or empty) ReplacementData object.
     */
    public static ReplacementData parse(ReplacementDefinitions definitions) {
        ReplacementData data = new ReplacementData();
        if (definitions == null) {
            // Return an empty data object if there are no definitions.
            return data;
        }

        // Process <Def> tags. These tags can contain both a value and a location.
        if (definitions.getDef() != null) {
            for (ReplacementDefinitions.Def def : definitions.getDef()) {
                String key = def.getKey();
                String value = def.getValue();
                SourceLocationType loc = def.getSourceLocation();

                // Safely extract location attributes, providing null if not present.
                String path = (loc != null) ? loc.getPath() : null;
                String line = (loc != null && loc.getLine() != null) ? loc.getLine().toString() : null;
                String colStart = (loc != null && loc.getColStart() != null) ? loc.getColStart().toString() : null;
                String colEnd = (loc != null && loc.getColEnd() != null) ? loc.getColEnd().toString() : null;

                // Use the addReplacement method from your existing ReplacementData class.
                data.addReplacement(key, value, path, line, colStart, colEnd);
            }
        }

        // Process <LocationDef> tags. These tags *only* contain location information.
        if (definitions.getLocationDef() != null) {
            for (ReplacementDefinitions.LocationDef locDef : definitions.getLocationDef()) {
                Map<String, String> attrs = new HashMap<>();
                attrs.put("path", locDef.getPath() != null ? locDef.getPath() : "");
                attrs.put("line", locDef.getLine() != null ? locDef.getLine().toString() : "0");
                attrs.put("colStart", locDef.getColStart() != null ? locDef.getColStart().toString() : "0");
                attrs.put("colEnd", locDef.getColEnd() != null ? locDef.getColEnd().toString() : "0");

                // Use the addLocationReplacement method from your existing ReplacementData class.
                data.addLocationReplacement(locDef.getKey(), attrs);
            }
        }

        return data;
    }
}