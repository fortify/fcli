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
import com.fortify.cli.common.cli.util.CommandSpecDescriptor;
import com.fortify.cli.common.json.JsonHelper;

import lombok.extern.slf4j.Slf4j;

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
public final class RPCMethodHandlerFcliListCommands implements IRPCMethodHandler {
    private static final ObjectMapper OM = JsonHelper.getObjectMapper();

    @Override
    public String description() {
        return "List available fcli commands with optional filtering";
    }

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
            Stream<CommandSpecDescriptor> descriptorStream = CommandSpecDescriptor.rootDescriptorStream();
            
            // Apply filters
            if (module != null && !module.isBlank()) {
                final String modulePrefix = "fcli " + module + " ";
                final String moduleExact = "fcli " + module;
                descriptorStream = descriptorStream.filter(d -> {
                    var qualifiedName = d.getSpec().qualifiedName(" ");
                    return qualifiedName.startsWith(modulePrefix) || qualifiedName.equals(moduleExact);
                });
            }

            if (runnableOnly) {
                descriptorStream = descriptorStream.filter(d -> d.getSpec() != null && (d.getSpec().userObject() instanceof Runnable || d.getSpec().userObject() instanceof java.util.concurrent.Callable));
            }

            if (!includeHidden) {
                descriptorStream = descriptorStream.filter(d -> !d.getSpec().usageMessage().hidden());
            }

            ArrayNode commands = OM.createArrayNode();
            descriptorStream.map(CommandSpecDescriptor::getCommandSpecNode).forEach(commands::add);
            
            ObjectNode result = OM.createObjectNode();
            result.set("commands", commands);
            result.put("count", commands.size());
            
            return result;
        } catch (Exception e) {
            log.error("Error listing fcli commands", e);
            throw RPCMethodException.internalError("Failed to list commands: " + e.getMessage(), e);
        }
    }
    
    // Returning commandSpecNode directly; no further conversion needed.
}
