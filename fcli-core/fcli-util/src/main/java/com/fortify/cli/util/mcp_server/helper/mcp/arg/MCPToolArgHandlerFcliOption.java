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
package com.fortify.cli.util.mcp_server.helper.mcp.arg;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Model.OptionSpec;

/**
 * {@link IMCPToolArgHandler} implementation for handling fcli options (represented as 
 * Picocli {@link OptionSpec}) as MCP tool arguments. Most of the functionality is
 * provided by the {@link AbstractMCPToolArgHandlerFcli} base class; this class just
 * provides the {@link OptionSpec} and name for the configured option, and the 
 * {@link #combineFcliCmdArgs(String, Stream)} method for formatting MCP tool argument
 * values as fcli options.
 *
 * @author Ruud Senden
 */
@RequiredArgsConstructor
public final class MCPToolArgHandlerFcliOption extends AbstractMCPToolArgHandlerFcli {
    @Getter private final OptionSpec argSpec;
    @Override
    protected String getName() {
        return argSpec.longestName();
    }
    @Override
    protected String combineFcliCmdArgs(String name, Stream<String> values) {
        return String.format("\"%s=%s\"", name, values.collect(Collectors.joining(",")));
    }
    @Override
    protected boolean isRequired() {
        // We always require session name to be specified, so we can inform the user about the session that will be used
        return getName().matches("--[a-z-]*session|--admin-config") ? true : super.isRequired();
    }
}