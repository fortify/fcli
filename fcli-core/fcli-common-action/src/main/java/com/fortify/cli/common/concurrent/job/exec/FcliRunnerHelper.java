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
package com.fortify.cli.common.concurrent.job.exec;

import java.util.ArrayList;
import java.util.Map;
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
        return collectStdout(fullCmd, null);
    }
    
    /**
     * Execute a command and collect stdout output with default options.
     */
    public static Result collectStdout(String fullCmd, Map<String, String> defaultOptions) {
        var builder = FcliCommandExecutorFactory.builder()
            .cmd(fullCmd)
            .stdoutOutputType(OutputType.collect)
            .stderrOutputType(OutputType.collect)
            .onFail(r -> {});
        
        if (defaultOptions != null) {
            builder.defaultOptionsIfNotPresent(defaultOptions);
        }
        
        return builder.build().create().execute();
    }
    
    /**
     * Execute a command and collect structured records.
     */
    public static Result collectRecords(String fullCmd, Consumer<ObjectNode> recordConsumer) {
        return collectRecords(fullCmd, recordConsumer, null);
    }
    
    /**
     * Execute a command and collect structured records with default options.
     */
    public static Result collectRecords(String fullCmd, Consumer<ObjectNode> recordConsumer, Map<String, String> defaultOptions) {
        var builder = FcliCommandExecutorFactory.builder()
            .cmd(fullCmd)
            .stdoutOutputTypeIfRecordCollectionSupported(OutputType.suppress)
            .stdoutOutputTypeIfRecordCollectionNotSupported(OutputType.collect)
            .stderrOutputType(OutputType.collect)
            .recordConsumer(recordConsumer)
            .onFail(r -> {});
        
        if (defaultOptions != null) {
            builder.defaultOptionsIfNotPresent(defaultOptions);
        }
        
        return builder.build().create().execute();
    }
    
    /**
     * Execute a command and return a FcliExecutionResult with all collected records.
     */
    public static FcliExecutionResult collectRecordsAsResult(String fullCmd) {
        return collectRecordsAsResult(fullCmd, null);
    }
    
    /**
     * Execute a command and return a FcliExecutionResult with all collected records and default options.
     */
    public static FcliExecutionResult collectRecordsAsResult(String fullCmd, Map<String, String> defaultOptions) {
        var records = new ArrayList<JsonNode>();
        var result = collectRecords(fullCmd, records::add, defaultOptions);
        return FcliExecutionResult.fromRecords(result, records);
    }
    
    /**
     * Execute a command and return a FcliExecutionResult with stdout.
     */
    public static FcliExecutionResult collectStdoutAsResult(String fullCmd) {
        return collectStdoutAsResult(fullCmd, null);
    }
    
    /**
     * Execute a command and return a FcliExecutionResult with stdout and default options.
     */
    public static FcliExecutionResult collectStdoutAsResult(String fullCmd, Map<String, String> defaultOptions) {
        var result = collectStdout(fullCmd, defaultOptions);
        return FcliExecutionResult.fromPlainText(result);
    }
}
