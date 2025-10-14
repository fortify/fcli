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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.FcliDataHelper;
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
    private static final Logger LOG = LoggerFactory.getLogger(ToolInstallationDescriptor.class);
    private String installDir;
    private String binDir;
    private String globalBinDir;
    
    public ToolInstallationDescriptor(Path installPath, Path binPath, Path globalBinPath) {
        this.installDir = installPath==null ? null : installPath.toAbsolutePath().normalize().toString();
        this.binDir = binPath==null ? null : binPath.toAbsolutePath().normalize().toString();
        this.globalBinDir = globalBinPath==null ? null : globalBinPath.toAbsolutePath().normalize().toString();
    }
    
    public static final ToolInstallationDescriptor optionalCopyFromToolInstallPath(Path toolInstallPath, String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        var installDescriptor = load(toolName, versionDescriptor);
        if ( installDescriptor!=null ) { return installDescriptor; }
        try {
            var installDescriptorToolCopyPath = getInstallDescriptorToolCopyPath(toolInstallPath, toolName, versionDescriptor);
            if ( Files.exists(installDescriptorToolCopyPath) ) {
                var descriptorFromToolInstallPath = JsonHelper.jsonStringToValue(Files.readString(installDescriptorToolCopyPath, StandardCharsets.UTF_8), ToolInstallationDescriptor.class);
                descriptorFromToolInstallPath.save(toolName, versionDescriptor);
                return descriptorFromToolInstallPath;
            }
        } catch ( Exception ignore ) {}
        return null;
    }
    
    public static final ToolInstallationDescriptor load(String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        var result = FcliDataHelper.readFile(getInstallDescriptorPath(toolName, versionDescriptor.getVersion()), ToolInstallationDescriptor.class, false);
        // Check for stale descriptor
        if ( result!=null && !Files.exists(result.getInstallPath()) ) {
            delete(toolName, versionDescriptor);
            result = null;
        }
        return result;
    }
    
    public static final ToolInstallationDescriptor loadLastModified(String toolName) {
        var installDescriptorsDir = getInstallDescriptorsDirPath(toolName).toFile();
        var descriptorFiles = installDescriptorsDir.listFiles(File::isFile);
        if ( descriptorFiles!=null ) {
            Optional<File> lastModifiedFile = Arrays.stream(descriptorFiles)
                    .max((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            return lastModifiedFile.map(File::toPath)
                    .map(ToolInstallationDescriptor::load)
                    // The load method may delete stale descriptors, in which case we need to look for the next one
                    .orElseGet(()->loadLastModified(toolName));
        }
        return null;
    }
    
    public static final void delete(String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        delete(getInstallDescriptorPath(toolName, versionDescriptor.getVersion()));
    }
    
    public final void save(String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        Path installDescriptorPath = getInstallDescriptorPath(toolName, versionDescriptor.getVersion());
        FcliDataHelper.saveFile(installDescriptorPath, this, true);
        copyInstallDescriptorToToolPath(installDescriptorPath, toolName, versionDescriptor);
    }

    private void copyInstallDescriptorToToolPath(Path installDescriptorPath, String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        try {
            var installDescriptorToolCopyPath = getInstallDescriptorToolCopyPath(getInstallPath(), toolName, versionDescriptor);
            Files.createDirectories(installDescriptorToolCopyPath.getParent());
            Files.copy(installDescriptorPath, installDescriptorToolCopyPath, StandardCopyOption.REPLACE_EXISTING);
        } catch ( IOException ioe ) {
            LOG.warn("WARN: Unable to copy tool installation manifest to tool installation directory");
            LOG.debug("Exception details", ioe);
        }
    }
    
    public Path getInstallPath() {
        return asPath(installDir);
    }
    
    public Path getBinPath() {
        return asPath(binDir);
    }
    
    public Path getGlobalBinPath() {
        return asPath(binDir);
    }
    
    private static final ToolInstallationDescriptor load(Path descriptorPath) {
        var result = FcliDataHelper.readFile(descriptorPath, ToolInstallationDescriptor.class, false);
        // Check for stale descriptor
        if ( result!=null && !Files.exists(result.getInstallPath()) ) {
            delete(descriptorPath);
            result = null;
        }
        return result;
    }
    
    private static final void delete(Path descriptorPath) {
        FcliDataHelper.deleteFile(descriptorPath, true);
    }
    
    private static final Path asPath(String dir) {
        return StringUtils.isNotBlank(dir) ? Paths.get(dir) : null;
    }
    
    private static final Path getInstallDescriptorPath(String toolName, String version) {
        return getInstallDescriptorsDirPath(toolName).resolve(version);
    }

    private static final Path getInstallDescriptorsDirPath(String toolName) {
        return ToolInstallationHelper.getToolsStatePath().resolve(toolName);
    }
    
    private static final Path getInstallDescriptorToolCopyPath(Path installPath, String toolName, ToolDefinitionVersionDescriptor versionDescriptor) {
        return installPath.resolve("install-descriptor").resolve(toolName).resolve(versionDescriptor.getVersion());
    }

}
