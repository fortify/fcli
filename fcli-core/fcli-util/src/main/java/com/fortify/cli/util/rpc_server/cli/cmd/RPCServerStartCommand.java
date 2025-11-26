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
package com.fortify.cli.util.rpc_server.cli.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.util.rpc_server.helper.rpc.JsonRpcServer;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command to start the fcli JSON-RPC server for IDE plugin integration.
 * The server listens on stdin/stdout for JSON-RPC 2.0 requests.
 *
 * @author Ruud Senden
 */
@Command(name = OutputHelperMixins.Start.CMD_NAME)
@MCPExclude
@Slf4j
public class RPCServerStartCommand extends AbstractRunnableCommand {
    @Option(names = {"--threads", "-t"}, defaultValue = "4")
    private int threads;
    
    @Override
    public Integer call() throws Exception {
        log.info("Starting JSON-RPC server with {} threads", threads);
        
        var objectMapper = new ObjectMapper();
        var server = new JsonRpcServer(objectMapper, threads);
        
        // Start the server on stdin/stdout
        server.start(System.in, System.out);
        
        return 0;
    }
}
