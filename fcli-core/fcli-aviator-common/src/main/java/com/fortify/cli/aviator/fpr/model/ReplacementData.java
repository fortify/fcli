package com.fortify.cli.aviator.fpr.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Data structure for ReplacementDefinitions from FVDL AnalysisInfo.
 * Holds maps for regular defs and location defs.
 */
@Getter
@Setter
public class ReplacementData {
    // Map for <Def key="..." value="..." [SourceLocation attrs]>
    private final Map<String, Replacement> replacements = new HashMap<>();

    // Map for <LocationDef key="..." path="..." line="..." colStart="..." colEnd="...">
    private final Map<String, Map<String, String>> locationReplacements = new HashMap<>();

    /**
     * Adds a replacement from Def (value + optional location attrs).
     *
     * @param key Key attribute
     * @param value Value attribute (optional)
     * @param path Path from SourceLocation (optional)
     * @param line Line from SourceLocation (optional)
     * @param colStart ColStart from SourceLocation (optional)
     * @param colEnd ColEnd from SourceLocation (optional)
     */
    public void addReplacement(String key, String value, String path, String line, String colStart, String colEnd) {
        replacements.put(key, new Replacement(value, path, line, colStart, colEnd));
    }

    /**
     * Adds a location def (attrs map).
     *
     * @param key Key attribute
     * @param attrs Map of path/line/colStart/colEnd
     */
    public void addLocationReplacement(String key, Map<String, String> attrs) {
        locationReplacements.put(key, attrs);
    }

    /**
     * Inner class for a single replacement (value + location).
     */
    @Getter
    @Setter
    @AllArgsConstructor
    public static class Replacement {
        private String value;
        private String path;
        private String line;
        private String colStart;
        private String colEnd;

        /**
         * Checks if this replacement has a location.
         *
         * @return true if path/line/colStart/colEnd are all set
         */
        public boolean hasLocation() {
            return path != null && line != null && colStart != null && colEnd != null;
        }
    }
}