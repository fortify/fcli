/**
 * Copyright 2023 Open Text.
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

import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.common.util.SemVer;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;

import lombok.Data;

@Data
public class ToolUninstaller {
    private static final Logger LOG = LoggerFactory.getLogger(ToolUninstaller.class);
    private static final Path deleteOnStartPath = ToolInstallationHelper.getToolsStatePath().resolve("delete-on-start.json");
    private final String toolName;
    
    public final ToolInstallationOutputDescriptor uninstall(ToolDefinitionVersionDescriptor versionDescriptor, ToolInstallationDescriptor installationDescriptor) {
        return uninstall(versionDescriptor, installationDescriptor, null);
    }
    
    // TODO Remove/update global bin script to point to latest remaining installation?
    public final ToolInstallationOutputDescriptor uninstall(ToolDefinitionVersionDescriptor versionDescriptor, ToolInstallationDescriptor installationDescriptor, ToolDefinitionVersionDescriptor replacementVersion) {
        var installPath = installationDescriptor.getInstallPath();
        var action = "UNINSTALLED";
        if ( !FileUtils.isDirPathInUse(installPath) ) {
            FileUtils.deleteRecursive(installPath);
        } else if (replacementVersion==null || new SemVer(replacementVersion.getVersion()).compareTo("2.2.0")<0 ) {
            action = "MANUAL_DELETE_REQUIRED";
        } else {
            action = "PENDING_FCLI_RESTART";
            savePendingDelete(installPath);
        }
        ToolInstallationDescriptor.delete(toolName, versionDescriptor);
        return new ToolInstallationOutputDescriptor(toolName, versionDescriptor, installationDescriptor, action);
    }

    private void savePendingDelete(Path installPath) {
        var deleteOnStartArray = FcliDataHelper.readFile(deleteOnStartPath, ArrayNode.class, false);
        if ( deleteOnStartArray==null ) {
            deleteOnStartArray = JsonHelper.getObjectMapper().createArrayNode();
        }
        deleteOnStartArray.add(installPath.toAbsolutePath().toString());
        FcliDataHelper.saveFile(deleteOnStartPath, deleteOnStartArray, false);
    }
    
    public static final void deleteAllPending() {
        var deleteOnStartArray = FcliDataHelper.readFile(deleteOnStartPath, ArrayNode.class, false);
        if ( deleteOnStartArray!=null ) {
            var failingDirsArray = JsonHelper.stream(deleteOnStartArray)
                    .map(ToolUninstaller::deletePending)
                    .filter(Objects::nonNull)
                    .collect(JsonHelper.arrayNodeCollector());
            if ( failingDirsArray.isEmpty() ) {
                FcliDataHelper.deleteFile(deleteOnStartPath, true);
            } else {
                FcliDataHelper.saveFile(deleteOnStartPath, failingDirsArray, true);
            }
        }
    }
    
    public static final JsonNode deletePending(JsonNode dirNode) {
        var dirPath = Path.of(dirNode.asText());
        if ( !FileUtils.isDirPathInUse(dirPath) ) {
            try {
                FileUtils.deleteRecursive(dirPath);
            } catch ( Exception e ) {
                LOG.warn("WARN: Error on pending delete; please delete manually: "+dirPath);
            }
            return null;
        }
        return dirNode;
    }
    
    
}
