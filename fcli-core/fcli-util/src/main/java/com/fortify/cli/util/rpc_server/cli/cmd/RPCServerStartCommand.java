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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.runner.ActionFunctionExecutor;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.util.rpc_server.helper.RPCMethodHandlerActionFunction;
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
        
        var objectMapper = new ObjectMapper();
        var server = new RPCServer(objectMapper, !noDefaults);
        
        if (importFiles != null) {
            for (var importFile : importFiles) {
                registerImportedFunctions(server, importFile);
            }
        }
        
        var input = inputOverride != null ? inputOverride : System.in;
        var output = outputOverride != null ? outputOverride : System.out;
        var statusOutput = statusOutputOverride != null ? statusOutputOverride : System.err;
        server.start(input, output, statusOutput);
        
        return 0;
    }

    private void registerImportedFunctions(RPCServer server, String importFile) {
        var action = loadImportedAction(importFile);
        for (var entry : action.getFunctions().entrySet()) {
            var function = entry.getValue();
            if (!function.isExported()) { continue; }
            var methodName = "fn." + function.getKey();
            var executor = new ActionFunctionExecutor(action, function);
            server.registerMethod(methodName, new RPCMethodHandlerActionFunction(executor, server.getCache()));
            log.debug("Registered imported function as RPC method: {}", methodName);
        }
    }

    private Action loadImportedAction(String importFile) {
        var sources = ActionSource.externalActionSources(importFile);
        var validationHandler = ActionValidationHandler.WARN;
        return ActionLoaderHelper.load(sources, importFile, validationHandler).getAction();
    }
}
