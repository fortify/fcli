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
package com.fortify.cli.common.action.runner.processor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStep;
import com.fortify.cli.common.action.model.ActionStepRunFcliEntry;
import com.fortify.cli.common.action.model.ActionStepRunFcliEntry.ActionStepFcliForEachDescriptor;
import com.fortify.cli.common.action.model.FcliActionValidationException;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.FcliActionStepException;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.common.util.OutputHelper.Result;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorRunFcli extends AbstractActionStepProcessorMapEntries<String, ActionStepRunFcliEntry> {
    private final ActionRunnerContext ctx;
    private final ActionRunnerVars vars;
    private final LinkedHashMap<String,ActionStepRunFcliEntry> map;
    
    @Override
    protected final void process(String name, ActionStepRunFcliEntry entry) {
        if ( isSkipped(entry) ) { return; }
        var cmd = vars.eval(entry.getCmd(), String.class);
        ctx.getProgressWriter().writeProgress("Executing fcli %s", cmd.replaceAll("^fcli ", ""));
        var recordConsumer = createFcliRecordConsumer(entry);
        var stdoutOutputType = getFcliOutputTypeOrDefault(entry.getStdoutOutputType(), recordConsumer==null ? OutputType.show : OutputType.suppress );
        var stderrOutputType = getFcliOutputTypeOrDefault(entry.getStderrOutputType(), OutputType.show );
        var cmdExecutor = FcliCommandExecutorFactory.builder()
                .cmd(cmd)
                .progressOptionValueIfNotPresent(ctx.getConfig().getProgressWriterFactory().getType().name())
                .defaultOptionsIfNotPresent(ctx.getConfig().getDefaultFcliRunOptions())
                .stdout(ctx.getStdout())
                .stderr(ctx.getStderr())
                .stdoutOutputType(stdoutOutputType)
                .stderrOutputType(stderrOutputType)
                .onResult(r->setFcliVars(entry, recordConsumer, r))
                .onSuccess(r->onFcliSuccess(entry))
                .onException(e->onFcliException(entry, e))
                .onFail(r->onFcliFail(entry, recordConsumer, r))
                .recordConsumer(recordConsumer).build().create();
        if ( recordConsumer!=null && !cmdExecutor.canCollectRecords() ) {
            throw new FcliActionValidationException("Can't use records.for-each on fcli command: "+cmd);
        }
        cmdExecutor.execute();
    }

    private final boolean isSkipped(ActionStepRunFcliEntry entry) {
        var skipIf = entry.getSkipIf();
        if ( skipIf==null ) { return false; }
        var skipMessageExpression = skipIf.entrySet().stream()
            .filter(this::isSkipped)
            .findFirst()
            .map(Map.Entry::getValue)
            .orElse(null);
        if ( skipMessageExpression!=null ) {
            var skipMessage = vars.eval(skipMessageExpression, String.class);
            var fullSkipMessage = String.format("SKIPPED: %s: %s", entry.getKey(), skipMessage.trim());
            LOG.debug(fullSkipMessage);
            ctx.getStdout().println(fullSkipMessage);
            return true;
        }
        return false;
    }
    
    private final boolean isSkipped(Map.Entry<TemplateExpression, TemplateExpression> entry) {
        return vars.eval(entry.getKey(), Boolean.class);
    }

    private void onFcliSuccess(ActionStepRunFcliEntry fcli) {
        if ( fcli.getOnSuccess()!=null ) {
            processSteps(fcli.getOnSuccess());
        }
    }

    private void onFcliFail(ActionStepRunFcliEntry fcli, FcliRecordConsumer recordConsumer, Result result) {
        setFcliVars(fcli, recordConsumer, result);
        ArrayList<ActionStep> onFail = fcli.getOnFail();
        if ( onFail==null ) {
            throw new FcliActionStepException(String.format("'%s' returned non-zero exit code %s", 
                    getFcliCmdWithoutOpts(fcli), result.getExitCode()));
        } else {
            processSteps(onFail);
        }
    }

    private String getFcliCmdWithoutOpts(ActionStepRunFcliEntry fcli) {
        var result = vars.eval(fcli.getCmd(), String.class).replaceAll("\s+-.*", "");
        return result.startsWith("fcli") ? result : String.format("fcli %s", result);
    }

    private void onFcliException(ActionStepRunFcliEntry fcli, Throwable t) {
        vars.set(fcli.getKey()+"_exception", new POJONode(t));
        // See comments on commented out onException in ActionStepRunFcli
        //List<ActionStep> onException = fcli.getOnException();
        //if ( onException==null ) {
        throw new FcliActionStepException("Fcli command terminated with an exception", t);
        //    throw t instanceof RuntimeException 
        //        ? (RuntimeException)t
        //        : new FcliActionStepException("Fcli command terminated with an exception", t);
        //} else {
        //    processSteps(onException);
        //}
    }
    
    private void setFcliVars(ActionStepRunFcliEntry fcli, FcliRecordConsumer recordConsumer, Result result) {
        var name = fcli.getKey();
        vars.set(name, recordConsumer!=null ? recordConsumer.getRecords() : JsonHelper.getObjectMapper().createArrayNode());
        vars.set(name+"_stdout", fcli.getStdoutOutputType()==OutputType.collect ? "" : result.getOut());
        vars.set(name+"_stderr", fcli.getStderrOutputType()==OutputType.collect ? "" : result.getErr());
        vars.set(name+"_exitCode", new IntNode(result.getExitCode()));
    }

    private OutputType getFcliOutputTypeOrDefault(OutputType outputType, OutputType _default) {
        return outputType==null ? _default : outputType;
    }

    private final FcliRecordConsumer createFcliRecordConsumer(ActionStepRunFcliEntry fcli) {
        var forEachRecord = fcli.getForEachRecord();
        var collectRecords = fcli.isCollectRecords();
        return forEachRecord!=null || collectRecords 
                ? new FcliRecordConsumer(forEachRecord, collectRecords)
                : null;
    }
    @RequiredArgsConstructor
    private class FcliRecordConsumer implements Consumer<ObjectNode> {
        private final ActionStepFcliForEachDescriptor fcliForEach;
        private final boolean collectRecords;
        
        @Getter(lazy=true) private final ArrayNode records = JsonHelper.getObjectMapper().createArrayNode();
        private boolean continueDoSteps = true;
        @Override
        public void accept(ObjectNode record) {
            if ( collectRecords ) {
                getRecords().add(record);
            }
            if ( continueDoSteps && fcliForEach!=null ) {
                continueDoSteps = processForEachStepNode(fcliForEach, record);
            }
        }
    }
    
}
