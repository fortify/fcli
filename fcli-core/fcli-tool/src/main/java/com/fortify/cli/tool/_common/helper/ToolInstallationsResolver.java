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
package com.fortify.cli.tool._common.helper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.tool.definitions.helper.ToolDefinitionRootDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

/**
 * Helper for resolving tool installation descriptors together with their
 * corresponding definition descriptors. This consolidates logic that was
 * previously duplicated across list/run commands and will be shared with
 * the new env command hierarchy.
 */
public final class ToolInstallationsResolver {
    private ToolInstallationsResolver() {}

    public static ToolInstallations resolve(Tool tool) {
        var toolName = tool.getToolName();
        var definition = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName);
        var lastInstalled = ToolInstallationDescriptor.loadLastModified(toolName);
        var definedRecords = definition.getVersionsStream()
                .map(vd -> createRecord(toolName, vd, lastInstalled, true));
        var unknownRecords = getUnknownRecords(toolName, definition, lastInstalled);
        var records = Stream.concat(definedRecords, unknownRecords)
                .toList();
        return new ToolInstallations(tool, definition, lastInstalled, records);
    }

    private static Stream<ToolInstallationRecord> getUnknownRecords(String toolName,
            ToolDefinitionRootDescriptor definition,
            ToolInstallationDescriptor lastInstalled) {
        Path stateDir = ToolInstallationHelper.getToolsStatePath().resolve(toolName);
        if (!Files.isDirectory(stateDir)) {
            return Stream.empty();
        }
        File[] versionFiles = stateDir.toFile().listFiles(File::isFile);
        if (versionFiles == null || versionFiles.length == 0) {
            return Stream.empty();
        }
        return Stream.of(versionFiles)
                .map(File::getName)
                .filter(version -> isUnknownVersion(version, toolName, definition))
                .map(version -> createRecord(toolName,
                        createSyntheticDescriptor(definition, version),
                        lastInstalled,
                        false));
    }

    private static boolean isUnknownVersion(String versionFileName, String toolName,
            ToolDefinitionRootDescriptor definition) {
        if (versionFileName.equals(toolName)) {
            return false;
        }
        // Special handling for "unknown" version - don't try to look it up in definitions
        if ("unknown".equals(versionFileName)) {
            return true;
        }
        if (definition.getOptionalVersion(versionFileName).isPresent()) {
            return false;
        }
        var normalized = definition.normalizeVersionFormat(versionFileName);
        // Also check if normalized version is "unknown"
        if ("unknown".equals(normalized)) {
            return true;
        }
        return definition.getOptionalVersion(normalized).isEmpty();
    }

    private static ToolInstallationRecord createRecord(String toolName,
            ToolDefinitionVersionDescriptor versionDescriptor,
            ToolInstallationDescriptor lastInstalled,
            boolean knownVersion) {
        var installationDescriptor = ToolInstallationDescriptor.load(toolName, versionDescriptor);
        boolean isDefault = isDefault(installationDescriptor, lastInstalled);
        return new ToolInstallationRecord(versionDescriptor, installationDescriptor, isDefault, knownVersion);
    }

    private static boolean isDefault(ToolInstallationDescriptor descriptor,
            ToolInstallationDescriptor lastInstalled) {
        if (descriptor == null || lastInstalled == null) {
            return false;
        }
        return StringUtils.isNotBlank(descriptor.getInstallDir())
                && descriptor.getInstallDir().equals(lastInstalled.getInstallDir());
    }

    private static ToolDefinitionVersionDescriptor createSyntheticDescriptor(
            ToolDefinitionRootDescriptor definition,
            String version) {
        String normalizedVersion = definition.normalizeVersionFormat(version);
        var descriptor = new ToolDefinitionVersionDescriptor();
        descriptor.setVersion(normalizedVersion);
        descriptor.setStable(false);
        descriptor.setAliases(new String[0]);
        return descriptor;
    }

    public static record ToolInstallations(
            Tool tool,
            ToolDefinitionRootDescriptor definition,
            ToolInstallationDescriptor lastInstalled,
            List<ToolInstallationRecord> records) {
        public Stream<ToolInstallationRecord> stream() {
            return records.stream();
        }
        public Stream<ToolInstallationRecord> installedStream() {
            return records.stream().filter(ToolInstallationRecord::isInstalled);
        }
        public Optional<ToolInstallationRecord> defaultInstallation() {
            return installedStream().filter(ToolInstallationRecord::isDefault).findFirst();
        }
        public Optional<ToolInstallationRecord> findByVersion(String version) {
            return records.stream()
                    .filter(record -> record.versionDescriptor().getVersion().equals(version))
                    .findFirst();
        }
    }

    public static record ToolInstallationRecord(
            ToolDefinitionVersionDescriptor versionDescriptor,
            ToolInstallationDescriptor installationDescriptor,
            boolean isDefault,
            boolean knownVersion) {
        public boolean isInstalled() {
            return installationDescriptor != null && StringUtils.isNotBlank(installationDescriptor.getInstallDir());
        }
    }
}
