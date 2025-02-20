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

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.processor.writer.record.RecordWriterConfig;
import com.fortify.cli.common.action.runner.processor.writer.record.util.AbstractWriterWrapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionStepWriterConfigFactory {
    public static final RecordWriterConfig createRecordWriterConfig(ActionRunnerContext ctx, ActionRunnerVars vars, String to, Map<String, String> options) {
        return RecordWriterConfig.builder()
                .writerSupplier(()->createWriter(ctx, vars, to))
                .options(options)
                .build();
    }
    
    @SneakyThrows
    public static final Writer createWriter(ActionRunnerContext ctx, ActionRunnerVars vars, String target) {
        // TODO Add support for stdout/stderr, utilizing ctx to identify whether output should be delayed,
        //      and if so, add delayedConsoleWriterRunnables. If output is not delayed, we can use a non-closing
        //      writer wrapper to wrap System.out/System.err
        if ( target.startsWith("var.text:") ) { // Note that var.json: is handled in ActionStepWriterFactory
            return new FcliActionVariableWriter(vars, target.replaceAll("^var.text:", ""));
        } else {
            return new FileWriter(target);
        }
    }
    
    private static final class FcliActionVariableWriter extends AbstractWriterWrapper<StringWriter> {
        private final ActionRunnerVars vars;
        private final String varName;
        public FcliActionVariableWriter(ActionRunnerVars vars, String varName) {
            super(new StringWriter());
            this.vars = vars;
            this.varName = varName;
        }
        
        @Override
        public void close() throws IOException {
            var wrappee = getWrappee();
            wrappee.close();
            var value = wrappee.getBuffer().toString();
            vars.set(varName, new TextNode(value));
        }
        
    }
}
