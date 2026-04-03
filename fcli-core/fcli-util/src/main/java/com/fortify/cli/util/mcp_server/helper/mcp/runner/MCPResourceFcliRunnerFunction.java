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
package com.fortify.cli.util.mcp_server.helper.mcp.runner;

import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.runner.ActionFunctionExecutor;
import com.fortify.cli.common.json.JsonHelper;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

/**
 * Handler for MCP resource read requests backed by action functions.
 * Extracts URI template parameters from the request URI, maps them to
 * function args, executes the function, and returns the result as
 * {@link TextResourceContents}.
 */
public final class MCPResourceFcliRunnerFunction {
    private static final Pattern URI_TEMPLATE_PARAM = Pattern.compile("\\{([^}]+)}");
    private final ActionFunctionExecutor executor;
    private final String uriTemplate;
    private final String mimeType;

    public MCPResourceFcliRunnerFunction(ActionFunctionExecutor executor, String uriTemplate, String mimeType) {
        this.executor = executor;
        this.uriTemplate = uriTemplate;
        this.mimeType = mimeType != null ? mimeType : "application/json";
    }

    public ReadResourceResult read(McpSyncServerExchange exchange, ReadResourceRequest request) {
        try {
            var argsNode = extractArgsFromUri(request.uri());
            var result = executor.execute(argsNode);
            var text = resultToString(result);
            var contents = new TextResourceContents(request.uri(), mimeType, text);
            return new ReadResourceResult(List.of(contents));
        } catch (Exception e) {
            var errorText = "Error: " + e.getMessage();
            var contents = new TextResourceContents(request.uri(), "text/plain", errorText);
            return new ReadResourceResult(List.of(contents));
        }
    }

    private ObjectNode extractArgsFromUri(String uri) {
        var argsNode = JsonHelper.getObjectMapper().createObjectNode();
        var templatePattern = buildUriPattern(uriTemplate);
        var matcher = templatePattern.matcher(uri);
        if (matcher.matches()) {
            var paramNames = extractParamNames(uriTemplate);
            for (int i = 0; i < paramNames.size(); i++) {
                argsNode.put(paramNames.get(i), matcher.group(i + 1));
            }
        }
        return argsNode;
    }

    private static Pattern buildUriPattern(String uriTemplate) {
        // Split template on {param} placeholders, quote literal parts, replace params with capture groups
        var parts = URI_TEMPLATE_PARAM.split(uriTemplate, -1);
        var sb = new StringBuilder("^");
        var paramMatcher = URI_TEMPLATE_PARAM.matcher(uriTemplate);
        int partIdx = 0;
        while (paramMatcher.find()) {
            sb.append(Pattern.quote(parts[partIdx++]));
            sb.append("([^/]+)");
        }
        if (partIdx < parts.length) {
            sb.append(Pattern.quote(parts[partIdx]));
        }
        sb.append("$");
        return Pattern.compile(sb.toString());
    }

    private static List<String> extractParamNames(String uriTemplate) {
        var result = new java.util.ArrayList<String>();
        var matcher = URI_TEMPLATE_PARAM.matcher(uriTemplate);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private String resultToString(Object result) {
        if (result instanceof JsonNode jn) {
            return jn.toPrettyString();
        }
        if (result instanceof com.fortify.cli.common.action.model.ActionStepRecordsForEach.IActionStepForEachProcessor processor) {
            var records = new java.util.ArrayList<JsonNode>();
            processor.process(node -> {
                records.add(node);
                return true;
            });
            var arrayNode = JsonHelper.getObjectMapper().createArrayNode();
            records.forEach(arrayNode::add);
            return arrayNode.toPrettyString();
        }
        return String.valueOf(result);
    }
}
