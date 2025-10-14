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
package com.fortify.cli.common.action.runner.processor.writer;

import java.util.Arrays;

import com.fortify.cli.common.action.model.ActionStepWithWriter;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.FcliActionStepException;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.output.writer.record.RecordWriterStyle;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ActionStepRecordWriterFactory {
    public static final IRecordWriter createWriter(ActionRunnerContext ctx, ActionRunnerVars vars, ActionStepWithWriter withWriter) {
        var config = new WithWriterConfig(ctx, vars, withWriter);
        return createStandardWriter(config);
    }

    private static final IRecordWriter createStandardWriter(WithWriterConfig config) {
        return config.getFactory().createWriter(ActionStepRecordWriterConfigFactory.createRecordWriterConfig(config));
    }
    
    @Getter
    static final class WithWriterConfig {
        private final ActionRunnerContext ctx;
        private final ActionRunnerVars vars;
        private final RecordWriterFactory factory;
        private final String to;
        private final RecordWriterStyle style;
        private final String recordWriterArgs;
        
        public WithWriterConfig(ActionRunnerContext ctx, ActionRunnerVars vars, ActionStepWithWriter withWriter) {
            this.ctx = ctx;
            this.vars = vars;
            this.factory = getFactory(vars.eval(withWriter.getType(), String.class));
            this.to = vars.eval(withWriter.getTo(), String.class);
            this.style = getStyle(vars, withWriter);
            this.recordWriterArgs = vars.eval(withWriter.getTypeArgs(), String.class);
        }

        private RecordWriterStyle getStyle(ActionRunnerVars vars, ActionStepWithWriter withWriter) {
            var styleElementsString = vars.eval(withWriter.getStyle(), String.class);
            return RecordWriterStyle.apply(styleElementsString==null?null:styleElementsString.split("[\\s,]+"));
        }

        private static final RecordWriterFactory getFactory(String type) {
            return Arrays.stream(RecordWriterFactory.values())
                    .filter(e->e.toString().equalsIgnoreCase(type))
                    .findFirst()
                    .orElseThrow(()->new FcliActionStepException("Unknown writer type: "+type));
        }   
    }
}
