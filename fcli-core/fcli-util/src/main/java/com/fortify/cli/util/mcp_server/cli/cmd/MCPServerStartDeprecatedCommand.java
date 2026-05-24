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
package com.fortify.cli.util.mcp_server.cli.cmd;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.cli.util.IFcliExecutionContextManager;
import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.OutputHelper.OutputType;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Unmatched;

@Command(name = OutputHelperMixins.Start.CMD_NAME)
@MCPExclude
@Slf4j
public class MCPServerStartDeprecatedCommand extends AbstractRunnableCommand implements IFcliExecutionContextManager {
    @Unmatched private List<String> delegatedArgs;

    @Override
    public Integer call() {
        var baseArgs = new String[]{"ai-assist", "mcp", "start-stdio"};
        var allArgs = delegatedArgs != null && !delegatedArgs.isEmpty()
            ? Stream.concat(Arrays.stream(baseArgs), delegatedArgs.stream()).toArray(String[]::new)
            : baseArgs;
        log.warn("The 'fcli util mcp-server start' command is deprecated; please use 'fcli ai-assist mcp start-stdio'");
        var result = FcliCommandExecutorFactory.builder()
            .args(allArgs)
            .stdoutOutputType(OutputType.show)
            .stderrOutputType(OutputType.show)
            .onFail(r -> {})
            .build().create().execute();
        if ( result.getExitCode() != 0 && StringUtils.isNotBlank(result.getErr()) ) {
            log.debug("Delegated command failed: {}", result.getErr());
        }
        return result.getExitCode();
    }
}
