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
package com.fortify.cli.util.rpc_server.helper.rpc;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.cli.util.ModuleType;
import com.fortify.cli.common.cli.util.ProductModule;
import com.fortify.cli.common.cli.util.RelatedModules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Model.CommandSpec;

/**
 * RPC method handler for listing available fcli commands.
 * 
 * Method: fcli.listCommands
 * Params:
 * - module (string, optional): Filter by module (e.g., "ssc", "fod")
 * - runnableOnly (boolean, optional): If true, only return runnable (leaf)
 * commands
 * - includeHidden (boolean, optional): If true, include hidden commands
 * 
 * Returns:
 * - commands (array): Array of command descriptors with:
 * - name (string): Qualified command name
 * - module (string): The module this command belongs to
 * - usageHeader (string): Short description
 * - runnable (boolean): Whether the command is executable
 * - hidden (boolean): Whether the command is hidden
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RpcMethodHandlerFcliListCommands implements IRpcMethodHandler {
    private final ObjectMapper objectMapper;

    @Override
    public JsonNode execute(JsonNode params) throws RpcMethodException {
        var moduleParam = params != null && params.has("module")
                ? params.get("module").asText(null)
                : null;
        var modulesOnly = params != null && params.has("modulesOnly")
                && params.get("modulesOnly").asBoolean(false);
        var runnableOnly = params != null && params.has("runnableOnly")
                && params.get("runnableOnly").asBoolean(false);
        var includeHidden = params != null && params.has("includeHidden")
                && params.get("includeHidden").asBoolean(false);
        var moduleTypeParam = params != null && params.has("moduleType")
                ? params.get("moduleType").asText(null)
                : null;

        var requestedModules = parseRequestedModules(moduleParam);
        var requestedModuleType = parseRequestedModuleType(moduleTypeParam);

        log.debug("Listing fcli commands (module={}, moduleType={}, runnableOnly={}, includeHidden={}, modulesOnly={})",
                moduleParam, moduleTypeParam, runnableOnly, includeHidden, modulesOnly);

        try {
            var rootSpec = FcliCommandSpecHelper.getRootCommandLine().getCommandSpec();

            if (modulesOnly) {
                // Special path: return modules (with related + type filter) instead of commands
                return listModulesWithRelations(rootSpec, requestedModules, requestedModuleType, runnableOnly,
                        includeHidden);
            }

            // Normal commands listing path
            Stream<CommandSpec> commandStream = FcliCommandSpecHelper.commandTreeStream(rootSpec);

            // Apply module filter (single or multiple)
            if (requestedModules != null && !requestedModules.isEmpty()) {
                commandStream = commandStream.filter(spec -> {
                    String qualifiedName = spec.qualifiedName(" ");
                    String[] parts = qualifiedName.split(" ");
                    String moduleName = parts.length > 1 ? parts[1] : "";
                    return requestedModules.contains(moduleName);
                });
            }

            if (runnableOnly) {
                commandStream = commandStream.filter(FcliCommandSpecHelper::isRunnable);
            }

            if (!includeHidden) {
                commandStream = commandStream.filter(spec -> !spec.usageMessage().hidden());
            }

            ArrayNode commands = objectMapper.createArrayNode();
            commandStream
                    .map(this::specToDescriptor)
                    .forEach(commands::add);

            ObjectNode result = objectMapper.createObjectNode();
            result.set("commands", commands);
            result.put("count", commands.size());
            return result;
        } catch (Exception e) {
            log.error("Error listing fcli commands", e);
            throw RpcMethodException.internalError("Failed to list commands: " + e.getMessage(), e);
        }
    }

    private ObjectNode specToDescriptor(CommandSpec spec) {
        var descriptor = objectMapper.createObjectNode();
        var qualifiedName = spec.qualifiedName(" ");

        descriptor.put("name", qualifiedName);
        descriptor.put("module", extractModule(qualifiedName));
        descriptor.put("usageHeader", getUsageHeader(spec));
        descriptor.put("runnable", FcliCommandSpecHelper.isRunnable(spec));
        descriptor.put("hidden", spec.usageMessage().hidden());

        return descriptor;
    }

    private String extractModule(String qualifiedName) {
        // Format: "fcli <module> ..." or just "fcli"
        var parts = qualifiedName.split(" ");
        if (parts.length >= 2) {
            return parts[1];
        }
        return "";
    }

    private String getUsageHeader(CommandSpec spec) {
        var headerLines = spec.usageMessage().header();
        if (headerLines != null && headerLines.length > 0) {
            return String.join(" ", headerLines);
        }
        return "";
    }

    private Set<String> parseRequestedModules(String moduleParam) {
        if (moduleParam == null || moduleParam.isBlank()) {
            return null;
        }
        return Stream.of(moduleParam.split("[|,]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private JsonNode listModulesWithRelations(CommandSpec rootSpec,
            Set<String> requestedModules,
            ModuleType moduleTypeFilter,
            boolean runnableOnly,
            boolean includeHidden) {
        var modules = new java.util.LinkedHashSet<String>();

        FcliCommandSpecHelper.commandTreeStream(rootSpec)
                .forEach(spec -> {
                    if (runnableOnly && !FcliCommandSpecHelper.isRunnable(spec)) {
                        return;
                    }
                    if (!includeHidden && spec.usageMessage().hidden()) {
                        return;
                    }

                    String qualifiedName = spec.qualifiedName(" ");
                    String[] parts = qualifiedName.split(" ");
                    String moduleName = parts.length > 1 ? parts[1] : "";
                    String entityName = parts.length > 2 ? parts[2] : "";

                    // Only consider module-level commands: "fcli <module>"
                    if (moduleName.isEmpty() || !entityName.isEmpty()) {
                        return;
                    }

                    // No specific base modules requested: include all
                    if (requestedModules == null || requestedModules.isEmpty()) {
                        if (matchesModuleType(spec, moduleTypeFilter)) {
                            modules.add(moduleName);
                        }
                        return;
                    }

                    // Directly requested module
                    if (requestedModules.contains(moduleName)) {
                        if (matchesModuleType(spec, moduleTypeFilter)) {
                            modules.add(moduleName);
                        }
                        return;
                    }

                    // Indirectly related via @RelatedModules on the command class
                    RelatedModules related = getRelatedModulesAnnotation(spec);
                    if (related != null) {
                        for (String base : related.value()) {
                            if (requestedModules.contains(base)) {
                                if (matchesModuleType(spec, moduleTypeFilter)) {
                                    modules.add(moduleName);
                                }
                                break;
                            }
                        }
                    }
                });

        ArrayNode modulesArray = objectMapper.createArrayNode();
        modules.forEach(modulesArray::add);

        ObjectNode result = objectMapper.createObjectNode();
        result.set("modules", modulesArray);
        result.put("count", modules.size());
        return result;
    }

    private RelatedModules getRelatedModulesAnnotation(CommandSpec spec) {
        Object userObject = FcliCommandSpecHelper.userObject(spec);
        if (userObject == null) {
            return null;
        }
        return userObject.getClass().getAnnotation(RelatedModules.class);
    }

    private ModuleType parseRequestedModuleType(String moduleTypeParam) {
        if (moduleTypeParam == null || moduleTypeParam.isBlank()) {
            return null;
        }
        String v = moduleTypeParam.trim();
        if (v.equalsIgnoreCase("product")) {
            return ModuleType.PRODUCT;
        }
        if (v.equalsIgnoreCase("other") || v.equalsIgnoreCase("others")) {
            return ModuleType.OTHER;
        }
        return null; // Unknown value: ignore filter
    }

    private boolean matchesModuleType(CommandSpec spec, ModuleType moduleTypeFilter) {
        if (moduleTypeFilter == null) {
            return true; // no filter → accept all
        }
        ProductModule pm = getProductModuleAnnotation(spec);
        // Treat unannotated modules as OTHER by default
        ModuleType effectiveType = pm != null ? pm.value() : ModuleType.OTHER;
        return effectiveType == moduleTypeFilter;
    }

    private ProductModule getProductModuleAnnotation(CommandSpec spec) {
        Object userObject = FcliCommandSpecHelper.userObject(spec);
        if (userObject == null) {
            return null;
        }
        return userObject.getClass().getAnnotation(ProductModule.class);
    }
}
