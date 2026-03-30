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

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.common.util.OutputHelper.Result;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for executing fcli commands synchronously.
 * 
 * Method: fcli.execute
 * Params:
 *   - command (string, required): The fcli command to execute (e.g., "ssc appversion list")
 *   - collectRecords (boolean, optional): If true, collect structured records instead of stdout
 * 
 * Returns:
 *   - exitCode (integer): The command exit code
 *   - records (array, optional): Array of ALL record objects if collectRecords=true
 *   - stdout (string, optional): Standard output if collectRecords=false
 *   - stderr (string): Standard error output
 * 
 * Note: This method returns ALL records without paging. For commands that may return
 * large datasets (e.g., issue list), use fcli.executeAsync + fcli.getPage instead.
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RPCMethodHandlerFcliExecute implements IRPCMethodHandler {
    private final ObjectMapper objectMapper;
    
    @Override
    public JsonNode execute(JsonNode params) throws RPCMethodException {
        if (params == null || !params.has("command")) {
            throw RPCMethodException.invalidParams("'command' parameter is required");
        }
        
        var command = params.get("command").asText();
        var collectRecords = params.has("collectRecords") && params.get("collectRecords").asBoolean(false);
        
        if (command == null || command.isBlank()) {
            throw RPCMethodException.invalidParams("'command' cannot be empty");
        }
        
        log.debug("Executing fcli command: {} (collectRecords={})", command, collectRecords);
        
        try {
            if (collectRecords) {
                return executeWithRecords(command);
            } else {
                return executeWithStdout(command);
            }
        } catch (Exception e) {
            log.error("Error executing fcli command: {}", command, e);
            throw RPCMethodException.internalError("Command execution failed: " + e.getMessage(), e);
        }
    }
    
    private JsonNode executeWithStdout(String command) {
        var result = FcliCommandExecutorFactory.builder()
            .cmd(command)
            .stdoutOutputType(OutputType.collect)
            .stderrOutputType(OutputType.collect)
            .onFail(r -> {})
            .build().create().execute();
        
        return buildResponse(result, null);
    }
    
    private JsonNode executeWithRecords(String command) {
        var allRecords = new ArrayList<JsonNode>();
        
        var result = FcliCommandExecutorFactory.builder()
            .cmd(command)
            .stdoutOutputType(OutputType.suppress)
            .stderrOutputType(OutputType.collect)
            .recordConsumer(allRecords::add)
            .onFail(r -> {})
            .build().create().execute();
        
        return buildResponse(result, allRecords);
    }
    
    private ObjectNode buildResponse(Result result, java.util.List<JsonNode> records) {
        var response = objectMapper.createObjectNode();
        response.put("exitCode", result.getExitCode());
        
        if (records != null) {
            ArrayNode recordsArray = response.putArray("records");
            records.forEach(recordsArray::add);
            response.put("totalRecords", records.size());
        } else {
            response.put("stdout", result.getOut());
        }
        
        if (result.getErr() != null && !result.getErr().isBlank()) {
            response.put("stderr", result.getErr());
        }
        
        return response;
    }
}
