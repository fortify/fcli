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
        
        server.start(System.in, System.out);
        
        return 0;
    }

    private void registerImportedFunctions(RPCServer server, String importFile) {
        var action = loadImportedAction(importFile);
        for (var entry : action.getFunctions().entrySet()) {
            var function = entry.getValue();
            if (!function.isExported()) { continue; }
            var methodName = "fn." + function.getKey();
            var executor = new ActionFunctionExecutor(action, function);
            server.registerMethod(methodName, new RPCMethodHandlerActionFunction(executor));
            log.debug("Registered imported function as RPC method: {}", methodName);
        }
    }

    private Action loadImportedAction(String importFile) {
        var sources = ActionSource.externalActionSources(importFile);
        var validationHandler = ActionValidationHandler.WARN;
        return ActionLoaderHelper.streamAsActions(sources, validationHandler)
                .findFirst()
                .orElseThrow(() -> new FcliSimpleException("No action found in: " + importFile));
    }
}
