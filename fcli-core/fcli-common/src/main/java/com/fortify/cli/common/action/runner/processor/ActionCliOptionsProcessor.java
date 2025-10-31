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
package com.fortify.cli.common.action.runner.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.ActionCliOption;
import com.fortify.cli.common.action.model.FcliActionValidationException;
import com.fortify.cli.common.action.runner.ActionRunnerConfig;
import com.fortify.cli.common.cli.util.SimpleOptionsParser;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.IOptionDescriptor;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.OptionDescriptor;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.OptionsParseResult;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.log.LogMaskHelper;
import com.fortify.cli.common.log.LogMaskSource;
import com.fortify.cli.common.spel.IConfigurableSpelEvaluator;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import lombok.Builder;

@Builder
public final class ActionCliOptionsProcessor {
    private final ActionRunnerConfig config;
    private final IConfigurableSpelEvaluator spelEvaluator;
    private static final Map<String, Function<String, JsonNode>> optionValueConverters = createParameterConverters();

    public final ObjectNode parseOptionValues(String[] args) {
        var optionValues = JsonHelper.getObjectMapper().createObjectNode();
        var optionsParseResult = _parseParameterValues(optionValues, args);
        if ( optionsParseResult.hasValidationErrors() ) {
            throw config.getOnValidationErrors().apply(optionsParseResult);
        }
        config.getAction().getCliOptions().values().forEach(o->addOptionValues(optionsParseResult, o, optionValues));
        return optionValues;
    }
    
    private final OptionsParseResult _parseParameterValues(ObjectNode result, String[] args) {
        List<IOptionDescriptor> optionDescriptors = ActionOptionHelper.getOptionDescriptors(config.getAction());
        var parseResult = new SimpleOptionsParser(optionDescriptors).parse(args);
        addDefaultValues(parseResult, result);
        addValidationMessages(parseResult);
        return parseResult;
    }

    private final void addDefaultValues(OptionsParseResult parseResult, ObjectNode parameterValues) {
        config.getAction().getCliOptions().values().forEach(o->addDefaultValue(parseResult, o, parameterValues));
    }
    
    private final void addValidationMessages(OptionsParseResult parseResult) {
        config.getAction().getCliOptions().values().forEach(o->addValidationMessages(parseResult, o));
    }
    
    private final void addDefaultValue(OptionsParseResult parseResult, ActionCliOption option, ObjectNode parameterValues) {
        var value = getOptionValue(parseResult, option);
        if ( value==null ) {
            var defaultValueExpression = option.getDefaultValue();
            value = defaultValueExpression==null 
                    ? null 
                    : spelEvaluator.evaluate(defaultValueExpression, parameterValues, String.class);
        }
        parseResult.getOptionValuesById().put(option.getKey(), value);
    }
    
    private final void addValidationMessages(OptionsParseResult parseResult, ActionCliOption option) {
        if ( option.isRequired() && StringUtils.isBlank(getOptionValue(parseResult, option)) ) {
            parseResult.getValidationErrors().add("No value provided for required option "+
                Arrays.stream(option.getNamesAsArray()).collect(Collectors.joining(" | ")));           
        }
    }

    private final void addOptionValues(OptionsParseResult optionsParseResult, ActionCliOption option, ObjectNode optionValues) {
        var value = getOptionValue(optionsParseResult, option);
        if ( value==null ) {
            var defaultValueExpression = option.getDefaultValue();
            value = defaultValueExpression==null 
                    ? null 
                    : spelEvaluator.evaluate(defaultValueExpression, optionValues, String.class);
        }
        var mask = option.getMask(); 
        if ( mask!=null ) {
            var description = mask.getDescription();
            if ( StringUtils.isBlank(description)) {
                description = option.getKey();
            }
            LogMaskHelper.INSTANCE.registerValue(mask.getSensitivityLevel(), LogMaskSource.CLI_OPTION, description, value, mask.getPattern());
        }
        optionValues.set(option.getKey(), convertOptionValue(value, option, optionValues));
    }
    private String getOptionValue(OptionsParseResult parseResult, ActionCliOption option) {
        return parseResult.getOptionValuesById().get(option.getKey());
    }
    
    private JsonNode convertOptionValue(String value, ActionCliOption option, ObjectNode optionValues) {
        var type = StringUtils.isBlank(option.getType()) ? "string" : option.getType();
        var optionValueConverter = optionValueConverters.get(type);
        if ( optionValueConverter==null ) {
            throw new FcliActionValidationException(String.format("Unknown option type %s for option %s", type, option.getKey())); 
        } else {
            var result = optionValueConverter.apply(value);
            return result==null ? NullNode.instance : result; 
        }
    }
    
    public static final class ActionOptionHelper {
        private ActionOptionHelper() {}
        
        public static final List<IOptionDescriptor> getOptionDescriptors(Action action) {
            var parameters = action.getCliOptions();
            List<IOptionDescriptor> result = new ArrayList<>(parameters.size());
            parameters.values().forEach(o->addOptionDescriptor(result, o));
            return result;
        }

        private static final void addOptionDescriptor(List<IOptionDescriptor> result, ActionCliOption cliOption) {
            result.add(OptionDescriptor.builder()
                    .id(cliOption.getKey())
                    .optionNames(cliOption.getNamesAsArray())
                    .description(cliOption.getDescription())
                    .bool(cliOption.getType()!=null && cliOption.getType().equals("boolean"))
                    .build());
        }
        
        public static final String getSupportedOptionsTable(Action action) {
            return getSupportedOptionsTable(getOptionDescriptors(action));
        }
        
        public static final String getSupportedOptionsTable(List<IOptionDescriptor> options) {
            return AsciiTable.builder()
                .border(AsciiTable.NO_BORDERS)
                .data(new Column[] {
                        new Column().dataAlign(HorizontalAlign.LEFT),
                        new Column().dataAlign(HorizontalAlign.LEFT),
                    },
                    options.stream()
                        .map(option->new String[] {option.getOptionNamesString(" | "), option.getDescription()})
                        .toList().toArray(String[][]::new))
                .asString();
        }
    }
    
    private static final Map<String, Function<String, JsonNode>> createParameterConverters() {
        Map<String, Function<String, JsonNode>> result = new HashMap<>();
        // TODO Most of these will likely fail in case value is null or empty
        result.put("string",  v->new TextNode(v));
        result.put("boolean", v->BooleanNode.valueOf(Boolean.parseBoolean(v)));
        result.put("int",     v->IntNode.valueOf(Integer.parseInt(v)));
        result.put("long",    v->LongNode.valueOf(Long.parseLong(v)));
        result.put("double",  v->DoubleNode.valueOf(Double.parseDouble(v)));
        result.put("float",   v->FloatNode.valueOf(Float.parseFloat(v)));
        result.put("array",   v->StringUtils.isBlank(v)
                ? JsonHelper.toArrayNode(new String[] {}) 
                : JsonHelper.toArrayNode(v.split(",")));
        // TODO Add BigIntegerNode/DecimalNode/ShortNode support?
        return result;
    }
}