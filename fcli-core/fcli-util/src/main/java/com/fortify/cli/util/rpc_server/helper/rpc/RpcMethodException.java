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

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Exception thrown by RPC method handlers to indicate a method execution error.
 * This maps to JSON-RPC error responses.
 *
 * @author Ruud Senden
 */
public class RpcMethodException extends Exception {
    private final int code;
    private final JsonNode data;
    
    public RpcMethodException(int code, String message) {
        this(code, message, null, null);
    }
    
    public RpcMethodException(int code, String message, JsonNode data) {
        this(code, message, data, null);
    }
    
    public RpcMethodException(int code, String message, JsonNode data, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.data = data;
    }
    
    public int getCode() {
        return code;
    }
    
    public JsonNode getData() {
        return data;
    }
    
    public JsonRpcError toJsonRpcError() {
        return new JsonRpcError(code, getMessage(), data);
    }
    
    public static RpcMethodException invalidParams(String message) {
        return new RpcMethodException(JsonRpcError.INVALID_PARAMS, message);
    }
    
    public static RpcMethodException internalError(String message) {
        return new RpcMethodException(JsonRpcError.INTERNAL_ERROR, message);
    }
    
    public static RpcMethodException internalError(String message, Throwable cause) {
        return new RpcMethodException(JsonRpcError.INTERNAL_ERROR, message, null, cause);
    }
}
