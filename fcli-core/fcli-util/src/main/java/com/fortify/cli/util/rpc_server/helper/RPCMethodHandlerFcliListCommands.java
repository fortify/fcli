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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.CommandSpecDescriptor;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.query.QueryExpression;
import com.fortify.cli.common.spel.query.QueryExpressionTypeConverter;

import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for listing available fcli commands.
 * 
 * Method: fcli.listCommands
 * Params:
 *   - query (string, optional): SpEL expression to filter commands (same syntax as --query option)
 * 
 * Returns:
 *   - commands (array): Array of command descriptors
 *   - count (integer): Number of matching commands
 *
 * @author Ruud Senden
 */
@Slf4j
public final class RPCMethodHandlerFcliListCommands implements IRPCMethodHandler {
    private static final ObjectMapper OM = JsonHelper.getObjectMapper();
    private static final QueryExpressionTypeConverter QUERY_CONVERTER = new QueryExpressionTypeConverter();

    @Override
    public String description() {
        return "List available fcli commands with optional SpEL query filtering";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        var queryString = params != null && params.has("query")
            ? params.get("query").asText(null) : null;

        log.debug("Listing fcli commands (query={})", queryString);

        try {
            QueryExpression queryExpression = queryString != null && !queryString.isBlank()
                ? QUERY_CONVERTER.convert(queryString) : null;

            ArrayNode commands = OM.createArrayNode();
            CommandSpecDescriptor.rootDescriptorStream()
                .filter(d -> d.matches(queryExpression))
                .map(CommandSpecDescriptor::getCommandSpecNode)
                .forEach(commands::add);

            ObjectNode result = OM.createObjectNode();
            result.set("commands", commands);
            result.put("count", commands.size());

            return result;
        } catch (Exception e) {
            log.error("Error listing fcli commands", e);
            throw RPCMethodException.internalError("Failed to list commands: " + e.getMessage(), e);
        }
    }
}
