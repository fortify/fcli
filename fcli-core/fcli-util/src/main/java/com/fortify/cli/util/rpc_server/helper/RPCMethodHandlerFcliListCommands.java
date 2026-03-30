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
package com.fortify.cli.util.rpc_server.helper;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Model.CommandSpec;

/**
 * RPC method handler for listing available fcli commands.
 * 
 * Method: fcli.listCommands
 * Params:
 *   - module (string, optional): Filter by module (e.g., "ssc", "fod")
 *   - runnableOnly (boolean, optional): If true, only return runnable (leaf) commands
 *   - includeHidden (boolean, optional): If true, include hidden commands
 * 
 * Returns:
 *   - commands (array): Array of command descriptors with:
 *     - name (string): Qualified command name
 *     - module (string): The module this command belongs to
 *     - usageHeader (string): Short description
 *     - runnable (boolean): Whether the command is executable
 *     - hidden (boolean): Whether the command is hidden
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerFcliListCommands implements IRPCMethodHandler {
    private final ObjectMapper objectMapper;
    
    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        var module = params != null && params.has("module") 
            ? params.get("module").asText(null) : null;
        var runnableOnly = params != null && params.has("runnableOnly") 
            && params.get("runnableOnly").asBoolean(false);
        var includeHidden = params != null && params.has("includeHidden") 
            && params.get("includeHidden").asBoolean(false);
        
        log.debug("Listing fcli commands (module={}, runnableOnly={}, includeHidden={})", 
                  module, runnableOnly, includeHidden);
        
        try {
            var rootSpec = FcliCommandSpecHelper.getRootCommandLine().getCommandSpec();
            Stream<CommandSpec> commandStream = FcliCommandSpecHelper.commandTreeStream(rootSpec);
            
            // Apply filters
            if (module != null && !module.isBlank()) {
                final String modulePrefix = "fcli " + module + " ";
                final String moduleExact = "fcli " + module;
                commandStream = commandStream.filter(spec -> {
                    var qualifiedName = spec.qualifiedName(" ");
                    return qualifiedName.startsWith(modulePrefix) || qualifiedName.equals(moduleExact);
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
            throw RPCMethodException.internalError("Failed to list commands: " + e.getMessage(), e);
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
}
