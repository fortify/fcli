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

import com.fortify.cli.common.util.PlatformHelper;

/**
 * Enumeration of all supported tools with their metadata and helper implementations.
 * Provides centralized tool configuration including names, binary names, and environment prefixes.
 * 
 * @author Ruud Senden
 */
public enum Tool {
    FCLI(new ToolHelperFcli(), "fcli"),
    SC_CLIENT(new ToolHelperSCClient(), "sc-client", "scancentral-client"),
    FOD_UPLOADER(new ToolHelperFoDUploader(), "fod-uploader"),
    BUGTRACKER_UTILITY(new ToolHelperBugTrackerUtility(), "bugtracker-utility", "fbtu"),
    VULN_EXPORTER(new ToolHelperVulnExporter(), "vuln-exporter", "fve"),
    DEBRICKED_CLI(new ToolHelperDebrickedCli(), "debricked-cli", "dcli");
    
    private static final Map<String, Tool> TOOL_NAME_MAP = new HashMap<>();
    private static final Map<String, Tool> TOOL_ALIAS_MAP = new HashMap<>();
    
    static {
        for (Tool tool : values()) {
            TOOL_NAME_MAP.put(tool.getToolName(), tool);
            for (String alias : tool.aliases) {
                TOOL_ALIAS_MAP.put(alias, tool);
            }
        }
    }
    
    private final IToolHelper toolHelper;
    private final String[] aliases;
    
    Tool(IToolHelper toolHelper, String... aliases) {
        this.toolHelper = toolHelper;
        this.aliases = aliases;
    }
    
    /**
     * Get the Tool enum entry by tool name.
     * @param toolName the tool name (e.g., "fcli", "sc-client")
     * @return the corresponding Tool enum entry, or null if not found
     */
    public static Tool getByToolName(String toolName) {
        return TOOL_NAME_MAP.get(toolName);
    }
    
    /**
     * Get the Tool enum entry by tool name or alias.
     * @param nameOrAlias the tool name or alias (e.g., "fcli", "dcli", "scancentral-client")
     * @return the corresponding Tool enum entry, or null if not found
     */
    public static Tool getByToolNameOrAlias(String nameOrAlias) {
        Tool tool = TOOL_NAME_MAP.get(nameOrAlias);
        if (tool == null) {
            tool = TOOL_ALIAS_MAP.get(nameOrAlias);
        }
        return tool;
    }
    
    /**
     * Get all aliases for this tool (includes the canonical name).
     * @return array of all aliases including the tool name
     */
    public String[] getAliases() {
        return aliases.clone();
    }
    
    /**
     * Get the tool name identifier (e.g., "fcli", "sc-client").
     */
    public String getToolName() {
        return toolHelper.getToolName();
    }
    
    /**
     * Get the platform-specific default binary name.
     */
    public String getDefaultBinaryName() {
        return toolHelper.getDefaultBinaryName();
    }
    
    /**
     * Get the default environment variable prefix for this tool.
     */
    public String getDefaultEnvPrefix() {
        return toolHelper.getDefaultEnvPrefix();
    }
    
    /**
     * Interface defining tool-specific helper methods.
     * Each tool implementation provides its own concrete helper class.
     */
    public interface IToolHelper {
        String getToolName();
        String getDefaultBinaryName();
        
        default String getDefaultEnvPrefix() {
            return getToolName().toUpperCase().replace('-', '_');
        }
    }
    
    /**
     * Helper implementation for fcli tool.
     */
    private static final class ToolHelperFcli implements IToolHelper {
        private static final String TOOL_NAME = "fcli";
        
        @Override
        public String getToolName() {
            return TOOL_NAME;
        }
        
        @Override
        public String getDefaultBinaryName() {
            return PlatformHelper.isWindows() ? "fcli.exe" : "fcli";
        }
    }
    
    /**
     * Helper implementation for sc-client tool.
     */
    private static final class ToolHelperSCClient implements IToolHelper {
        private static final String TOOL_NAME = "sc-client";
        
        @Override
        public String getToolName() {
            return TOOL_NAME;
        }
        
        @Override
        public String getDefaultBinaryName() {
            return PlatformHelper.isWindows() ? "scancentral.bat" : "scancentral";
        }
    }
    
    /**
     * Helper implementation for fod-uploader tool.
     */
    private static final class ToolHelperFoDUploader implements IToolHelper {
        private static final String TOOL_NAME = "fod-uploader";
        
        @Override
        public String getToolName() {
            return TOOL_NAME;
        }
        
        @Override
        public String getDefaultBinaryName() {
            return PlatformHelper.isWindows() ? "FoDUpload.bat" : "FoDUpload";
        }
    }
    
    /**
     * Helper implementation for bugtracker-utility tool.
     */
    private static final class ToolHelperBugTrackerUtility implements IToolHelper {
        private static final String TOOL_NAME = "bugtracker-utility";
        
        @Override
        public String getToolName() {
            return TOOL_NAME;
        }
        
        @Override
        public String getDefaultBinaryName() {
            return PlatformHelper.isWindows() ? "FortifyBugTrackerUtility.bat" : "FortifyBugTrackerUtility";
        }
        
        @Override
        public String getDefaultEnvPrefix() {
            return "FBTU";
        }
    }
    
    /**
     * Helper implementation for vuln-exporter tool.
     */
    private static final class ToolHelperVulnExporter implements IToolHelper {
        private static final String TOOL_NAME = "vuln-exporter";
        
        @Override
        public String getToolName() {
            return TOOL_NAME;
        }
        
        @Override
        public String getDefaultBinaryName() {
            return PlatformHelper.isWindows() ? "FortifyVulnerabilityExporter.bat" : "FortifyVulnerabilityExporter";
        }
        
        @Override
        public String getDefaultEnvPrefix() {
            return "FVE";
        }
    }
    
    /**
     * Helper implementation for debricked-cli tool.
     */
    private static final class ToolHelperDebrickedCli implements IToolHelper {
        private static final String TOOL_NAME = "debricked-cli";
        
        @Override
        public String getToolName() {
            return TOOL_NAME;
        }
        
        @Override
        public String getDefaultBinaryName() {
            return PlatformHelper.isWindows() ? "debricked.exe" : "debricked";
        }
        
        @Override
        public String getDefaultEnvPrefix() {
            return "DEBRICKED";
        }
    }
}
