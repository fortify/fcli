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
package com.fortify.cli.common.action.runner.processor.writer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.ActionStepWithWriter;
import com.fortify.cli.common.action.model.FcliActionValidationException;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.processor.writer.record.IRecordWriter;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterFactory;
import com.fortify.cli.common.json.JsonHelper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ActionStepWriterFactory {
    public static final IRecordWriter createWriter(ActionRunnerContext ctx, ActionRunnerVars vars, ActionStepWithWriter withWriter) {
        var type = vars.eval(withWriter.getType(), String.class);
        var to = vars.eval(withWriter.getTo(), String.class);
        if ( to.startsWith("var.json:") ) {
            return createJsonVarRecordWriter(vars, type, to.replaceAll("^var.json:", ""));
        } else {
            return createStandardWriter(ctx, vars, withWriter, type, to);
        }
    }

    private static final IRecordWriter createStandardWriter(ActionRunnerContext ctx, ActionRunnerVars vars,
            ActionStepWithWriter withWriter, String type, String to) {
        Map<String,String> options = withWriter.getOptions()==null ? null : withWriter.getOptions().entrySet().stream()
                .collect(HashMap::new, (map,e)->map.put(e.getKey(), vars.eval(e.getValue(), String.class)), HashMap::putAll);
        var config = ActionStepWriterConfigFactory.createRecordWriterConfig(ctx, vars, to, options);
        return Arrays.stream(RecordWriterFactory.values())
                .filter(e->e.toString().equalsIgnoreCase(type))
                .findFirst()
                .map(e->e.createWriter(config))
                .orElseThrow(()->new FcliActionValidationException("Unknown writer type: "+type));
    }
    
    private static final IRecordWriter createJsonVarRecordWriter(ActionRunnerVars vars, String type, String varName) {
        if ( !type.equals("json") ) {
            throw new FcliActionValidationException("'to: var.json' can only be used with 'type: json'");
        } else {
            return new FcliActionJsonVarRecordWriter(vars, varName);
        }
    }
    
    @RequiredArgsConstructor
    private static final class FcliActionJsonVarRecordWriter implements IRecordWriter {
        private final ActionRunnerVars vars;
        private final String varName;
        private final ArrayNode result = JsonHelper.getObjectMapper().createArrayNode();
        
        @Override
        public void append(ObjectNode node) {
            result.add(node);
        }
        @Override
        public void close() {
            vars.set(varName, result);
        }
    }
}
