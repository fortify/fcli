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
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.FcliBuildProperties;

/**
 * RPC method handler for returning full fcli build information.
 *
 * Method: fcli.buildInfo
 * Params: none
 *
 * Returns an object with all fcli build properties:
 *   - projectName (string)
 *   - version (string)
 *   - buildDate (string)
 *   - actionSchemaVersion (string)
 *   - actionSchemaUrl (string)
 *   - docBaseUrl (string)
 *   - sourceCodeBaseUrl (string)
 *
 * @author Ruud Senden
 */
public final class RPCMethodHandlerFcliInfo implements IRPCMethodHandler {
    private static final ObjectMapper OM = JsonHelper.getObjectMapper();

    @Override
    public String description() {
        return "Get fcli build information (version, build date, schema versions, URLs)";
    }

    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        var props = FcliBuildProperties.INSTANCE;

        ObjectNode result = OM.createObjectNode();
        result.put("projectName", props.getFcliProjectName());
        result.put("version", props.getFcliVersion());
        result.put("buildDate", props.getFcliBuildDateString());
        result.put("actionSchemaVersion", props.getFcliActionSchemaVersion());
        result.put("actionSchemaUrl", props.getFcliActionSchemaUrl());
        result.put("docBaseUrl", props.getFcliDocBaseUrl());
        result.put("sourceCodeBaseUrl", props.getSourceCodeBaseUrl());

        return result;
    }
}
