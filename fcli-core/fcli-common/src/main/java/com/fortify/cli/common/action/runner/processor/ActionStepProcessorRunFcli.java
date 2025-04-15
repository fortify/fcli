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
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStepRunFcliEntry;
import com.fortify.cli.common.action.model.ActionStepRunFcliEntry.ActionStepFcliForEachDescriptor;
import com.fortify.cli.common.action.model.FcliActionValidationException;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.FcliActionStepException;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.json.JsonHelper;
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
        logStatus(entry, "START");
        var cmd = vars.eval(entry.getCmd(), String.class);
        ctx.getProgressWriter().writeProgress("Executing fcli %s", cmd.replaceAll("^fcli ", ""));
        var recordConsumer = createFcliRecordConsumer(entry);
        var stdoutOutputType = getOutputTypeOrDefault(entry.getStdoutOutputType(), recordConsumer==null ? OutputType.show : OutputType.suppress );
        var stderrOutputType = getOutputTypeOrDefault(entry.getStderrOutputType(), OutputType.show );
        var cmdExecutor = FcliCommandExecutorFactory.builder()
                .cmd(cmd)
                .progressOptionValueIfNotPresent(ctx.getConfig().getProgressWriterFactory().getType().name())
                .defaultOptionsIfNotPresent(ctx.getConfig().getDefaultFcliRunOptions())
                .stdout(ctx.getStdout())
                .stderr(ctx.getStderr())
                .stdoutOutputType(stdoutOutputType)
                .stderrOutputType(stderrOutputType)
                .onResult(r->onFcliResult(entry, recordConsumer, r))
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
        var plainSkipMessage = getSkipMessage(entry);
        if ( StringUtils.isBlank(plainSkipMessage) ) { 
            vars.set(String.format("%s.skipped", entry.getKey()), BooleanNode.FALSE);
            vars.set(String.format("%s.skipReason", entry.getKey()), NullNode.instance);
            // status & dependencySkipReason are set by setFcliVars after execution
            return false; 
        } else {
            plainSkipMessage = plainSkipMessage.trim();
            var fullSkipMessage = String.format("SKIPPED: %s: %s", entry.getKey(), plainSkipMessage);
            var dependencySkipReason = String.format("%s was skipped", entry.getKey());
            LOG.info(fullSkipMessage);
            ctx.getStdout().println(fullSkipMessage);
            vars.set(String.format("%s.skipped", entry.getKey()), BooleanNode.TRUE);
            vars.set(String.format("%s.skipReason", entry.getKey()), new TextNode(plainSkipMessage));
            vars.set(String.format("%s.status", entry.getKey()), new TextNode("SKIPPED"));
            vars.set(String.format("%s.dependencySkipReason", entry.getKey()), new TextNode(dependencySkipReason));
            setGroupVars(entry);
            return true;
        }
    }

    private final String getSkipMessage(ActionStepRunFcliEntry entry) {
        var skipIfReason = entry.getSkipIfReason();
        if ( skipIfReason!=null ) {
            return skipIfReason.stream()
                    .filter(Objects::nonNull)
                    .map(t->vars.eval(t, String.class))
                    .filter(StringUtils::isNotBlank)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
    
    private void onFcliSuccess(ActionStepRunFcliEntry fcli) {
        if ( fcli.getOnSuccess()!=null ) {
            processSteps(fcli.getOnSuccess());
        }
    }

    private void onFcliFail(ActionStepRunFcliEntry fcli, FcliRecordConsumer recordConsumer, Result result) {
        var onFail = fcli.getOnFail();
        if ( onFail!=null ) {
            processSteps(onFail);
        }
        var statusCheckFromConfig = ctx.getConfig().getAction().getConfig().getRunFcliStatusCheckDefault();
        var statusCheck = fcli.getStatusCheck();
        statusCheck = statusCheck==null ? statusCheckFromConfig : statusCheck;
        boolean statusCheckPrimitive = statusCheck==null ? onFail==null : statusCheck;
        if ( statusCheckPrimitive ) {
            throw new FcliActionStepException(String.format("'%s' returned non-zero exit code %s", 
                    getFcliCmdWithoutOpts(fcli), result.getExitCode())).exitCode(result.getExitCode());
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
    
    private void onFcliResult(ActionStepRunFcliEntry fcli, FcliRecordConsumer recordConsumer, Result result) {
        setResultVars(fcli, recordConsumer, result);
        logStatus(fcli, result);
    }

    private void setResultVars(ActionStepRunFcliEntry fcli, FcliRecordConsumer recordConsumer, Result result) {
        var name = fcli.getKey();
        vars.set(name+".records", recordConsumer!=null ? recordConsumer.getRecords() : JsonHelper.getObjectMapper().createArrayNode());
        vars.set(name+".stdout", result.getOut());
        vars.set(name+".stderr", result.getErr());
        vars.set(name+".exitCode", new IntNode(result.getExitCode()));
        vars.set(name+".status", getStatusString(result));
        vars.set(name+".success", BooleanNode.valueOf(result.getExitCode()==0));
        vars.set(name+".failed", BooleanNode.valueOf(result.getExitCode()!=0));
        vars.set(name+".dependencySkipReason", result.getExitCode()==0 ? NullNode.instance : new TextNode(String.format("%s failed", name)));
        setGroupVars(fcli);
    }
    
    private final void setGroupVars(ActionStepRunFcliEntry entry) {
        var groupFromConfig = ctx.getConfig().getAction().getConfig().getRunFcliGroupDefault();
        var group = entry.getGroup();
        group = StringUtils.isNotBlank(group) ? group : groupFromConfig;
        if ( StringUtils.isNotBlank(group) ) {
            var key = entry.getKey();
            vars.set(String.format("%s.%s", group, key), vars.eval(String.format("#root['%s']", key), ObjectNode.class));
        }
    }
    
    private void logStatus(ActionStepRunFcliEntry fcli, Result result) {
        logStatus(fcli, getStatusString(result));
    }
    
    private void logStatus(ActionStepRunFcliEntry fcli, String status) {
        var msg = String.format("%s: %s", status, fcli.getKey());
        LOG.info(msg);
        boolean statusLogPrimitive = isLogStatus(fcli);
        if ( statusLogPrimitive ) {
            ctx.getStdout().println(msg);
        }
    }

    private boolean isLogStatus(ActionStepRunFcliEntry fcli) {
        var statusLogFromConfig = ctx.getConfig().getAction().getConfig().getRunFcliStatusLogDefault();
        var statusLog = fcli.getStatusLog();
        statusLog = statusLog==null ? statusLogFromConfig : statusLog;
        boolean statusLogPrimitive = statusLog==null ? false : statusLog;
        return statusLogPrimitive;
    }
    
    private String getStatusString(Result result) {
        return result.getExitCode()==0 ? "SUCCESS" : "FAILED";
    }

    private OutputType getOutputTypeOrDefault(OutputType outputType, OutputType _default) {
        return outputType!=null ? outputType : _default;
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
