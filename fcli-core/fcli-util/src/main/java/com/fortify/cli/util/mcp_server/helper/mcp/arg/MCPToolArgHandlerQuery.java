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
package com.fortify.cli.util.mcp_server.helper.mcp.arg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.PropertyPathFormatter;
import com.fortify.cli.common.output.transform.fields.SelectedFieldsTransformer;
import com.fortify.cli.common.util.PicocliSpecHelper;
import com.networknt.schema.utils.StringUtils;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import lombok.SneakyThrows;
import picocli.CommandLine.Model.CommandSpec;

/**
 * {@link IMCPToolArgHandler} implementation for handling queries. The {@link #updateSchema(JsonSchema)}
 * method adds a {@value MCPToolArgHandlerQuery#ARG_QUERY} argument to the MCP tool schema, generating
 * an associated JSON type definition that lists some of the commonly used fields that can be queried
 * upon. The {@link #getFcliCmdArgs(Map)} uses the {@value MCPToolArgHandlerQuery#ARG_QUERY} argument
 * value to generate the appropriate SpEL-formatted value for the fcli --query argument.
 *
 * @author Ruud Senden
 */
public final class MCPToolArgHandlerQuery implements IMCPToolArgHandler {
    private static final String ARG_QUERY = "query";
    private final CommandSpec spec;
    private final Map<String, String> fieldsBySchemaPropertyName;
    private final ObjectNode querySchema;
    
    public MCPToolArgHandlerQuery(CommandSpec spec) {
        this.spec = spec;
        this.fieldsBySchemaPropertyName = generateFieldsBySchemaPropertyName(spec);
        this.querySchema = generateQuerySchema(spec, fieldsBySchemaPropertyName.keySet());
    }
    
    private static final Map<String, String> generateFieldsBySchemaPropertyName(CommandSpec spec) {
        var result = new LinkedHashMap<String, String>();
        var fieldsFromTableArgs = PicocliSpecHelper.getMessageString(spec, "output.table.args");
        var fieldsFromMCPFields = PicocliSpecHelper.getMessageString(spec, "mcp.fields");
        var fields = Stream.of(fieldsFromTableArgs, fieldsFromMCPFields).filter(StringUtils::isNotBlank).collect(Collectors.joining(","));
        if ( StringUtils.isNotBlank(fields) ) {
            SelectedFieldsTransformer.parsePropertyNames(fields)
                .entrySet().forEach(e->addFieldBySchemaPropertyName(result, spec, e));
        }
        return result;
    }
    
    private static final void addFieldBySchemaPropertyName(LinkedHashMap<String, String> result, CommandSpec spec, Entry<String, String> e) {
        var fieldName = e.getKey();
        var schemaPropertyName = e.getValue();
        if ( StringUtils.isBlank(schemaPropertyName) ) { // Table columns may have empty headers, so we use field name instead
            schemaPropertyName = fieldName;
        }
        schemaPropertyName = schemaPropertyName.replaceAll("String$", ""); // Remove 'String' suffix, like in OutputRecordWriterFactory::addHeader
        /* TODO Do we want to add entity prefix?
        if ( !schemaPropertyName.contains(".") ) { // Add entity name, for example 'name' becomes 'appversion.name'
            var entityName = spec.name().startsWith("list-") 
                    ? spec.name().replaceFirst("^list-", "")
                    : spec.parent()!=null
                      ? spec.parent().name()
                      : null;
            if ( StringUtils.isNotBlank(entityName) ) {
                var singularEntityName = entityName.replaceAll("s$", "");
                if ( !schemaPropertyName.toLowerCase().startsWith(singularEntityName.toLowerCase())) {
                    schemaPropertyName = String.format("%s.%s", singularEntityName, schemaPropertyName);
                }
            }
        }
        */
        schemaPropertyName = schemaPropertyName.replaceAll("[-_]", "."); // Replace dashes and underscores by '.'
        result.put(schemaPropertyName, fieldName);
    }
    
    private static final ObjectNode generateQuerySchema(CommandSpec spec, Set<String> schemaPropertyNames) {
        var properties = JsonHelper.getObjectMapper().createObjectNode();
        schemaPropertyNames.forEach(p->properties.set(p, getPropertySchema(spec, p)));
        return JsonHelper.getObjectMapper().createObjectNode()
                .put("type", "object")
                .put("description", "TODO")
                .put("title", "TODO")
                .set("properties", properties);
    }

    @SneakyThrows
    private static final ObjectNode getPropertySchema(CommandSpec spec, String schemaPropertyName) {
        var schemaString = getMessageString(spec, schemaPropertyName, "schema", null);
        if ( schemaString==null ) {
            schemaString = generatePropertySchemaString(spec, schemaPropertyName);
        }
        return (ObjectNode)JsonHelper.getObjectMapper().readTree(schemaString);
    }

    private static final String generatePropertySchemaString(CommandSpec spec, String schemaPropertyName) {
        var humanReadableName = PropertyPathFormatter.humanReadable(schemaPropertyName);
        return String.format("""
            {
              "anyOf": [
                {
                  "type": "string",
                  "format": "regex"
                },
                {
                  "type": "null"
                }
              ],
              "default": null,
              "title": "%s",
              "description": "%s"
            }
            """, 
              getMessageString(spec, schemaPropertyName, "title", humanReadableName), 
              getMessageString(spec, schemaPropertyName, "description", getPropertyDescription(humanReadableName))
            );
    }

    private static String getPropertyDescription(String humanReadableName) {
        return String.format("Match full %s against the given regular expression; for 'contains' queries, make sure to include '.*' before and after the regex to be matched", humanReadableName);
    }

    private static final String getMessageString(CommandSpec spec, String schemaPropertyName, String name, String defaultValue) {
        var result = PicocliSpecHelper.getMessageString(spec, String.format("mcp.%s.%s", schemaPropertyName, name));
        return StringUtils.isNotBlank(result) ? result : defaultValue;
    }

    @Override @SneakyThrows
    public void updateSchema(JsonSchema schema) {
        var defName = PropertyPathFormatter.pascalCase(String.format("%s.query", spec.qualifiedName(".").replaceAll("[-_]", "."))); 
        schema.properties().put(ARG_QUERY, JsonHelper.getObjectMapper().readTree(String.format("""
            {
              "anyOf": [
                {
                  "$ref": "#/$defs/%s"
                },
                {
                  "type": "null"
                }
              ],
              "default": null,
              "title": "Query",
              "description": "Filter results of this MCP tool using the given queries."
            }    
            """, defName)));
        schema.defs().put(defName, querySchema);
    }
    
    @Override
    public String getFcliCmdArgs(Map<String, Object> toolArgs) {
        var queries = new ArrayList<String>();
        var queryObj = toolArgs.get(ARG_QUERY);
        if ( queryObj != null ) {
            if ( queryObj instanceof Map ) {
                ((Map<?,?>)queryObj).entrySet().forEach(e->addQuery(queries, (String)e.getKey(), (String)e.getValue() ));
            } else if ( queryObj instanceof ObjectNode ) {
                ((ObjectNode)queryObj).properties().forEach(e->addQuery(queries, e.getKey(), e.getValue().asText()));
            } else {
                throw new FcliSimpleException("Invalid type (%s) for --query; expected object", queryObj.getClass().getSimpleName());
            }
        }
        return queries.isEmpty() ? "" : String.format("\"--query=%s\"", String.join(" && ", queries));
    }

    private final void addQuery(ArrayList<String> queries, String schemaPropertyName, String value) {
        var fieldName = fieldsBySchemaPropertyName.getOrDefault(schemaPropertyName, schemaPropertyName);
        queries.add(String.format("%s matches '%s'", fieldName, value));
    }
}