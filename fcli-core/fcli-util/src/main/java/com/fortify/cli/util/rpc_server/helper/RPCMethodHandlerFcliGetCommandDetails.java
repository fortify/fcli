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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.CommandSpecDescriptor;
import com.fortify.cli.common.json.JsonHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RPCMethodHandlerFcliGetCommandDetails implements IRPCMethodHandler {
    private static final ObjectMapper OM = JsonHelper.getObjectMapper();

    @Override
    public String description() {
        return "Get detailed spec and argument information for a specific fcli command";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("command")) {
            throw RPCMethodException.invalidParams("Missing required parameter: command");
        }
        String command = params.get("command").asText(null);
        if (command == null || command.isBlank()) {
            throw RPCMethodException.invalidParams("Parameter 'command' is empty");
        }
        log.debug("Getting command args for: {}", command);
        try {
            var desc = CommandSpecDescriptor.of(command);
            if (desc == null) {
                throw RPCMethodException.invalidParams("Unknown command: "+command);
            }
            ObjectNode result = OM.createObjectNode();
            result.put("command", desc.getSpec().qualifiedName(" "));
            result.set("commandSpec", desc.getCommandSpecNode());
            result.set("commandArgs", desc.getCommandArgsNode());
            return result;
        } catch (RPCMethodException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting command args", e);
            throw RPCMethodException.internalError("Failed to get command args: "+e.getMessage(), e);
        }
    }
}
