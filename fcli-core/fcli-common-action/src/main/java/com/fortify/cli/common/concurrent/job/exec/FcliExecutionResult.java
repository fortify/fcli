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
package com.fortify.cli.common.concurrent.job.exec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliExceptionHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.OutputHelper.Result;

import lombok.Builder;
import lombok.Data;

/**
 * Holds the result of a fcli command or action function execution for caching and transport.
 * Supports plain text (stdout), structured records, and errors.
 * Null fields are excluded from JSON serialization.
 * Shared between MCP server and RPC server implementations.
 *
 * @author Ruud Senden
 */
@Data @Builder
@Reflectable
@JsonInclude(Include.NON_NULL)
public class FcliExecutionResult {
    // Common fields for all result types
    private final Integer exitCode;
    private final String stderr;
    
    // Error fields (populated when exitCode != 0)
    private final String error;
    private final String errorStackTrace;
    private final String errorGuidance;
    
    // Plain text output
    private final String stdout;
    
    // Structured records output
    private final List<JsonNode> records;
    
    // Factory methods
    
    /**
     * Create result from fcli execution with plain text stdout.
     */
    public static FcliExecutionResult fromPlainText(Result result) {
        return builder()
            .exitCode(result.getExitCode())
            .stderr(result.getErr())
            .stdout(result.getOut())
            .build();
    }
    
    /**
     * Create result from fcli execution with structured records.
     */
    public static FcliExecutionResult fromRecords(Result result, List<JsonNode> records) {
        return builder()
            .exitCode(result.getExitCode())
            .stderr(result.getErr())
            .records(records)
            .build();
    }
    
    /**
     * Create error result from exit code and stderr message.
     */
    public static FcliExecutionResult fromError(int exitCode, String stderr) {
        return builder()
            .exitCode(exitCode)
            .stderr(stderr != null ? stderr : "Unknown error")
            .records(List.<JsonNode>of())
            .build();
    }
    
    /**
     * Create error result from exception with structured error information.
     */
    public static FcliExecutionResult fromError(Exception e) {
        return builder()
            .exitCode(1)
            .stderr(getErrorMessage(e))
            .error(getErrorMessage(e))
            .errorStackTrace(formatException(e))
            .errorGuidance(buildErrorGuidance())
            .records(List.<JsonNode>of())
            .build();
    }
    
    /**
     * Create error result with simple message.
     */
    public static FcliExecutionResult fromError(String message) {
        return fromError(1, message);
    }
    
    // Conversion to JSON
    
    public final String asJsonString() {
        return JsonHelper.getObjectMapper().valueToTree(this).toPrettyString();
    }
    
    public final JsonNode asJsonNode() {
        return JsonHelper.getObjectMapper().valueToTree(this);
    }

    // Exception formatting helpers
    
    private static String formatException(Exception e) {
        return FcliExceptionHelper.formatException(e);
    }
    
    private static String getErrorMessage(Exception e) {
        return FcliExceptionHelper.getErrorMessage(e);
    }
    
    private static String buildErrorGuidance() {
        return """
            The fcli command failed with an exception. You may use the error message and stack trace to:
            1. Diagnose the root cause and suggest corrective actions to resolve the issue
            2. Provide the error details to the user if manual troubleshooting is required
            3. Adjust command parameters or suggest alternative approaches to accomplish the task
            """;
    }
}
