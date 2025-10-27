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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.spel.query.IQueryExpressionSupplier;
import com.fortify.cli.common.util.ReflectionHelper;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import lombok.Getter;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Given a {@link CommandSpec} instance, this class will collect all relevant {@link IMCPToolArgHandler} 
 * instances that will handle fcli options, positional parameters, queries, and paging. 
 *
 * @author Ruud Senden
 */
public final class MCPToolArgHandlers {
    private final List<IMCPToolArgHandler> mcpToolArgHandlers;
    @Getter private final JsonSchema schema;
    @Getter private final boolean paged;
    
    public MCPToolArgHandlers(CommandSpec spec) {
        // TODO Improve paged criteria, for example by looking at isSingular() if available, and/or allow 
        // customizing for individual commands through annotation or resource bundle
        this.paged = spec.name().startsWith("list");
        this.mcpToolArgHandlers = createToolSpecArgHelpers(spec, paged);
        this.schema = createSchema(mcpToolArgHandlers);
    }
    
    public final String getFcliCmdArgs(Map<String, Object> toolArgs) {
        return mcpToolArgHandlers.stream().map(h->h.getFcliCmdArgs(toolArgs==null?Map.of():toolArgs)).collect(Collectors.joining(" ")).trim();
    }

    private static final List<IMCPToolArgHandler> createToolSpecArgHelpers(CommandSpec spec, boolean paged) {
        var result = new ArrayList<IMCPToolArgHandler>();
        addArgSpecHelpers(result, spec.positionalParameters(), MCPToolArgHandlerFcliParam::new);
        addArgSpecHelpers(result, spec.options(), MCPToolArgHandlerFcliOption::new);
        addQueryToolSpecArgHelper(result, spec);
        addPagingArgSpecHelper(result, paged);
        return result;
    }

    private static void addPagingArgSpecHelper(ArrayList<IMCPToolArgHandler> result, boolean paged) {
        if ( paged ) {
            result.add(new MCPToolArgHandlerPaging());
        }
    }

    private static void addQueryToolSpecArgHelper(ArrayList<IMCPToolArgHandler> result, CommandSpec spec) {
        if ( hasGenericQueryOpt(spec) ) {
            result.add(new MCPToolArgHandlerQuery(spec));
        }
    }

    private static final boolean hasGenericQueryOpt(CommandSpec spec) {
        return FcliCommandSpecHelper.getAllUserObjectsStream(spec)
                .anyMatch(o -> o instanceof IQueryExpressionSupplier);
    }

    private static <T extends ArgSpec> void addArgSpecHelpers(List<IMCPToolArgHandler> result, List<T> argSpecs, Function<T, IMCPToolArgHandler> factory) {
        argSpecs.stream()
            .filter(as->!ignore(as))
            .map(factory::apply)
            .forEach(result::add);
    }

    private static final JsonSchema createSchema(List<IMCPToolArgHandler> mCPToolArgHandlers) {
        var result = new JsonSchema("object", new LinkedHashMap<String, Object>(), new ArrayList<String>(), false, new LinkedHashMap<String, Object>(), new LinkedHashMap<String, Object>());
        mCPToolArgHandlers.forEach(h->h.updateSchema(result));
        return result;
    }
    
    private static final boolean ignore(ArgSpec as) {
        return ReflectionHelper.hasAnnotation(as.userObject(), MCPExclude.class) || isSensitive(as);
    }
    
    public static final boolean isSensitive(ArgSpec as) {
        return (as.interactive() && !as.echo()) 
            || ReflectionHelper.getAnnotationValue(as.userObject(), MaskValue.class, MaskValue::sensitivity, ()->null)==LogSensitivityLevel.high;
    }
}