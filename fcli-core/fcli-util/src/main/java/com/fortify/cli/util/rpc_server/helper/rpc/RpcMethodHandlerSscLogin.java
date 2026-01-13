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
import com.fortify.cli.util.rpc_server.helper.rpc.RpcSessionManager.ProductType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for SSC session login.
 * 
 * Method: fcli.ssc.login
 * Params:
 *   - url (string, required): SSC URL
 *   - user (string, optional): Username for user/password auth
 *   - password (string, optional): Password for user/password auth  
 *   - token (string, optional): UnifiedLoginToken for token-based auth
 *   - client-auth-token (string, optional): SC-SAST client auth token
 *   - sc-sast-url (string, optional): SC-SAST controller URL
 *   - expire-in (string, optional): Token expiration time (e.g., "1d", "8h")
 *   - insecure (boolean, optional): Allow insecure connections
 * 
 * At least one auth method must be provided: (user+password) or token.
 * 
 * Returns:
 *   - success (boolean): Whether login was successful
 *   - sessionName (string): The session name created
 *   - product (string): "ssc"
 *   - message (string): Status message
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RpcMethodHandlerSscLogin implements IRpcMethodHandler {
    private final ObjectMapper objectMapper;
    private final RpcSessionManager sessionManager;
    
    @Override
    public JsonNode execute(JsonNode params) throws RpcMethodException {
        if (params == null || !params.has("url")) {
            throw RpcMethodException.invalidParams("'url' parameter is required");
        }
        
        var loginArgs = buildLoginArgs(params);
        
        log.debug("SSC login with args: {}", loginArgs.replaceAll("(--password|--token|--client-auth-token)\\s+\\S+", "$1 ***"));
        
        return sessionManager.executeLogin(ProductType.SSC, loginArgs);
    }
    
    private String buildLoginArgs(JsonNode params) throws RpcMethodException {
        var sb = new StringBuilder();
        
        // URL is required
        sb.append("--url ").append(quoteValue(params.get("url").asText())).append(" ");
        
        // Authentication - at least one method required
        boolean hasAuth = false;
        
        if (params.has("user") && params.has("password")) {
            sb.append("--user ").append(quoteValue(params.get("user").asText())).append(" ");
            sb.append("--password ").append(quoteValue(params.get("password").asText())).append(" ");
            hasAuth = true;
        }
        
        if (params.has("token")) {
            sb.append("--token ").append(quoteValue(params.get("token").asText())).append(" ");
            hasAuth = true;
        }
        
        if (!hasAuth) {
            throw RpcMethodException.invalidParams(
                "SSC login requires one of: (user + password) or token");
        }
        
        // Optional parameters
        if (params.has("expire-in")) {
            sb.append("--expire-in ").append(params.get("expire-in").asText()).append(" ");
        }
        
        if (params.has("client-auth-token")) {
            sb.append("--client-auth-token ").append(quoteValue(params.get("client-auth-token").asText())).append(" ");
        }
        
        if (params.has("sc-sast-url")) {
            sb.append("--sc-sast-url ").append(quoteValue(params.get("sc-sast-url").asText())).append(" ");
        }
        
        if (params.has("insecure") && params.get("insecure").asBoolean(false)) {
            sb.append("-k ");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Quote a value for use in fcli command arguments.
     * Always quotes the value to ensure special characters are handled correctly.
     * The value is placed in double quotes with any internal quotes escaped.
     */
    private String quoteValue(String value) {
        if (value == null || value.isEmpty()) {
            return "\"\"";
        }
        // Escape any double quotes in the value and wrap in double quotes
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
