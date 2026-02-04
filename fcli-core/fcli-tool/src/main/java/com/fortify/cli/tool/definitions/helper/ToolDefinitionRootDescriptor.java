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
package com.fortify.cli.tool.definitions.helper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliSimpleException;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class represents the contents of a tool definition YAML file, usually
 * deserialized from [tool-name].yaml loaded from tool-definitions.yaml.zip. 
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@Reflectable @NoArgsConstructor
@Data
public class ToolDefinitionRootDescriptor {
    private String schema_version;
    private ToolDefinitionVersionDescriptor[] versions;
    
    public final ToolDefinitionVersionDescriptor[] getVersions() {
        return getVersionsStream().toArray(ToolDefinitionVersionDescriptor[]::new);
    }
    
    public final Stream<ToolDefinitionVersionDescriptor> getVersionsStream() {
        return Stream.of(versions);
    }
    
    public final ToolDefinitionVersionDescriptor getVersion(String versionOrAlias) {
        return getOptionalVersion(versionOrAlias)
                .orElseThrow(() -> new FcliSimpleException("Version or alias "+versionOrAlias+" not found"));
    }

    public final Optional<ToolDefinitionVersionDescriptor> getOptionalVersion(String versionOrAlias) {
        return getVersionsStream()
                .filter(v->matches(v, versionOrAlias))
                .findFirst();
    }
    
    public final ToolDefinitionVersionDescriptor getVersionOrDefault(String versionOrAlias) {
        return getVersion(normalizeVersionOrAliasForDefault(versionOrAlias));
    }
    
    public final Optional<ToolDefinitionVersionDescriptor> getOptionalVersionOrDefault(String versionOrAlias) {
        return getOptionalVersion(normalizeVersionOrAliasForDefault(versionOrAlias));
    }
    
    private static String normalizeVersionOrAliasForDefault(String versionOrAlias) {
        if (StringUtils.isBlank(versionOrAlias) || "default".equals(versionOrAlias)) {
            return "latest";
        }
        return versionOrAlias;
    }
    
    private static final boolean matches(ToolDefinitionVersionDescriptor descriptor, String versionOrAlias) {
        var result = descriptor.getVersion().equals(versionOrAlias) 
                || Arrays.stream(descriptor.getAliases()).anyMatch(versionOrAlias::equals);
        if ( !result && versionOrAlias.startsWith("v") ) {
            result = matches(descriptor, versionOrAlias.replaceFirst("^v", ""));
        }
        return result;
    }
    
    /**
     * Normalize a version string to match the format commonly used in tool definitions.
     * Determines the most common number of version segments (dots) from actual version
     * strings in tool definitions (excluding aliases), then truncates or pads the input
     * version to match that format.
     * 
     * Examples:
     * - If definitions use 3-segment versions (e.g., 24.2.0), normalize 24.2.0.0050 to 24.2.0
     * - If definitions use 2-segment versions (e.g., 1.2), normalize 1.2.3 to 1.2
     * 
     * @param detectedVersion The version string to normalize
     * @return Normalized version string matching tool definition format
     */
    public final String normalizeVersionFormat(String detectedVersion) {
        if (StringUtils.isBlank(detectedVersion) || "unknown".equals(detectedVersion)) {
            return detectedVersion;
        }
        
        int targetSegments = determineVersionFormatSegmentCount();
        if (targetSegments <= 0) {
            // No consistent format found, return as-is
            return detectedVersion;
        }
        
        // Split version into segments (ignoring any suffix like -SNAPSHOT)
        String versionBase = detectedVersion;
        String suffix = "";
        int suffixIdx = detectedVersion.indexOf('-');
        if (suffixIdx > 0) {
            versionBase = detectedVersion.substring(0, suffixIdx);
            suffix = detectedVersion.substring(suffixIdx);
        }
        
        String[] segments = versionBase.split("\\.");
        
        if (segments.length == targetSegments) {
            return detectedVersion; // Already correct format
        }
        
        if (segments.length > targetSegments) {
            // Truncate to target segment count
            return String.join(".", Arrays.copyOf(segments, targetSegments)) + suffix;
        } else {
            // Pad with zeros to reach target segment count
            String[] paddedSegments = Arrays.copyOf(segments, targetSegments);
            for (int i = segments.length; i < targetSegments; i++) {
                paddedSegments[i] = "0";
            }
            return String.join(".", paddedSegments) + suffix;
        }
    }
    
    /**
     * Determine the most common number of version segments (dots + 1) used in tool definitions.
     * Only considers actual version strings, not aliases.
     * 
     * @return Number of segments in the most common version format, or -1 if no pattern found
     */
    private int determineVersionFormatSegmentCount() {
        if (versions == null || versions.length == 0) {
            return -1;
        }
        
        // Count occurrences of each segment count
        var segmentCounts = new HashMap<Integer, Integer>();
        
        for (ToolDefinitionVersionDescriptor descriptor : versions) {
            String version = descriptor.getVersion();
            if (StringUtils.isNotBlank(version)) {
                // Extract base version (before any suffix like -SNAPSHOT)
                String versionBase = version;
                int suffixIdx = version.indexOf('-');
                if (suffixIdx > 0) {
                    versionBase = version.substring(0, suffixIdx);
                }
                
                // Count dots + 1 to get segment count
                int segments = versionBase.split("\\.").length;
                segmentCounts.merge(segments, 1, Integer::sum);
            }
        }
        
        if (segmentCounts.isEmpty()) {
            return -1;
        }
        
        // Return the most common segment count
        return segmentCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(-1);
    }
    
}