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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * JSON-RPC 2.0 response object. Per specification:
 * - jsonrpc: MUST be "2.0"
 * - result: Required on success. Value determined by method invocation.
 * - error: Required on error. Error object describing the error.
 * - id: MUST be same as request id, or null if id couldn't be determined
 *
 * @author Ruud Senden
 */
@Reflectable
@JsonInclude(Include.NON_NULL)
public record RPCResponse(
    String jsonrpc,
    JsonNode result,
    RPCError error,
    JsonNode id
) {
    public static RPCResponse success(JsonNode id, JsonNode result) {
        return new RPCResponse("2.0", result, null, id);
    }
    
    public static RPCResponse error(JsonNode id, RPCError error) {
        return new RPCResponse("2.0", null, error, id);
    }
    
    public static RPCResponse parseError() {
        return error(null, RPCError.parseError());
    }
    
    public static RPCResponse invalidRequest(JsonNode id) {
        return error(id, RPCError.invalidRequest());
    }
    
    public static RPCResponse methodNotFound(JsonNode id, String method) {
        return error(id, RPCError.methodNotFound(method));
    }
    
    public static RPCResponse invalidParams(JsonNode id, String message) {
        return error(id, RPCError.invalidParams(message));
    }
    
    public static RPCResponse internalError(JsonNode id, String message) {
        return error(id, RPCError.internalError(message));
    }
}
