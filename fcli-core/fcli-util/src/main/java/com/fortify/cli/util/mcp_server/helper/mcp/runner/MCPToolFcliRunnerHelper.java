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
package com.fortify.cli.util.mcp_server.helper.mcp.runner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.mcp.MCPDefaultValue;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.common.util.OutputHelper.Result;
import com.fortify.cli.common.util.ReflectionHelper;

import picocli.CommandLine.Model.CommandSpec;

/**
 * Helper methods for running a given fcli command, collecting either records or stdout
 * 
 * @author Ruud Senden
 */
public class MCPToolFcliRunnerHelper {
    static final Result collectStdout(String fullCmd, CommandSpec spec) {
        return createBuilder(fullCmd, spec)
            .stdoutOutputType(OutputType.collect)
            .build().create().execute();
    }
    
    static final Result collectRecords(String fullCmd, Consumer<ObjectNode> recordConsumer, CommandSpec spec) {
        return createBuilder(fullCmd, spec)
            .stdoutOutputType(OutputType.suppress)
            .recordConsumer(recordConsumer)
            .build().create().execute();
    }
    
    static final MCPToolResultRecords collectRecords(String fullCmd, CommandSpec spec) {
        var records = new ArrayList<JsonNode>();
        var result = collectRecords(fullCmd, records::add, spec);
        return MCPToolResultRecords.from(result, records);
    }
    
    private static final FcliCommandExecutorFactory.FcliCommandExecutorFactoryBuilder createBuilder(String fullCmd, CommandSpec spec) {
        return FcliCommandExecutorFactory.builder()
            .cmd(fullCmd)
            .defaultOptionsIfNotPresent(collectMcpDefaultOptions(spec))
            .onFail(r->{})
            .stderrOutputType(OutputType.collect);           
    }

    private static final Map<String,String> collectMcpDefaultOptions(CommandSpec spec) {
        if ( spec==null ) { return Map.of(); }
        var result = new LinkedHashMap<String,String>();
        spec.options().forEach(o->{
            var defVal = ReflectionHelper.getAnnotationValue(o.userObject(), MCPDefaultValue.class, MCPDefaultValue::value, ()->null);
            if ( StringUtils.isBlank(defVal) ) { return; }
            var name = o.longestName();
            if ( name!=null ) { result.put(name, defVal); }
        });
        return result;
    }
}
