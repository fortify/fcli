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
package com.fortify.cli.common.action.runner.n;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.ActionParameter;
import com.fortify.cli.common.action.runner.ActionRunner.ParameterTypeConverterArgs;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.StringUtils;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Model.ArgGroupSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

@Builder
public final class ActionParameterHelper {
    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(ActionParameterHelper.class);
    private final ActionSourceConfig actionSourceConfig;
    private final ActionRuntimeConfig actionRuntimeConfig;
    /** What action to take on unknown parameter types, configured through builder method */
    private final OnUnknownParameterType onUnknownParameterType;

    public final void addOptions(CommandSpec actionCmd) {
        var action = actionSourceConfig.getAction();
        var actionArgGroup = ArgGroupSpec.builder()
                .exclusive(false)
                .heading("Action Options\n")
                .multiplicity("1");
        for ( var p : action.getParameters() ) {
            actionArgGroup.addArg(createOptionSpec(p));
        }
        actionCmd.addArgGroup(actionArgGroup.build());
    }
    
    public final ObjectNode getParameterValues(CommandSpec actionCmd, Function<ActionParameter, ParameterTypeConverterArgs> parameterTypeConverterArgsSupplier) {
        var action = actionSourceConfig.getAction();
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        for ( var p : action.getParameters() ) {
            var name = p.getName();
            var option = actionCmd.findOption(name);
            if ( option==null ) { throw new IllegalStateException("Can't find option for parameter name: "+name); }
            var value = option.getValue();
            if ( value instanceof ParameterValueSupplier ) {
                value = ((ParameterValueSupplier)value).getValue(parameterTypeConverterArgsSupplier.apply(p));
            }
            result.putPOJO(name, value);
        }
        return result;
    }

    private final OptionSpec createOptionSpec(ActionParameter p) {
        var builder = OptionSpec.builder(getOptionNames(p));
        configureType(builder, p);
        if ( p.getDefaultValue()!=null ) {
            builder.defaultValue(actionRuntimeConfig.createSpelEvaluator().evaluate(p.getDefaultValue(), null, String.class));
        }
        builder.description(p.getDescription());
        builder.required(p.isRequired());
        return builder.build();
    }

    private void configureType(OptionSpec.Builder builder, ActionParameter p) {
        var type = p.getType();
        var configurer = actionRuntimeConfig.getOptionSpecTypeConfigurer(StringUtils.ifBlank(type, "string"));
        if ( configurer==null ) {
            (onUnknownParameterType==null ? OnUnknownParameterType.ERROR : onUnknownParameterType).configure(builder, p);
        } else {
            configurer.accept(builder, p);
        }
    }

    private static final String[] getOptionNames(ActionParameter p) {
        var names = new ArrayList<String>();
        names.add(getOptionName(p.getName()));
        for ( var alias : p.getCliAliasesArray() ) {
            names.add(getOptionName(alias));
        }
        return names.toArray(String[]::new);
    }

    private static final String getOptionName(String name) {
        var prefix = name.length()==1 ? "-" : "--";
        return prefix+name;
    }
    
    @RequiredArgsConstructor
    public static enum OnUnknownParameterType { 
        WARN(OnUnknownParameterType::warn), ERROR(OnUnknownParameterType::error);
        
        private final BiConsumer<OptionSpec.Builder, ActionParameter> configurer;
        
        public void configure(OptionSpec.Builder builder, ActionParameter p) {
            configurer.accept(builder, p);
        }
        
        private static final void warn(OptionSpec.Builder builder, ActionParameter p) {
            LOG.warn("WARN: "+getMessage(p)+", action will fail to run");
            builder.arity("1").type(String.class).paramLabel("<unknown>");
        }
        
        private static final void error(OptionSpec.Builder builder, ActionParameter p) {
            throw new IllegalStateException(getMessage(p));
        }
        
        private static final String getMessage(ActionParameter p) {
            return "Unknow parameter type '"+p.getType()+"' for parameter '"+p.getName()+"'";
        }
    }
    
    // TODO What values should we store in this class (like ActionParameter object),
    //      and what values should be passed on the getValue method?
    @RequiredArgsConstructor
    public static final class ParameterValueSupplier {
        private final String value;
        private final BiFunction<String, ParameterTypeConverterArgs, JsonNode> converter;
        
        public JsonNode getValue(ParameterTypeConverterArgs args) {
            return converter.apply(value, args);
        }
        
        public static final OptionSpec.Builder configure(OptionSpec.Builder builder, BiFunction<String, ParameterTypeConverterArgs, JsonNode> converter) {
            builder.arity("1").type(ParameterValueSupplier.class).converters(value->new ParameterValueSupplier(value, converter));
            return builder;
        }
    }
}
