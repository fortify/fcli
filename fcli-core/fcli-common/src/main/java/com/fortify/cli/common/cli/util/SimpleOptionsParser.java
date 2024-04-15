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
package com.fortify.cli.common.cli.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

/**
 * This class allows for parsing unmatched options; if a command requires
 * support for dynamic options (that cannot be declared through Picocli
 * {@link Option} annotation), it can declare a field with Picocli
 * {@link Unmatched} annotation and use this class to parse & process
 * these unmatched options. 
 */
@RequiredArgsConstructor
public final class SimpleOptionsParser {
    private final List<IOptionDescriptor> options;
    @Getter(lazy = true) private final Map<String, IOptionDescriptor> optionsDescriptorsByNameAndAliases = _getOptionsDescriptorsByNameAndAliases();
    
    public static interface IOptionDescriptor {
        String getName();
        List<String> getAliases();
        String getDescription();
        
        default String getOptionNamesAndAliasesString(String delimiter) {
            var name = getName();
            List<String> aliases = getAliases()==null ? Collections.emptyList() : getAliases();
            return Stream.concat(Stream.of(name), aliases.stream())
                    .collect(Collectors.joining(delimiter));
        }
    }
    
    @Builder @Data
    public static class OptionDescriptor implements IOptionDescriptor {
        private final String name;
        private final List<String> aliases;
        private final String description;
    }
    
    @Data
    public final class OptionsParseResult {
        private final List<IOptionDescriptor> options;
        private final Map<String, String> optionValuesByName = new LinkedHashMap<>();
        private final Map<String, String> cliArgsByOptionNames = new LinkedHashMap<>();
        private final List<String> validationErrors = new ArrayList<>();
        
        public final boolean hasValidationErrors() {
            return validationErrors.size()>0;
        }
    }

    public final OptionsParseResult parse(String[] args) {
        var result = new OptionsParseResult(options);
        var validationErrors = result.getValidationErrors();
        var rawArgValues = parseArgs(validationErrors, args);
        updateResult(result, rawArgValues);
        return result;
    }

    private final void updateResult(OptionsParseResult result, Map<String, String> rawArgValues) {
        rawArgValues.entrySet().forEach(e->updateResult(result, e.getKey(), e.getValue()));
    }
    
    private final void updateResult(OptionsParseResult result, String arg, String value) {
        var option = getOption(arg);
        var name = option.getName();
        result.getCliArgsByOptionNames().put(name, arg);
        result.getOptionValuesByName().put(name, value); // Store option names, not aliases
    }

    /**
     * This method returns a map of CLI argument names with corresponding values. Note that
     * we return the argument names as specified on the command line, not the corresponding
     * option name. This allows for later validation messages to display the original
     * argument name (which may be an alias), not the option name. 
     */
    private Map<String, String> parseArgs(List<String> validationErrors, String[] args) {
        Map<String, String> result = new LinkedHashMap<>();
        if ( args!=null && args.length>0 ) {
            var optionsByNameAndAliases = getOptionsDescriptorsByNameAndAliases();
            var argsDeque = new ArrayDeque<>(Arrays.asList(args));
            while ( !argsDeque.isEmpty() ) {
                var argWithPossibleValue = argsDeque.pop();
                var argElts = argWithPossibleValue.split("=", 2);
                var arg = argElts[0];
                if ( !optionsByNameAndAliases.containsKey(arg) ) {
                    validationErrors.add("Unknown command line option: "+arg);
                } else if ( argElts.length==2 ) {
                    result.put(arg, argElts[1]);
                } else {
                    var nextArg = argsDeque.peek(); 
                    var value = optionsByNameAndAliases.containsKey(nextArg) ? null : argsDeque.pop();
                    result.put(arg, value);
                }
            }
        }
        return result;
    }
    
    private final Map<String, IOptionDescriptor> _getOptionsDescriptorsByNameAndAliases() {
        final Map<String, IOptionDescriptor> result = new LinkedHashMap<>();
        options.forEach(option->{
            result.put(option.getName(), option);
            var aliases = option.getAliases();
            if ( aliases!=null ) {
                aliases.forEach(alias->result.put(alias, option));
            }
        });
        return result;
    }
    
    private final IOptionDescriptor getOption(String nameOrAlias) {
        return getOptionsDescriptorsByNameAndAliases().get(nameOrAlias);
    }
}