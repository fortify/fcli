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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.util.FcliBuildProperties;

import lombok.RequiredArgsConstructor;

/**
 * RPC method handler for getting fcli version information.
 * 
 * Method: fcli.version
 * Params: none
 * 
 * Returns:
 *   - version (string): The fcli version
 *   - buildDate (string): The build date
 *   - actionSchemaVersion (string): The action schema version
 *
 * @author Ruud Senden
 */
@RequiredArgsConstructor
public final class RpcMethodHandlerFcliVersion implements IRpcMethodHandler {
    private final ObjectMapper objectMapper;
    
    @Override
    public JsonNode execute(JsonNode params) throws RpcMethodException {
        var props = FcliBuildProperties.INSTANCE;
        
        ObjectNode result = objectMapper.createObjectNode();
        result.put("version", props.getFcliVersion());
        result.put("buildDate", props.getFcliBuildDateString());
        result.put("actionSchemaVersion", props.getFcliActionSchemaVersion());
        
        return result;
    }
}
