/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.util.mcpserver.helper.mcp.exec;

import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.util.mcpserver.helper.mcp.arg.CommandToolSpecArgHelper;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Model.CommandSpec;

@RequiredArgsConstructor
public final class CommandToolSpecPlainExecutor extends AbstractCommandToolSpecExecutor {
    @Getter private final CommandToolSpecArgHelper toolSpecArgHelper;
    @Getter private final CommandSpec commandSpec;
    
    @Override
    protected CallToolResult execute(McpSyncServerExchange exchange, CallToolRequest request, String fullCmd) {
        var result = FcliCommandExecutorFactory.builder()
            .cmd(fullCmd)
            .stdoutOutputType(OutputType.collect)
            .stderrOutputType(OutputType.collect)
            .onFail(r->{}) // Continue on non-zero exit code, assuming stdout/stderr shows more info about the error, which in turn can be
                           //  used by the LLM to provide suggestions on how to fix.
            .build().create().execute();
        return new CallToolResult(JsonHelper.getObjectMapper().valueToTree(result).toPrettyString(), result.getExitCode()!=0);
    }
}