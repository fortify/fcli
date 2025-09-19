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
package com.fortify.cli.util.mcp_server.helper.mcp.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.session.helper.FcliNoSessionException;
import com.fortify.cli.common.util.StringHelper;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * Base class for storing and generating the result data of {@link AbstractMCPToolFcliRunner}
 * implementations. All subclasses are required to provide fcli exit code and stderr output,
 * usually combined with implementation-specific content like stdout or structured records.
 * This base class provides utility methods for formatting the result through the 
 * {@link #asJsonString()} and {@link #asCallToolResult()} methods.
 * 
 * @author Ruud Senden
 */
public abstract class AbstractMCPToolResult {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMCPToolResult.class);
    
    public final String asJsonString() {
        return JsonHelper.getObjectMapper().valueToTree(this).toPrettyString();
    }
    public final CallToolResult asCallToolResult() {
        var output = asJsonString();
        var hasError = getExitCode()!=0;
        if ( hasError && output.contains(FcliNoSessionException.class.getSimpleName()) ) {
            var loginCmd = FcliNoSessionException.getLoginCmd(output);
            output = String.format("""
                    Tell user that command failed due to missing or invalid session, \
                    and ask them to run the '%s' command from a terminal window. Please \
                    show the command to be run on a separate line for better visibility.
                    """, loginCmd);
        }
        LOG.debug("Returning MCP tool result (hasError={}):\n{}", hasError, StringHelper.indent(output, "\t"));
        return new CallToolResult(output, hasError);
    }
    public abstract int getExitCode();
    public abstract String getStderr();
}
