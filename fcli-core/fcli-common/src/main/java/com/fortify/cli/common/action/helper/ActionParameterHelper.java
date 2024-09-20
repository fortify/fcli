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
package com.fortify.cli.common.action.helper;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.ActionParameter;
import com.fortify.cli.common.action.runner.ActionRunner.ParameterTypeConverterArgs;
import com.fortify.cli.common.action.runner.ActionSpelFunctions;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spring.expression.IConfigurableSpelEvaluator;
import com.fortify.cli.common.spring.expression.SpelEvaluator;
import com.fortify.cli.common.spring.expression.SpelHelper;
import com.fortify.cli.common.util.StringUtils;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

@Builder
public final class ActionParameterHelper {
    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(ActionParameterHelper.class);
    /** Static SpEL evaluator configured with {@link ActionSpelFunctions} */
    private static final IConfigurableSpelEvaluator spelEvaluator = SpelEvaluator.JSON_GENERIC.copy()
        .configure(c->SpelHelper.registerFunctions(c, ActionSpelFunctions.class));
    
    /** Action instance, configured through builder method */
    @NonNull private final Action action;
    /** Type-based OptionSpec configurers */
    @Singular private final Map<String, BiConsumer<OptionSpec.Builder, ActionParameter>> optionSpecTypeConfigurers;
    /** What action to take on unknown parameter types, configured through builder method */
    private final OnUnknownParameterType onUnknownParameterType;

    public final void addOptions(CommandSpec actionCmd) {
        for ( var p : action.getParameters() ) {
            actionCmd.addOption(createOptionSpec(p));
        }
    }
    
    public final ObjectNode getParameterValues(CommandSpec actionCmd, Function<ActionParameter, ParameterTypeConverterArgs> parameterTypeConverterArgsSupplier) {
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
            builder.defaultValue(spelEvaluator.evaluate(p.getDefaultValue(), null, String.class));
        }
        builder.description(p.getDescription());
        builder.required(p.isRequired());
        return builder.build();
    }

    private void configureType(OptionSpec.Builder builder, ActionParameter p) {
        var type = p.getType();
        var configurer = optionSpecTypeConfigurers.get(StringUtils.ifBlank(type, "string"));
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
    
    public static class ActionParameterHelperBuilder {
        public ActionParameterHelperBuilder() {
            addDefaultOptionSpecTypeConfigurers();
        }

        private final void addDefaultOptionSpecTypeConfigurers() {
            optionSpecTypeConfigurer("string",  (b,p)->b.arity("1").type(String.class));
            optionSpecTypeConfigurer("boolean", (b,p)->b.arity("0..1").type(Boolean.class).defaultValue("false"));
            optionSpecTypeConfigurer("int",     (b,p)->b.arity("1").type(Integer.class));
            optionSpecTypeConfigurer("long",    (b,p)->b.arity("1").type(Long.class));
            optionSpecTypeConfigurer("double",  (b,p)->b.arity("1").type(Double.class));
            optionSpecTypeConfigurer("float",   (b,p)->b.arity("1").type(Float.class));
            optionSpecTypeConfigurer("array",   (b,p)->b.arity("1").type(ArrayNode.class).converters(new ArrayNodeConverter()));
        }
    }
    
    private static final class ArrayNodeConverter implements ITypeConverter<ArrayNode> {
        @Override
        public ArrayNode convert(String value) throws Exception {
            return StringUtils.isBlank(value)
                ? JsonHelper.toArrayNode(new String[] {}) 
                : JsonHelper.toArrayNode(value.split(","));
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
