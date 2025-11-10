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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.mcp.MCPDefaultValue;
import com.fortify.cli.common.util.ReflectionHelper;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.ITypeInfo;

/**
 * This abstract class provides a partial implementation of the {@link IMCPToolArgHandler}
 * interface for fcli options and positional parameters. The {@link #updateSchema(JsonSchema)}
 * method adds the option or positional parameter as described by the {@link #getArgSpec()}
 * and {@link #getName()} methods as an MCP tool argument. The {@link #getFcliCmdArgs(Map)}
 * method in turn processes the given MCP tool arguments, utilizing the abstract 
 * {@link #combineFcliCmdArgs(String, Stream)} method to generate the actual fcli arguments.
 *
 * @author Ruud Senden
 */
abstract class AbstractMCPToolArgHandlerFcli implements IMCPToolArgHandler { 
    protected abstract ArgSpec getArgSpec();
    protected abstract String getName();
    protected abstract String combineFcliCmdArgs(String name, Stream<String> values);
    @Override
    public void updateSchema(JsonSchema schema) {
        var argSpec = getArgSpec();
        schema.properties().put(getName(), createProperty(argSpec));
        if ( isRequired() ) {
            schema.required().add(getName());
        }
    }
    
    @Override
    public String getFcliCmdArgs(Map<String, Object> toolArgs) {
        var name = getName();
        var toolArgValue = toolArgs.get(name);
        return toolArgValue==null ? "" : combineFcliCmdArgs(name, streamValueElts(toolArgValue));
    }
    
    private static Stream<String> streamValueElts(Object value) {
        Stream<?> os = null;
        if ( value==null ) { 
            os = Stream.empty(); 
        } else if ( value.getClass().isArray() ) { 
            os = Stream.of((Object[])value); 
        } else if ( Collection.class.isAssignableFrom(value.getClass()) ) {
            os = ((Collection<?>)value).stream();
        } else {
            os = Stream.of(value);
        }
        return os.filter(Objects::nonNull).map(Object::toString);
    }
    
    private final ObjectNode createProperty(ArgSpec argSpec) {
        var node = JsonHelper.getObjectMapper().createObjectNode();
        node.put("description", getDescription());
        node.set("type", getPropertyType(argSpec.typeInfo()));
        var defVal = ReflectionHelper.getAnnotationValue(argSpec.userObject(), MCPDefaultValue.class, MCPDefaultValue::value, ()->null);
        if ( defVal!=null && !defVal.isBlank() ) {
            node.put("default", defVal);
        }
        return node;
    }

    private static final JsonNode getPropertyType(ITypeInfo typeInfo) {
        // TODO Although technically we can just always accept strings, using proper types based on the type of as.userObject(),
        //      for example booleans, might result in a better user experience. However, we do then need to convert back to string
        //      when generating the fcli command to be executed, which in most cases can likely be just toString() or similar, but
        //      arrays/collections may need to be converted to comma-separated string.
        var type = typeInfo.getType();
        
        // GitHub Copilot Eclipse plugin doesn't seem to like 'array' or 'enum' types, so returning 'string' for those for now.
        // Symptoms include Copilot preferences not listing any MCP tools (even from other MCP servers), and no response to chat messages.
        
        /* 
        if ( typeInfo.isArray() || typeInfo.isCollection() ) {
            return JsonHelper.getObjectMapper().createObjectNode()
                    .put("type", "array");
                    .set("items", 
                        JsonHelper.getObjectMapper().createObjectNode().set("type", getPropertyType(typeInfo.getAuxiliaryTypeInfos().get(0))));
        }
        */
        if ( typeInfo.isArray() || typeInfo.isCollection() ) {
            return new TextNode("string");
        }
        if (typeInfo.isBoolean()) {
            return new TextNode("boolean");
        }
        if (type==Integer.class || type==int.class ) {
            return new TextNode("integer");
        }
        if (type==Number.class || type==float.class || type==double.class) {
            return new TextNode("number");
        }
        //if (typeInfo.isEnum()) { return JsonHelper.getObjectMapper().createObjectNode().set("enum", JsonHelper.toArrayNode(typeInfo.getEnumConstantNames().toArray(String[]::new))); }
        return new TextNode("string");
    }

    protected boolean isRequired() {
        return getArgSpec().required(); // TODO If option is contained in exclusive arggroup, we need to consider it as optional
    }
    
    protected String getDescription() {
        String[] descElts = getArgSpec().description(); 
        return descElts==null || descElts.length<1 ? "" : String.join(" ", descElts);
    }
}