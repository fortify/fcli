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
package com.fortify.cli.tool.env.cli.mixin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstallationsResolver;
import com.fortify.cli.tool._common.helper.ToolInstallationsResolver.ToolInstallationRecord;
import com.fortify.cli.tool.env.helper.ToolEnvContext;

import picocli.CommandLine.Option;

/**
 * Mixin that owns the {@code --tools} option and all tool-selection and context-building
 * logic.  Commands call {@link #resolveToolEnvContexts()} to get the resolved list.
 */
public class ToolEnvToolSelectorMixin {
    @Option(names = "--tools", split = ",", descriptionKey = "fcli.tool.env.tools")
    private List<String> toolSelectors;

    /** Resolves tool installations into {@link ToolEnvContext} objects, throwing if none found. */
    public List<ToolEnvContext> resolveToolEnvContexts() {
        List<ToolEnvContext> contexts = resolveContexts();
        if (contexts.isEmpty()) {
            throw new FcliSimpleException("No matching tool installations detected");
        }
        return contexts;
    }

    private List<ToolEnvContext> resolveContexts() {
        if (toolSelectors == null || toolSelectors.isEmpty()) {
            return resolveDefaultContexts();
        }
        List<ToolEnvContext> contexts = new ArrayList<>();
        for (String selector : toolSelectors) {
            contexts.addAll(resolveSelector(selector));
        }
        return contexts;
    }

    private List<ToolEnvContext> resolveDefaultContexts() {
        return Arrays.stream(Tool.values())
                .map(ToolInstallationsResolver::resolve)
                .map(installations -> installations.defaultInstallation()
                        .map(record -> toContext(installations.tool(), record)))
                .flatMap(Optional::stream)
                .toList();
    }

    private List<ToolEnvContext> resolveSelector(String selector) {
        String normalized = StringUtils.defaultString(selector).trim();
        if (normalized.isEmpty()) {
            throw new FcliSimpleException("Tool selector may not be blank");
        }
        ToolSelector parsed = parseSelector(normalized);
        var installations = ToolInstallationsResolver.resolve(parsed.tool());
        List<ToolEnvContext> contexts = selectContexts(parsed, installations);
        if (contexts.isEmpty()) {
            throw new FcliSimpleException(String.format(
                    "No tool installation found for %s", formatSelector(parsed)));
        }
        return contexts;
    }

    private List<ToolEnvContext> selectContexts(ToolSelector selector, ToolInstallationsResolver.ToolInstallations installations) {
        if (StringUtils.isBlank(selector.versionSpec())) {
            return installations.defaultInstallation()
                    .map(record -> List.of(toContext(selector.tool(), record)))
                    .orElse(List.of());
        }
        String versionSpec = selector.versionSpec();
        if ("default".equalsIgnoreCase(versionSpec) || "auto".equalsIgnoreCase(versionSpec)) {
            return installations.defaultInstallation()
                    .map(record -> List.of(toContext(selector.tool(), record)))
                    .orElse(List.of());
        }
        if ("*".equals(versionSpec) || "all".equalsIgnoreCase(versionSpec)) {
            return installations.installedStream()
                    .map(record -> toContext(selector.tool(), record))
                    .toList();
        }
        return installations.installedStream()
                .filter(record -> matchesVersion(record, versionSpec))
                .map(record -> toContext(selector.tool(), record))
                .toList();
    }

    private boolean matchesVersion(ToolInstallationRecord record, String versionSpec) {
        String target = StringUtils.trimToEmpty(versionSpec);
        if (target.isEmpty()) {
            return false;
        }
        var versionDescriptor = record.versionDescriptor();
        String version = versionDescriptor.getVersion();
        if (equalsIgnoreCase(version, target) || Arrays.stream(versionDescriptor.getAliases())
                .anyMatch(alias -> equalsIgnoreCase(alias, target))) {
            return true;
        }
        return matchesVersionParts(removeVersionPrefix(version), removeVersionPrefix(target));
    }

    private static boolean matchesVersionParts(String version, String spec) {
        String baseVersion = StringUtils.substringBefore(version, "-");
        String[] versionParts = baseVersion.split("\\.");
        String[] specParts = spec.split("\\.");
        for (int i = 0; i < specParts.length; i++) {
            String part = specParts[i];
            if ("*".equals(part)) {
                continue;
            }
            String versionPart = i < versionParts.length ? versionParts[i] : "";
            if (!equalsIgnoreCase(versionPart, part)) {
                return false;
            }
        }
        return true;
    }

    private static boolean equalsIgnoreCase(String first, String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }

    private static String removeVersionPrefix(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        return first == 'v' || first == 'V' ? value.substring(1) : value;
    }

    private ToolEnvContext toContext(Tool tool, ToolInstallationRecord record) {
        ToolInstallationDescriptor descriptor = record.installationDescriptor();
        if (descriptor == null) {
            throw new FcliSimpleException(String.format(
                    "Tool %s version %s is not installed", tool.getToolName(),
                    record.versionDescriptor().getVersion()));
        }
        ObjectNode model = JsonHelper.getObjectMapper().createObjectNode();
        model.put("toolName", tool.getToolName());
        model.put("installDir", descriptor.getInstallDir());
        model.put("binDir", descriptor.getBinDir());
        model.put("globalBinDir", descriptor.getGlobalBinDir());
        model.put("cmd", computeCommand(tool, descriptor));
        model.put("version", record.versionDescriptor().getVersion());
        model.put("defaultEnvPrefix", tool.getDefaultEnvPrefix());
        model.put("isDefault", record.isDefault());
        ArrayNode aliases = model.putArray("aliases");
        Arrays.stream(record.versionDescriptor().getAliases()).forEach(aliases::add);
        return new ToolEnvContext(tool, record, descriptor, model);
    }

    private static String computeCommand(Tool tool, ToolInstallationDescriptor descriptor) {
        String binDir = descriptor.getBinDir();
        String binaryName = tool.getDefaultBinaryName();
        if (StringUtils.isBlank(binDir) || StringUtils.isBlank(binaryName)) {
            return null;
        }
        Path candidate = Path.of(binDir, binaryName).normalize();
        return Files.exists(candidate) ? candidate.toString() : null;
    }

    private ToolSelector parseSelector(String selector) {
        String[] parts = selector.split(":", 2);
        String toolName = parts[0].trim();
        if (toolName.isEmpty()) {
            throw new FcliSimpleException("Tool name in selector may not be blank");
        }
        Tool tool = resolveTool(toolName);
        String versionSpec = parts.length > 1 ? StringUtils.trimToNull(parts[1]) : null;
        return new ToolSelector(tool, versionSpec);
    }

    private Tool resolveTool(String toolName) {
        Tool exact = Tool.getByToolNameOrAlias(toolName);
        if (exact != null) {
            return exact;
        }
        return Arrays.stream(Tool.values())
                .filter(tool -> tool.getToolName().equalsIgnoreCase(toolName))
                .findFirst()
                .orElseThrow(() -> new FcliSimpleException(String.format("Unknown tool: %s", toolName)));
    }

    private static String formatSelector(ToolSelector selector) {
        if (selector.versionSpec() == null) {
            return selector.tool().getToolName();
        }
        return selector.tool().getToolName() + ":" + selector.versionSpec();
    }

    private record ToolSelector(Tool tool, String versionSpec) {}
}
