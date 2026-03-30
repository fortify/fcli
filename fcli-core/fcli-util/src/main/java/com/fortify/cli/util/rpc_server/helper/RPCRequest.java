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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * JSON-RPC 2.0 request object. Per specification:
 * - jsonrpc: MUST be "2.0"
 * - method: String containing the name of the method to be invoked
 * - params: Optional structured value holding parameter values
 * - id: An identifier established by the client (can be string, number, or null for notifications)
 *
 * @author Ruud Senden
 */
@Reflectable
@JsonIgnoreProperties(ignoreUnknown = true)
public record RPCRequest(
    String jsonrpc,
    String method,
    JsonNode params,
    JsonNode id
) {
    public boolean isNotification() {
        return id == null || id.isNull();
    }
    
    public boolean isValid() {
        return "2.0".equals(jsonrpc) && method != null && !method.isBlank();
    }
}
