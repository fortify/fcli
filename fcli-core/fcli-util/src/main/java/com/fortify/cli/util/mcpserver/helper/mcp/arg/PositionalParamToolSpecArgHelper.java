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

import java.lang.reflect.Field;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Model.PositionalParamSpec;

@RequiredArgsConstructor
public final class PositionalParamToolSpecArgHelper extends AbstractArgSpecToolSpecArgHelper {
    @Getter private final PositionalParamSpec argSpec;
    @Override
    protected String getName() {
       return ((Field)argSpec.userObject()).getName();
    }
    @Override
    protected String combineFcliCmdArgs(String name, Stream<String> values) {
        return values.map(v->"\""+v+"\"").collect(Collectors.joining(" "));
    }
}