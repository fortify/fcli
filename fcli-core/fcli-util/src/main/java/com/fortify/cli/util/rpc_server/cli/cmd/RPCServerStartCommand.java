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
package com.fortify.cli.util.rpc_server.cli.cmd;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.util.rpc_server.helper.RPCMethodHandlerRegistry;
import com.fortify.cli.util.rpc_server.helper.RPCServer;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command to start the fcli JSON-RPC server for IDE plugin integration.
 * The server listens on stdin/stdout for JSON-RPC 2.0 requests and processes
 * them synchronously.
 *
 * @author Ruud Senden
 */
@Command(name = OutputHelperMixins.Start.CMD_NAME)
@MCPExclude
@Slf4j
public class RPCServerStartCommand extends AbstractRunnableCommand {
    // Stream overrides for functional tests (RPCServerHelper) that run the server
    // in-process via reflective invocation, where System streams cannot be replaced.
    private static volatile InputStream inputOverride;
    private static volatile OutputStream outputOverride;
    private static volatile OutputStream statusOutputOverride;
    
    /**
     * Configure stream overrides for the next server invocation, used by functional
     * tests to run the server in-process with piped streams. Set any parameter to
     * {@code null} to use the corresponding System stream.
     */
    public static void configureStreams(InputStream input, OutputStream output, OutputStream statusOutput) {
        inputOverride = input;
        outputOverride = output;
        statusOutputOverride = statusOutput;
    }
    
    /** Clear any previously configured stream overrides. */
    public static void clearStreamOverrides() {
        inputOverride = null;
        outputOverride = null;
        statusOutputOverride = null;
    }
    
    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names={"--import"}, split=",") private List<String> importFiles;
    @Option(names={"--no-defaults"}, defaultValue="false") private boolean noDefaults;
    
    @Override
    public Integer call() throws Exception {
        if (noDefaults && (importFiles == null || importFiles.isEmpty())) {
            throw new FcliSimpleException("--import is required when --no-defaults is specified");
        }
        log.info("Starting JSON-RPC server");

        var registryBuilder = RPCMethodHandlerRegistry.builder();
        if (!noDefaults) {
            registryBuilder.withDefaults();
        }
        if (importFiles != null) {
            importFiles.forEach(registryBuilder::importAction);
        }

        var input = inputOverride != null ? inputOverride : System.in;
        var output = outputOverride != null ? outputOverride : System.out;
        var statusOutput = statusOutputOverride != null ? statusOutputOverride : System.err;
        new RPCServer(registryBuilder.build()).start(input, output, statusOutput);

        return 0;
    }
}
