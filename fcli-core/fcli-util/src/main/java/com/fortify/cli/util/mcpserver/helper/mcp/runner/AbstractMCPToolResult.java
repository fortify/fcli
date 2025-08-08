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
package com.fortify.cli.util.mcpserver.helper.mcp.runner;

import com.fortify.cli.common.json.JsonHelper;

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
    public final String asJsonString() {
        return JsonHelper.getObjectMapper().valueToTree(this).toPrettyString();
    }
    public final CallToolResult asCallToolResult() {
        return new CallToolResult(asJsonString(), getExitCode()!=0);
    }
    public abstract int getExitCode();
    public abstract String getStderr();
}
