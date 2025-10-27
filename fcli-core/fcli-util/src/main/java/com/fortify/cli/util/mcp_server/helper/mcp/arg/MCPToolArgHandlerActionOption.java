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
package com.fortify.cli.util.mcp_server.helper.mcp.arg;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.fortify.cli.common.json.JsonHelper;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;

/**
 * IMCPToolArgHandler implementation for action CLI options defined in action YAML files.
 * Each instance represents a single action CLI option (using its longest name like --file).
 */
public final class MCPToolArgHandlerActionOption implements IMCPToolArgHandler {
    private final String name; // Must include leading dashes like --file
    private final String description;
    private final boolean required;
    private final String type; // Simplified JSON schema type mapping
    
    public MCPToolArgHandlerActionOption(String name, String description, boolean required, String actionType) {
        this.name = name;
        this.description = description==null?"":description;
        this.required = required;
        this.type = mapType(actionType);
    }
    
    @Override
    public void updateSchema(JsonSchema schema) {
        var property = JsonHelper.getObjectMapper().createObjectNode()
                .put("description", description)
                .put("type", type);
        schema.properties().put(name, property);
        if ( required ) { schema.required().add(name); }
    }

    @Override
    public String getFcliCmdArgs(Map<String, Object> toolArgs) {
        var value = toolArgs==null?null:toolArgs.get(name);
        if ( value==null ) { return ""; }
        var values = streamValueElements(value).toList();
        if ( values.isEmpty() ) { return ""; }
        return String.format("\"%s=%s\"", name, String.join(",", values));
    }
    
    private static Stream<String> streamValueElements(Object value) {
        Stream<?> os;
        if ( value==null ) { os = Stream.empty(); }
        else if ( value.getClass().isArray() ) { os = Stream.of((Object[])value); }
        else if ( Collection.class.isAssignableFrom(value.getClass()) ) { os = ((Collection<?>)value).stream(); }
        else { os = Stream.of(value); }
        return os.filter(Objects::nonNull).map(Object::toString);
    }
    
    private static String mapType(String actionType) {
        if ( actionType==null ) { return "string"; }
        switch (actionType) {
            case "boolean": return "boolean";
            case "int": return "integer";
            case "long": return "integer";
            case "double": return "number";
            case "float": return "number";
            case "array": return "string"; // Represent arrays as comma-separated string
            default: return "string";
        }
    }
}
