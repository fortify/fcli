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
 * JSON-RPC 2.0 error object. Per specification:
 * - code: Integer indicating the error type
 * - message: String providing a short description of the error
 * - data: Optional value containing additional information about the error
 *
 * Standard error codes:
 * -32700: Parse error
 * -32600: Invalid Request
 * -32601: Method not found
 * -32602: Invalid params
 * -32603: Internal error
 * -32000 to -32099: Server error (reserved for implementation-defined errors)
 *
 * @author Ruud Senden
 */
@Reflectable
@JsonInclude(Include.NON_NULL)
public record JsonRpcError(
    int code,
    String message,
    JsonNode data
) {
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    public static final int SERVER_ERROR = -32000;
    
    public static JsonRpcError parseError() {
        return new JsonRpcError(PARSE_ERROR, "Parse error", null);
    }
    
    public static JsonRpcError invalidRequest() {
        return new JsonRpcError(INVALID_REQUEST, "Invalid Request", null);
    }
    
    public static JsonRpcError methodNotFound(String method) {
        return new JsonRpcError(METHOD_NOT_FOUND, "Method not found: " + method, null);
    }
    
    public static JsonRpcError invalidParams(String details) {
        return new JsonRpcError(INVALID_PARAMS, "Invalid params: " + details, null);
    }
    
    public static JsonRpcError internalError(String details) {
        return new JsonRpcError(INTERNAL_ERROR, "Internal error: " + details, null);
    }
    
    public static JsonRpcError serverError(int code, String message, JsonNode data) {
        if (code > SERVER_ERROR || code < SERVER_ERROR - 99) {
            code = SERVER_ERROR;
        }
        return new JsonRpcError(code, message, data);
    }
}
