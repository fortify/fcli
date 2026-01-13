/*
 * Copyright 2021-2025 Open Text.
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
public record JsonRpcResponse(
    String jsonrpc,
    JsonNode result,
    JsonRpcError error,
    JsonNode id
) {
    public static JsonRpcResponse success(JsonNode id, JsonNode result) {
        return new JsonRpcResponse("2.0", result, null, id);
    }
    
    public static JsonRpcResponse error(JsonNode id, JsonRpcError error) {
        return new JsonRpcResponse("2.0", null, error, id);
    }
    
    public static JsonRpcResponse parseError() {
        return error(null, JsonRpcError.parseError());
    }
    
    public static JsonRpcResponse invalidRequest(JsonNode id) {
        return error(id, JsonRpcError.invalidRequest());
    }
    
    public static JsonRpcResponse methodNotFound(JsonNode id, String method) {
        return error(id, JsonRpcError.methodNotFound(method));
    }
    
    public static JsonRpcResponse invalidParams(JsonNode id, String message) {
        return error(id, JsonRpcError.invalidParams(message));
    }
    
    public static JsonRpcResponse internalError(JsonNode id, String message) {
        return error(id, JsonRpcError.internalError(message));
    }
}
