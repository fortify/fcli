/*
 * Copyright 2021-2026 Open Text.
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
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStepRunFcliEntry;
import com.fortify.cli.common.action.model.ActionStepRunFcliEntry.ActionStepFcliForEachDescriptor;
import com.fortify.cli.common.action.model.FcliActionValidationException;
import com.fortify.cli.common.action.runner.ActionRunnerContextLocal;
import com.fortify.cli.common.action.runner.FcliActionStepException;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory.FcliCommandExecutor;
import com.fortify.cli.common.cli.util.StdioHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.common.util.OutputHelper.Result;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorRunFcli extends AbstractActionStepProcessorMapEntries<String, ActionStepRunFcliEntry> {
    // TODO Restructure vars.set calls across success/skipped/exception/... to reduce code duplication and making sure all relevant variables are set
    private static final String FMT_SUCCESS = "%s.success";
    private static final String FMT_FAILED = "%s.failed";
    private static final String FMT_STATUS = "%s.status";
    private static final String FMT_STATUS_REASON = "%s.statusReason";
    private static final String FMT_SKIPPED = "%s.skipped";
    private static final String FMT_SKIP_REASON = "%s.skipReason";
    private static final String FMT_DEPENDENCY_SKIP_REASON = "%s.dependencySkipReason";
    private static final String FMT_EXIT_CODE = "%s.exitCode";
    private static final String FMT_RECORDS = "%s.records";
    private static final String FMT_METADATA = "%s.metadata";
    private static final String FMT_STDOUT = "%s.stdout";
    private static final String FMT_STDERR = "%s.stderr";
    
    private final ActionRunnerContextLocal ctx;
    private final LinkedHashMap<String,ActionStepRunFcliEntry> map;
    
    /**
     * Override to disable default error handling - run.fcli has its own specialized
     * on.fail/on.success handling via callback methods.
     */
    @Override
    protected boolean shouldUseDefaultErrorHandling(Map.Entry<String, ActionStepRunFcliEntry> entry) {
        return false; // Use existing specialized handling
    }
    
    @Override
    protected final void process(String name, ActionStepRunFcliEntry entry) {
        if ( isSkipped(entry) ) { return; }
        logStatus(entry, "START");
        var cmd = getVars().eval(entry.getCmd(), String.class);
        ctx.getProgressWriter().writeProgress("Executing fcli %s",
                // Replace all whitespace (including newlines) with single space
                //   to avoid errors on single-line progress writer
                // Remove duplicate 'fcli' if included in cmd,
                asSingleLineString(cmd.replaceAll("^fcli ", "")));
        var recordConsumer = createFcliRecordConsumer(entry);
        var cmdExecutor = createCmdExecutor(entry, cmd, recordConsumer);
        if ( cmdExecutor!=null ) {
            if ( recordConsumer!=null && !cmdExecutor.canCollectRecords() ) {
                throw new FcliActionValidationException("Can't use records.for-each on fcli command: "+cmd);
            }
            cmdExecutor.execute();
        }
    }

    private FcliCommandExecutor createCmdExecutor(ActionStepRunFcliEntry entry, String cmd, FcliRecordConsumer recordConsumer) {
        var stdoutOutputType = getOutputTypeOrDefault(entry.getStdoutOutputType(), recordConsumer==null ? OutputType.show : OutputType.suppress );
        var stderrOutputType = getOutputTypeOrDefault(entry.getStderrOutputType(), OutputType.show );
        var metadataHolder = new AtomicReference<ObjectNode>();
        try {
            return FcliCommandExecutorFactory.builder()
                    .cmd(cmd)
                    .progressOptionValueIfNotPresent(ctx.getConfig().getProgressWriter().type())
                    .defaultOptionsIfNotPresent(ctx.getConfig().getDefaultFcliRunOptions())
                    .stdoutOutputType(stdoutOutputType)
                    .stderrOutputType(stderrOutputType)
                    .onResult(r->onFcliResult(entry, recordConsumer, metadataHolder, r))
                    .onSuccess(r->onFcliSuccess(entry))
                    .onException(e->onFcliException(entry, e))
                    .onFail(r->onFcliFail(entry, recordConsumer, r))
                    .recordConsumer(recordConsumer)
                    .metadataConsumer(metadataHolder::set)
                    .build().create();
        } catch ( Exception e ) {
            onFcliException(entry, e);
            return null;
        }
    }

    private final boolean isSkipped(ActionStepRunFcliEntry entry) {
        var plainSkipMessage = getSkipMessage(entry);
        if ( StringUtils.isBlank(plainSkipMessage) ) { 
            getVars().set(String.format(FMT_SKIPPED, entry.getKey()), BooleanNode.FALSE);
            getVars().set(String.format(FMT_SKIP_REASON, entry.getKey()), NullNode.instance);
            // status & dependencySkipReason are set by setFcliVars after execution
            return false; 
        } else {
            plainSkipMessage = plainSkipMessage.trim();
            var fullSkipMessage = String.format("SKIPPED: %s: %s", entry.getKey(), plainSkipMessage);
            var dependencySkipReason = String.format("%s was skipped", entry.getKey());
            LOG.info(fullSkipMessage);
            System.out.println(fullSkipMessage);
            getVars().set(String.format(FMT_STATUS, entry.getKey()), new TextNode("SKIPPED"));
            getVars().set(String.format(FMT_SKIPPED, entry.getKey()), BooleanNode.TRUE);
            getVars().set(String.format(FMT_SKIP_REASON, entry.getKey()), new TextNode(plainSkipMessage)); // Legacy; see statusReason below
            getVars().set(String.format(FMT_STATUS_REASON, entry.getKey()), new TextNode(plainSkipMessage));
            getVars().set(String.format(FMT_DEPENDENCY_SKIP_REASON, entry.getKey()), new TextNode(dependencySkipReason));
            setGroupVars(entry);
            return true;
        }
    }

    private final String getSkipMessage(ActionStepRunFcliEntry entry) {
        var skipIfReason = entry.getSkipIfReason();
        if ( skipIfReason!=null ) {
            return skipIfReason.stream()
                    .filter(Objects::nonNull)
                    .map(t->getVars().eval(t, String.class))
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
        } else {
            var msg = String.format("'%s' returned non-zero exit code %s", 
                    getFcliCmdWithoutOpts(fcli), result.getExitCode());
            if ( failOnError(fcli) ) {
                throw new FcliActionStepException(msg).exitCode(result.getExitCode());
            } else {
                getVars().set(String.format(FMT_STATUS_REASON, fcli.getKey()), new TextNode(msg));
            }
        }
    }
    
    private final boolean failOnError(ActionStepRunFcliEntry fcli) {
        var statusCheckFromConfig = ctx.getConfig().getAction().getConfig().getRunFcliStatusCheckDefault();
        var statusCheck = fcli.getStatusCheck();
        statusCheck = statusCheck==null ? statusCheckFromConfig : statusCheck;
        boolean statusCheckPrimitive = statusCheck==null ? fcli.getOnFail()==null : statusCheck;
        return statusCheckPrimitive;
    }

    private String getFcliCmdWithoutOpts(ActionStepRunFcliEntry fcli) {
        var result = getVars().eval(fcli.getCmd(), String.class).replaceAll("\s+-.*", "");
        return result.startsWith("fcli") ? result : String.format("fcli %s", result);
    }

    private void onFcliException(ActionStepRunFcliEntry fcli, Throwable t) {
        var name = fcli.getKey();
        // Set generic lastException* and ${name}_exception* variables (with .message, .type, .httpStatus sub-properties)
        setGenericExceptionVars(t, name, getVars()::set);
        var msg = t.getMessage();
        msg = msg==null 
            ? "Fcli command terminated with an exception"
            : String.format("Exception: %s", msg.split("\n", 2)[0]); // Get first line only
        getVars().set(String.format(FMT_STATUS_REASON, name), new TextNode(msg));
        getVars().set(String.format(FMT_EXIT_CODE, name), new IntNode(1));
        getVars().set(String.format(FMT_STATUS, name), "FAILED");
        getVars().set(String.format(FMT_SUCCESS, name), BooleanNode.FALSE);
        getVars().set(String.format(FMT_FAILED, name), BooleanNode.TRUE);
        getVars().set(String.format(FMT_DEPENDENCY_SKIP_REASON, name), new TextNode(String.format("%s failed", name)));
        setGroupVars(fcli);
        if ( failOnError(fcli) ) {
            throw new FcliActionStepException("Fcli command terminated with an exception", t);
        } else {
            t.printStackTrace();
        }
    }
    
    private void onFcliResult(ActionStepRunFcliEntry fcli, FcliRecordConsumer recordConsumer, AtomicReference<ObjectNode> metadataHolder, Result result) {
        setResultVars(fcli, recordConsumer, metadataHolder, result);
        logStatus(fcli, result);
    }

    private void setResultVars(ActionStepRunFcliEntry fcli, FcliRecordConsumer recordConsumer, AtomicReference<ObjectNode> metadataHolder, Result result) {
        var name = fcli.getKey();
        getVars().set(String.format(FMT_RECORDS, name), recordConsumer!=null ? recordConsumer.getRecords() : JsonHelper.getObjectMapper().createArrayNode());
        getVars().set(String.format(FMT_METADATA, name), metadataHolder.get()!=null ? metadataHolder.get() : NullNode.instance);
        getVars().set(String.format(FMT_STDOUT, name), result.getOut());
        getVars().set(String.format(FMT_STDERR, name), result.getErr());
        getVars().set(String.format(FMT_EXIT_CODE, name), new IntNode(result.getExitCode()));
        getVars().set(String.format(FMT_STATUS, name), getStatusString(result));
        getVars().set(String.format(FMT_SUCCESS, name), BooleanNode.valueOf(result.getExitCode()==0));
        getVars().set(String.format(FMT_FAILED, name), BooleanNode.valueOf(result.getExitCode()!=0));
        getVars().set(String.format(FMT_DEPENDENCY_SKIP_REASON, name), result.getExitCode()==0 ? NullNode.instance : new TextNode(String.format("%s failed", name)));
        setGroupVars(fcli);
    }
    
    private final void setGroupVars(ActionStepRunFcliEntry entry) {
        var groupFromConfig = ctx.getConfig().getAction().getConfig().getRunFcliGroupDefault();
        var group = entry.getGroup();
        group = StringUtils.isNotBlank(group) ? group : groupFromConfig;
        if ( StringUtils.isNotBlank(group) ) {
            var key = entry.getKey();
            getVars().set(String.format("%s.%s", group, key), getVars().eval(String.format("#root['%s']", key), ObjectNode.class));
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
            System.out.println(msg);
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
                // If fcliForEach is specified, FcliCommandExecutor will be configured to suppress stdout,
                // and users may optional suppress stderr. However, output suppression should only apply
                // to the fcli command itself, and not to any processing done for each record. As this
                // method is called from within the context of FcliCommandExecutor, we need to temporarily
                // restore stdout/stderr in order to allow any processing steps to write to stdout/stderr.
                try (var x = new TempRestoreOutput()) {
                    continueDoSteps = processForEachStepNode(fcliForEach, record);
                }
            }
        }
        
        private final class TempRestoreOutput implements AutoCloseable {
            private final PrintStream poppedOut = StdioHelper.popOut();
            private final PrintStream poppedErr = StdioHelper.popErr();
            @Override
            public void close() {
                StdioHelper.pushErr(poppedErr);
                StdioHelper.pushOut(poppedOut);
            }
        }
    }
    
}