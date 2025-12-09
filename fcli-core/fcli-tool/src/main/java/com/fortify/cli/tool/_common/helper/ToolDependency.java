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
package com.fortify.cli.tool._common.helper;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;

/**
 * Enumeration of tool dependencies that have definitions but are not exposed
 * as user-facing CLI commands. These are typically internal dependencies used
 * by other tools (e.g., JRE required by ScanCentral Client).
 * 
 * Unlike the Tool enum, ToolDependency entries do not have associated CLI commands
 * and do not define binary names or environment variable prefixes.
 * 
 * @author Ruud Senden
 */
@RequiredArgsConstructor
public enum ToolDependency {
    JRE(new ToolDependencyHelperJre());
    
    private static final Map<String, ToolDependency> TOOL_DEPENDENCY_NAME_MAP = new HashMap<>();
    
    static {
        for (ToolDependency dependency : values()) {
            TOOL_DEPENDENCY_NAME_MAP.put(dependency.getToolName(), dependency);
        }
    }
    
    /**
     * Get the ToolDependency enum entry by tool name.
     * @param toolName the tool name (e.g., "jre")
     * @return the corresponding ToolDependency enum entry, or null if not found
     */
    public static ToolDependency getByToolName(String toolName) {
        return TOOL_DEPENDENCY_NAME_MAP.get(toolName);
    }
    
    private final IToolDependencyHelper toolDependencyHelper;
    
    /**
     * Get the tool name identifier (e.g., "jre").
     */
    public String getToolName() {
        return toolDependencyHelper.getToolName();
    }
    
    /**
     * Interface defining tool dependency-specific helper methods.
     * Each tool dependency implementation provides its own concrete helper class.
     */
    public interface IToolDependencyHelper {
        String getToolName();
    }
    
    /**
     * Helper implementation for jre tool dependency.
     */
    private static final class ToolDependencyHelperJre implements IToolDependencyHelper {
        private static final String TOOL_NAME = "jre";
        
        @Override
        public String getToolName() {
            return TOOL_NAME;
        }
    }
}
