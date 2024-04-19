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
package com.fortify.cli.common.action.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.model.ActionParameter;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.IOptionDescriptor;
import com.fortify.cli.common.cli.util.SimpleOptionsParser.OptionDescriptor;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

public final class ActionParameterHelper {
    private ActionParameterHelper() {}
    
    public static final List<IOptionDescriptor> getOptionDescriptors(Action action) {
        var parameters = action.getParameters();
        List<IOptionDescriptor> result = new ArrayList<>(parameters.size());
        parameters.forEach(p->addOptionDescriptor(result, p));
        return result;
    }

    private static final void addOptionDescriptor(List<IOptionDescriptor> result, ActionParameter parameter) {
        result.add(OptionDescriptor.builder()
                .name(getOptionName(parameter.getName()))
                .aliases(getOptionAliases(parameter.getCliAliasesArray()))
                .description(parameter.getDescription())
                .build());
    }
    
    static final String getOptionName(String parameterNameOrAlias) {
        var prefix = parameterNameOrAlias.length()==1 ? "-" : "--";
        return prefix+parameterNameOrAlias;
    }
    
    private static final List<String> getOptionAliases(String[] aliases) {
        return aliases==null ? null : Stream.of(aliases).map(ActionParameterHelper::getOptionName).toList();
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
                    .map(option->new String[] {option.getOptionNamesAndAliasesString(" | "), option.getDescription()})
                    .toList().toArray(String[][]::new))
            .asString();
    }

}
