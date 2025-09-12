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

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.OutputHelper.Result;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data class representing the output of the {@link MCPToolFcliRunnerRecords},
 * storing records, stderr, and exit code.
 * 
 * @author Ruud Senden
 */
@Data @EqualsAndHashCode(callSuper = false) @Builder
@Reflectable
public class MCPToolResultRecords extends AbstractMCPToolResult {
    private final List<JsonNode> records;
    private final String stderr;
    private final int exitCode;
    
    public static final MCPToolResultRecords from(Result result, List<JsonNode> records) {
        return builder()
            .exitCode(result.getExitCode())
            .stderr(result.getErr())
            .records(records)
            .build();
    }
}
