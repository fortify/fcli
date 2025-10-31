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
package com.fortify.cli.util.mcp_server.helper.mcp.runner;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.OutputHelper.Result;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data class representing the output of the {@link MCPToolFcliRunnerPlainText},
 * storing stdout, stderr, and exit code.
 * 
 * @author Ruud Senden
 */
@Data @EqualsAndHashCode(callSuper = false) @Builder
@Reflectable
public class MCPToolResultPlainText extends AbstractMCPToolResult {
    private final String stdout;
    private final String stderr;
    private final int exitCode;
    
    public static final MCPToolResultPlainText from(Result result) {
        return builder()
            .exitCode(result.getExitCode())
            .stderr(result.getErr())
            .stdout(result.getOut())
            .build();
    }
}
