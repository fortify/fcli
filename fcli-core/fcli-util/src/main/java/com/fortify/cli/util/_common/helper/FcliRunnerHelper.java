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
package com.fortify.cli.util._common.helper;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.common.util.OutputHelper.Result;

/**
 * Helper methods for running fcli commands, collecting either records or stdout.
 * This class is shared between MCP server and RPC server implementations.
 * 
 * @author Ruud Senden
 */
public class FcliRunnerHelper {
    
    /**
     * Execute a command and collect stdout output.
     */
    public static Result collectStdout(String fullCmd) {
        return FcliCommandExecutorFactory.builder()
            .cmd(fullCmd)
            .stdoutOutputType(OutputType.collect)
            .stderrOutputType(OutputType.collect)
            .onFail(r -> {})
            .build().create().execute();
    }
    
    /**
     * Execute a command and collect structured records.
     */
    public static Result collectRecords(String fullCmd, Consumer<ObjectNode> recordConsumer) {
        return FcliCommandExecutorFactory.builder()
            .cmd(fullCmd)
            .stdoutOutputType(OutputType.suppress)
            .stderrOutputType(OutputType.collect)
            .recordConsumer(recordConsumer)
            .onFail(r -> {})
            .build().create().execute();
    }
    
    /**
     * Execute a command and return a FcliToolResult with all collected records.
     */
    public static FcliToolResult collectRecordsAsResult(String fullCmd) {
        var records = new ArrayList<JsonNode>();
        var result = collectRecords(fullCmd, records::add);
        return FcliToolResult.fromRecords(result, records);
    }
    
    /**
     * Execute a command and return a FcliToolResult with stdout.
     */
    public static FcliToolResult collectStdoutAsResult(String fullCmd) {
        var result = collectStdout(fullCmd);
        return FcliToolResult.fromPlainText(result);
    }
}
