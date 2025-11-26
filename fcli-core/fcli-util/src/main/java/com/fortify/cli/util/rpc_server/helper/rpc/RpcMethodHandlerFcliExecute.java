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

import java.util.ArrayList;
import java.util.List;

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
 * RPC method handler for executing fcli commands.
 * 
 * Method: fcli.execute
 * Params:
 *   - command (string, required): The fcli command to execute (e.g., "ssc appversion list")
 *   - collectRecords (boolean, optional): If true, collect structured records instead of stdout
 *   - offset (integer, optional): For paging, the offset to start from (default: 0)
 *   - limit (integer, optional): For paging, the maximum number of records (default: 100)
 * 
 * Returns:
 *   - exitCode (integer): The command exit code
 *   - records (array, optional): Array of record objects if collectRecords=true
 *   - stdout (string, optional): Standard output if collectRecords=false
 *   - stderr (string): Standard error output
 *   - pagination (object, optional): Pagination info for paged results
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RpcMethodHandlerFcliExecute implements IRpcMethodHandler {
    private final ObjectMapper objectMapper;
    
    @Override
    public JsonNode execute(JsonNode params) throws RpcMethodException {
        if (params == null || !params.has("command")) {
            throw RpcMethodException.invalidParams("'command' parameter is required");
        }
        
        var command = params.get("command").asText();
        var collectRecords = params.has("collectRecords") && params.get("collectRecords").asBoolean(false);
        var offset = params.has("offset") ? params.get("offset").asInt(0) : 0;
        var limit = params.has("limit") ? params.get("limit").asInt(100) : 100;
        
        if (command == null || command.isBlank()) {
            throw RpcMethodException.invalidParams("'command' cannot be empty");
        }
        
        if (offset < 0) {
            throw RpcMethodException.invalidParams("'offset' must be non-negative");
        }
        
        if (limit <= 0) {
            throw RpcMethodException.invalidParams("'limit' must be greater than 0");
        }
        
        log.debug("Executing fcli command: {} (collectRecords={}, offset={}, limit={})", 
                  command, collectRecords, offset, limit);
        
        try {
            if (collectRecords) {
                return executeWithRecords(command, offset, limit);
            } else {
                return executeWithStdout(command);
            }
        } catch (Exception e) {
            log.error("Error executing fcli command: {}", command, e);
            throw RpcMethodException.internalError("Command execution failed: " + e.getMessage(), e);
        }
    }
    
    private JsonNode executeWithStdout(String command) {
        var result = FcliCommandExecutorFactory.builder()
            .cmd(command)
            .stdoutOutputType(OutputType.collect)
            .stderrOutputType(OutputType.collect)
            .onFail(r -> {})
            .build().create().execute();
        
        return buildResponse(result, null, null);
    }
    
    private JsonNode executeWithRecords(String command, int offset, int limit) {
        var allRecords = new ArrayList<JsonNode>();
        
        var result = FcliCommandExecutorFactory.builder()
            .cmd(command)
            .stdoutOutputType(OutputType.suppress)
            .stderrOutputType(OutputType.collect)
            .recordConsumer(allRecords::add)
            .onFail(r -> {})
            .build().create().execute();
        
        // Apply pagination
        var totalRecords = allRecords.size();
        var endIndex = Math.min(offset + limit, totalRecords);
        List<JsonNode> pagedRecords = offset >= totalRecords 
            ? List.of() 
            : allRecords.subList(offset, endIndex);
        
        var pagination = buildPagination(offset, limit, totalRecords);
        return buildResponse(result, pagedRecords, pagination);
    }
    
    private ObjectNode buildResponse(Result result, List<JsonNode> records, ObjectNode pagination) {
        var response = objectMapper.createObjectNode();
        response.put("exitCode", result.getExitCode());
        
        if (records != null) {
            ArrayNode recordsArray = response.putArray("records");
            records.forEach(recordsArray::add);
        } else {
            response.put("stdout", result.getOut());
        }
        
        if (result.getErr() != null && !result.getErr().isBlank()) {
            response.put("stderr", result.getErr());
        }
        
        if (pagination != null) {
            response.set("pagination", pagination);
        }
        
        return response;
    }
    
    private ObjectNode buildPagination(int offset, int limit, int totalRecords) {
        var pagination = objectMapper.createObjectNode();
        pagination.put("offset", offset);
        pagination.put("limit", limit);
        pagination.put("totalRecords", totalRecords);
        pagination.put("totalPages", (int) Math.ceil((double) totalRecords / limit));
        pagination.put("hasMore", offset + limit < totalRecords);
        
        if (offset + limit < totalRecords) {
            pagination.put("nextOffset", offset + limit);
        }
        
        return pagination;
    }
}
