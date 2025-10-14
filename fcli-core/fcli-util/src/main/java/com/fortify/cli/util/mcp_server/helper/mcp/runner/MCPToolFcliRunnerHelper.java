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
package com.fortify.cli.util.mcp_server.helper.mcp.runner;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.common.util.OutputHelper.Result;

/**
 * Helper methods for running a given fcli command, collecting either records or stdout
 * 
 * @author Ruud Senden
 */
public class MCPToolFcliRunnerHelper {
    static final Result collectStdout(String fullCmd) {
        return createBuilder(fullCmd)
            .stdoutOutputType(OutputType.collect)
            .build().create().execute();
    }
    
    static final Result collectRecords(String fullCmd, Consumer<ObjectNode> recordConsumer) {
        return createBuilder(fullCmd)
            .stdoutOutputType(OutputType.suppress)
            .recordConsumer(recordConsumer)
            .build().create().execute();
    }
    
    static final MCPToolResultRecords collectRecords(String fullCmd) {
        var records = new ArrayList<JsonNode>();
        var result = collectRecords(fullCmd, records::add);
        return MCPToolResultRecords.from(result, records);
    }
    
    private static final FcliCommandExecutorFactory.FcliCommandExecutorFactoryBuilder createBuilder(String fullCmd) {
        return FcliCommandExecutorFactory.builder()
            .cmd(fullCmd)
            .onFail(r->{}) // Continue on non-zero exit code, assuming stdout/stderr shows more info about the error, which in turn can be
                        //  used by the LLM to provide suggestions on how to fix.
            .stderrOutputType(OutputType.collect);           
    }
}
