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
package com.fortify.cli.ai_assist.mcp.helper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.ai_assist.mcp.helper.arg.MCPToolArgHandlerPaging;
import com.fortify.cli.ai_assist.mcp.helper.runner.MCPResourceFcliRunnerFunction;
import com.fortify.cli.ai_assist.mcp.helper.runner.MCPToolFcliRunnerFunction;
import com.fortify.cli.ai_assist.mcp.helper.runner.MCPToolFcliRunnerFunctionStreaming;
import com.fortify.cli.common.action.helper.ActionLoaderHelper;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionSource;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.ActionFunction;
import com.fortify.cli.common.action.runner.ActionFunctionExecutor;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;

import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceTemplateSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.ResourceTemplate;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MCPImportedActionMcpSpecsFactory {
    private final MCPJobManager jobManager;
    private final Supplier<FcliExecutionContextHolder.ContextFrame> frameSupplier;

    public ImportedSpecs create(Path importFile) {
        var action = ActionLoaderHelper.load(
                ActionSource.externalActionSources(importFile.toString()),
                importFile.toString(),
                ActionValidationHandler.WARN
        ).getAction();
        var tools = new ArrayList<SyncToolSpecification>();
        var resourceTemplates = new ArrayList<SyncResourceTemplateSpecification>();
        for ( var entry : action.getFunctions().entrySet() ) {
            var functionName = entry.getKey();
            var function = entry.getValue();
            if ( !function.isExported() ) {
                continue;
            }
            if ( hasMcpResourceMeta(function) ) {
                resourceTemplates.add(createResourceTemplateSpec(action, functionName, function));
            } else {
                tools.add(createToolSpec(action, functionName, function));
            }
        }
        return new ImportedSpecs(tools, resourceTemplates);
    }

    private SyncToolSpecification createToolSpec(Action action, String functionName, ActionFunction function) {
        var executor = new ActionFunctionExecutor(action, function, frameSupplier);
        var toolName = "fcli_fn_" + functionName.replace('-', '_');
        var schema = buildFunctionArgsSchema(function);
        var description = function.getDescription() != null ? function.getDescription() : functionName;
        var runner = function.isStreaming()
                ? createStreamingRunner(executor, toolName, schema)
                : new MCPToolFcliRunnerFunction(executor, jobManager, toolName);
        var tool = Tool.builder()
                .name(toolName)
                .description(description)
                .inputSchema(schema)
                .build();
        return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
            .callHandler((ctx, request) -> runner.run(null, request))
                .build();
    }

    private MCPToolFcliRunnerFunctionStreaming createStreamingRunner(ActionFunctionExecutor executor, String toolName, JsonSchema schema) {
        new MCPToolArgHandlerPaging().updateSchema(schema);
        return new MCPToolFcliRunnerFunctionStreaming(executor, jobManager, toolName);
    }

    private SyncResourceTemplateSpecification createResourceTemplateSpec(Action action,
            String functionName, ActionFunction function)
    {
        var resourceMeta = function.getMeta().get("mcp.resource");
        var uriTemplate = getMetaString(resourceMeta, "uri-template");
        var name = getMetaString(resourceMeta, "name");
        var mimeType = getMetaString(resourceMeta, "mime-type");
        var executor = new ActionFunctionExecutor(action, function, frameSupplier);
        var template = ResourceTemplate.builder()
                .uriTemplate(uriTemplate)
                .name(name != null ? name : functionName)
                .description(function.getDescription())
                .mimeType(mimeType != null ? mimeType : "application/json")
                .build();
        var handler = new MCPResourceFcliRunnerFunction(executor, uriTemplate, mimeType);
        return new SyncResourceTemplateSpecification(template, (ctx, request) -> handler.read(null, request));
    }

    private JsonSchema buildFunctionArgsSchema(ActionFunction function) {
        var properties = new LinkedHashMap<String, Object>();
        var required = new ArrayList<String>();
        for ( var argEntry : function.getArgsOrEmpty().entrySet() ) {
            var argName = argEntry.getKey();
            var argDef = argEntry.getValue();
            var property = new LinkedHashMap<String, Object>();
            property.put("type", mapArgType(argDef.getType()));
            if ( argDef.getDescription() != null ) {
                property.put("description", argDef.getDescription());
            }
            properties.put(argName, property);
            if ( Boolean.TRUE.equals(argDef.getRequired()) ) {
                required.add(argName);
            }
        }
        return new JsonSchema("object", properties, required, false,
                new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    private String mapArgType(String type) {
        if ( type == null ) {
            return "string";
        }
        return switch (type) {
            case "boolean" -> "boolean";
            case "int", "long" -> "integer";
            case "double", "float" -> "number";
            case "array" -> "string";
            default -> "string";
        };
    }

    private boolean hasMcpResourceMeta(ActionFunction function) {
        return function.getMeta() != null && function.getMeta().has("mcp.resource");
    }

    private String getMetaString(JsonNode meta, String key) {
        if ( meta == null || !meta.has(key) ) {
            return null;
        }
        var node = meta.get(key);
        return node.isTextual() ? node.asText() : null;
    }

    public record ImportedSpecs(
            List<SyncToolSpecification> tools,
            List<SyncResourceTemplateSpecification> resourceTemplates
    ) {}
}