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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.processor.writer.ActionStepRecordWriterFactory.WithWriterConfig;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.fortify.cli.common.output.writer.record.util.AbstractWriterWrapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionStepRecordWriterConfigFactory {
    public static RecordWriterConfig createRecordWriterConfig(WithWriterConfig config) {
        return RecordWriterConfig.builder()
                .writerSupplier(()->createWriter(config))
                .style(config.getStyle())
                .args(config.getRecordWriterArgs())
                .build();
    }
    
    @SneakyThrows
    public static final Writer createWriter(WithWriterConfig config) {
        var id = config.getId();
        var to = config.getTo();
        var ctx = config.getCtx();
        var vars = config.getVars();
        var filePathVarName = String.format("%s.filePath", id);
        if ( "stdout".equals(to) ) {
            return new OutputStreamWriter(ctx.getStdout());
        } else if ( "stderr".equals(to) ) {
            return new OutputStreamWriter(ctx.getStderr());
        } else if ( to.startsWith("var:") ) {
            return new FcliActionVariableWriter(vars, to.replaceAll("^var:", ""));
        } else if ( "temp".equals(to) ) {
            var f = File.createTempFile("fcli", "temp-writer");
            f.deleteOnExit();
            vars.set(filePathVarName, f.getAbsolutePath());
            return new FileWriter(f, StandardCharsets.UTF_8, true);
        } else {
            vars.set(filePathVarName, new File(to).getAbsolutePath());
            return new FileWriter(to, StandardCharsets.UTF_8, false);
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
