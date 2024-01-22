/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.tool._common.helper;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.FcliDataHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class represents a single tool installation, containing information about the 
 * installation location. It doesn't include the actual tool name or version, as this 
 * is represented by the directory name (tool name) and file name (version) where the 
 * serialized installation descriptors are stored. 
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@Reflectable @NoArgsConstructor @AllArgsConstructor
@Data
public class ToolInstallationDescriptor {
    private String installDir;
    private String binDir;
    
    public ToolInstallationDescriptor(Path installPath, Path binPath) {
        this.installDir = installPath.toAbsolutePath().toString();
        this.binDir = binPath.toAbsolutePath().toString();
    }
    
    public static final ToolInstallationDescriptor load(String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        return FcliDataHelper.readFile(getInstallDescriptorPath(toolName, versionDescriptor.getVersion()), ToolInstallationDescriptor.class, false);
    }
    
    public static final void delete(String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        FcliDataHelper.deleteFile(getInstallDescriptorPath(toolName, versionDescriptor.getVersion()), true);
    }
    
    public final void save(String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        FcliDataHelper.saveFile(getInstallDescriptorPath(toolName, versionDescriptor.getVersion()), this, true);
    }
    
    public Path getInstallPath() {
        return asPath(installDir);
    }
    
    public Path getBinPath() {
        return asPath(binDir);
    }
    
    private static final Path asPath(String dir) {
        return StringUtils.isNotBlank(dir) ? Paths.get(dir) : null;
    }
    
    private static final Path getInstallDescriptorPath(String toolName, String version) {
        return getInstallDescriptorsDirPath(toolName).resolve(version);
    }

    private static Path getInstallDescriptorsDirPath(String toolName) {
        return FcliDataHelper.getFcliStatePath().resolve("tools").resolve(toolName);
    }

}
