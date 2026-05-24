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
package com.fortify.cli.ai_assist.mcp.helper.http;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fortify.cli.common.action.runner.ActionSpelFunctions;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.SpelEvaluator;
import com.fortify.cli.common.spel.SpelHelper;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class MCPServerHttpConfigLoader {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public static MCPServerHttpConfig load(Path configPath) {
        if ( configPath == null ) {
            throw new FcliSimpleException("HTTP MCP config path must be specified");
        }
        var normalizedPath = configPath.toAbsolutePath().normalize();
        if ( !Files.isRegularFile(normalizedPath) ) {
            throw new FcliSimpleException("HTTP MCP config file not found: " + normalizedPath);
        }
        try {
            var rawNode = YAML_MAPPER.readTree(normalizedPath.toFile());
            var resolvedNode = resolveTemplateExpressions(rawNode);
            var result = YAML_MAPPER.treeToValue(resolvedNode, MCPServerHttpConfig.class);
            result.validate(normalizedPath);
            return result;
        } catch (FcliSimpleException e) {
            throw e;
        } catch (IOException e) {
            throw new FcliSimpleException("Unable to read HTTP MCP config file: " + normalizedPath, e);
        }
    }

    private static JsonNode resolveTemplateExpressions(JsonNode node) {
        if ( node == null || node.isNull() ) {
            return NullNode.getInstance();
        }
        if ( node.isObject() ) {
            var objectNode = (ObjectNode)node;
            var result = JsonHelper.getObjectMapper().createObjectNode();
            objectNode.properties().forEach(e -> result.set(e.getKey(), resolveTemplateExpressions(e.getValue())));
            return result;
        }
        if ( node.isArray() ) {
            ArrayNode result = JsonHelper.getObjectMapper().createArrayNode();
            node.forEach(v -> result.add(resolveTemplateExpressions(v)));
            return result;
        }
        if ( node.isTextual() ) {
            return resolveTemplateExpression(node.asText());
        }
        return node.deepCopy();
    }

    private static JsonNode resolveTemplateExpression(String value) {
        if ( value == null || !value.contains("${") ) {
            return value == null ? NullNode.getInstance() : new TextNode(value);
        }
        var evaluator = SpelEvaluator.JSON_GENERIC.copy()
                .configure(ctx -> SpelHelper.registerFunctions(ctx, ActionSpelFunctions.class));
        var result = evaluator.evaluate(SpelHelper.parseTemplateExpression(value), null, Object.class);
        return result == null ? NullNode.getInstance() : JsonHelper.getObjectMapper().valueToTree(result);
    }
}