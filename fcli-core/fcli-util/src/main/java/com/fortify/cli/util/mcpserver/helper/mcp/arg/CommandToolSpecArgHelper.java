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
package com.fortify.cli.util.mcpserver.helper.mcp.arg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.mcp.MCPIgnore;
import com.fortify.cli.common.output.cli.mixin.QueryOptionsArgGroup;
import com.fortify.cli.common.util.ReflectionHelper;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import lombok.Getter;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;

public final class CommandToolSpecArgHelper {
    private final List<IToolSpecArgHelper> toolSpecArgHelpers;
    @Getter private final JsonSchema schema;
    @Getter private final boolean paged;
    
    public CommandToolSpecArgHelper(CommandSpec spec) {
        // TODO Improve paged criteria, for example by looking at isSingular() if available, and/or allow 
        // customizing for individual commands through annotation or resource bundle
        this.paged = spec.name().startsWith("list");
        this.toolSpecArgHelpers = createToolSpecArgHelpers(spec, paged);
        this.schema = createSchema(toolSpecArgHelpers);
    }
    
    public final String getFcliCmdArgs(Map<String, Object> toolArgs) {
        return toolSpecArgHelpers.stream().map(h->h.getFcliCmdArgs(toolArgs)).collect(Collectors.joining(" "));
    }

    private static final List<IToolSpecArgHelper> createToolSpecArgHelpers(CommandSpec spec, boolean paged) {
        var result = new ArrayList<IToolSpecArgHelper>();
        addArgSpecHelpers(result, spec.positionalParameters(), PositionalParamToolSpecArgHelper::new);
        addArgSpecHelpers(result, spec.options(), OptionToolSpecArgHelper::new);
        addQueryToolSpecArgHelper(result, spec);
        addPagingArgSpecHelper(result, paged);
        return result;
    }

    private static void addPagingArgSpecHelper(ArrayList<IToolSpecArgHelper> result, boolean paged) {
        if ( paged ) {
            result.add(new PagingToolSpecArgHelper());
        }
    }

    private static void addQueryToolSpecArgHelper(ArrayList<IToolSpecArgHelper> result, CommandSpec spec) {
        if ( hasGenericQueryOpt(spec) ) {
            result.add(new QueryToolSpecArgHelper(spec));
        }
    }

    private static final boolean hasGenericQueryOpt(CommandSpec spec) {
        var queryOpt = spec.optionsMap().get("--query"); 
        return queryOpt!=null && queryOpt.group()!=null && QueryOptionsArgGroup.class.equals(queryOpt.group().typeInfo().getType());
    }

    private static <T extends ArgSpec> void addArgSpecHelpers(List<IToolSpecArgHelper> result, List<T> argSpecs, Function<T, IToolSpecArgHelper> factory) {
        argSpecs.stream()
            .filter(as->!ignore(as))
            .map(factory::apply)
            .forEach(result::add);
    }

    private static final JsonSchema createSchema(List<IToolSpecArgHelper> toolSpecArgHelpers) {
        var result = new JsonSchema("object", new LinkedHashMap<String, Object>(), new ArrayList<String>(), false, new LinkedHashMap<String, Object>(), new LinkedHashMap<String, Object>());
        toolSpecArgHelpers.forEach(h->h.updateSchema(result));
        return result;
    }
    
    private static final boolean ignore(ArgSpec as) {
        return ReflectionHelper.hasAnnotation(as.userObject(), MCPIgnore.class) || isSensitive(as);
    }
    
    public static final boolean isSensitive(ArgSpec as) {
        return (as.interactive() && !as.echo()) 
            || ReflectionHelper.getAnnotationValue(as.userObject(), MaskValue.class, MaskValue::sensitivity, ()->null)==LogSensitivityLevel.high;
    }
}