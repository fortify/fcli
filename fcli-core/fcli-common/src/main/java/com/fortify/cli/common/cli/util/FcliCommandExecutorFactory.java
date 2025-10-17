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
package com.fortify.cli.common.cli.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliExecutionExceptionHandler;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.cmd.IRecordCollectionSupport;
import com.fortify.cli.common.util.OutputHelper;
import com.fortify.cli.common.util.OutputHelper.OutputType;
import com.fortify.cli.common.util.OutputHelper.Result;
import com.fortify.cli.common.variable.FcliVariableHelper;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import picocli.CommandLine;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;

@Builder @Data
public final class FcliCommandExecutorFactory { 
    @NonNull private final String cmd;
    private final Consumer<ObjectNode> recordConsumer;
    @Builder.Default private final OutputType stdoutOutputType = OutputType.show;
    @Builder.Default private final OutputType stderrOutputType = OutputType.show;
    private final PrintStream stdout;
    private final PrintStream stderr;
    private final Consumer<Result> onResult; // Always executed if fcli command didn't throw exception
    private final Consumer<Result> onSuccess; // Executed after onResult, if 0 exit code
    private final Consumer<Result> onFail; // Executed after onResult, if non-zero exit code
    private final Consumer<Throwable> onException;
    public final String progressOptionValueIfNotPresent; // TODO Should we integrate this into defaultOptionsIfNotPresent?
    public final Map<String, String> defaultOptionsIfNotPresent;
    
    private static final CommandLine getRootCommandLine() {
        return FcliCommandSpecHelper.getRootCommandLine();
    }
    
    public final FcliCommandExecutor create() {
        if ( StringUtils.isBlank(cmd) ) {
            throw new FcliSimpleException("Fcli command to be run may not be blank");
        }
        return new FcliCommandExecutor();
    }
    
    public final class FcliCommandExecutor {
        private static final Logger LOG = LoggerFactory.getLogger(FcliCommandExecutor.class);
        private final String[] resolvedArgs;
        private final CommandSpec replicatedLeafCommandSpec;
        private Result parseErrorResult = null;
        
        public FcliCommandExecutor() {
            this.resolvedArgs = FcliVariableHelper.resolveVariables(parseArgs(cmd));
            this.replicatedLeafCommandSpec = replicateLeafCommandSpecWithParents(parseArgs(resolvedArgs));
        }

        private ParseResult parseArgs(String[] resolvedArgs) {
            try {
                return getRootCommandLine().parseArgs(resolvedArgs);
            } catch ( ParameterException e ) {
                this.parseErrorResult = call(()->handleParseException(resolvedArgs, e));
                return null;
            }
        }

        private int handleParseException(String[] resolvedArgs, ParameterException e) {
            try {
                return getRootCommandLine().getParameterExceptionHandler().handleParseException(e, resolvedArgs);
            } catch ( Exception e2 ) {
                return FcliExecutionExceptionHandler.INSTANCE.handleException(e2, getRootCommandLine());
            }
        }

        public final Result execute() {
            if ( parseErrorResult!=null ) { return parseErrorResult; }
            if ( recordConsumer!=null && canCollectRecords() ) { setPerCommandRecordConsumer(); }
            return call(()->_execute());
        }

        private Result call(Callable<Integer> f) {
            Result result = null;
            try {
                result = OutputHelper.builder()
                        .stderr(stderr).stderrType(stderrOutputType)
                        .stdout(stdout).stdoutType(stdoutOutputType)
                        .build().call(f);
            } catch ( Throwable t ) {
                if ( t instanceof ExecutionException ) {
                    t = t.getCause();
                }
                consume(t, onException, this::rethrowAsRuntimeException);
                return new Result(999, "", "");
            }
            // We want result processing to be outside of the try/catch block above,
            // as any of these may throw an exception that we don't want to catch in
            // the catch-block above.
            consume(result, onResult, null);
            if ( result.getExitCode()==0 ) {
                consume(result, onSuccess, null);
            } else {
                consume(result, onFail, this::throwExceptionOnNonZeroExitCode);
            }
            return result;
        }
    
        private final int _execute() throws Exception {
            var args = addOptionsIfNotPresent(resolvedArgs);
            if ( LOG.isDebugEnabled() ) {
                LOG.debug("Executing '{}'", String.join(" ", args));
            }
            return createCommandLine().execute(args);
        }

        private String[] addOptionsIfNotPresent(String[] resolvedArgs) {
            var result = new ArrayList<>(Arrays.asList(resolvedArgs));
            addOptionIfNotPresent(result, "--progress", progressOptionValueIfNotPresent); // TODO Integrate with below?
            if ( defaultOptionsIfNotPresent!=null ) {
                for ( var e : defaultOptionsIfNotPresent.entrySet() ) {
                    addOptionIfNotPresent(result, e.getKey(), e.getValue());
                }
            }
            return result.toArray(String[]::new);
        }
        
        private final void addOptionIfNotPresent(List<String> resolvedArgs, String optionName, String value) {
            if ( StringUtils.isNotBlank(value) && hasOption(optionName) && !hasOptionValue(resolvedArgs, optionName) ) {
                resolvedArgs.add(String.format("%s=%s", optionName, value));
            }
        }
        
        private final boolean hasOptionValue(List<String> resolvedArgs, String optionName) {
            // TODO Do we manually check in resolvedArgs, or can we check using replicatedLeafCommandSpec.optionsMap()?
            return resolvedArgs.stream().anyMatch(s->s.equals(optionName) || s.startsWith(optionName+"="));
        }
        
        private final boolean hasOption(String optionName) {
            return replicatedLeafCommandSpec.optionsMap().containsKey(optionName);
        }

        private CommandLine createCommandLine() {
            var cl = new CommandLine(replicatedLeafCommandSpec.root());
            cl.setExecutionExceptionHandler(FcliExecutionExceptionHandler.INSTANCE);
            FcliExecutionStrategyFactory.configureCommandLine(cl);
            return cl;
        }

        public final boolean canCollectRecords() {
            return FcliCommandSpecHelper.canCollectRecords(replicatedLeafCommandSpec);
        }

        private void setPerCommandRecordConsumer() {
            var userObj = replicatedLeafCommandSpec.userObject();
            if ( userObj instanceof IRecordCollectionSupport ) {
                ((IRecordCollectionSupport)userObj).setRecordConsumer(recordConsumer, stdoutOutputType!=OutputType.show);
            }
        }
        
        private <T> void consume(T value, Consumer<T> consumer, Consumer<T> defaultConsumer) {
            if ( consumer!=null ) {
                consumer.accept(value);
            } else if ( defaultConsumer!=null ) {
                defaultConsumer.accept(value);
            }
        }
        
        private void rethrowAsRuntimeException(Throwable t) {
            throw new FcliSimpleException("Fcli command threw an exception", t);
        }
        
        private final void throwExceptionOnNonZeroExitCode(Result r) {
            throw new FcliSimpleException("Fcli command terminated with non-zero exit code "+r.getExitCode());
        }
        
        // We want to replicate the CommandSpec with new command instances, at least for the 
        // leaf command, to make sure that each invocation uses a separate instance of the 
        // leaf command. Otherwise, instance variables might have the wrong values, somewhat
        // similar to similar to https://vulncat.fortify.com/en/detail?category=Race%20Condition&subcategory=Singleton%20Member%20Field#Java%2fJS
        private static final CommandSpec replicateLeafCommandSpecWithParents(ParseResult parseResult) {
            if ( parseResult==null ) { return null; }
            // This is the safest approach, but causes picocli to recreate the
            // full command tree through reflection, which is far from optimal
            // as we already know which command to execute.
            //return CommandSpec.forAnnotatedObject(parseResult.commandSpec().userObject().getClass());
            
            // More optimized approach, just walking the requested command tree 
            CommandSpec replicatedSpec = null;
            for ( var pr = parseResult ; pr!=null ; pr = pr.subcommand() ) {
                var newSpec = replicateSpecForSubcommand(pr);
                if ( replicatedSpec!=null ) {
                    replicatedSpec.addSubcommand(newSpec.name(), newSpec);
                }
                replicatedSpec = newSpec;
            }
            return replicatedSpec;
        }

        private static CommandSpec replicateSpecForSubcommand(ParseResult pr) {
            var orgSpec = pr.commandSpec();
            if ( !pr.hasSubcommand() ) {
                // Create new spec from leaf command class
                return CommandSpec.forAnnotatedObject(orgSpec.userObject().getClass());
            } else {
                // Create shallow copy of container command spec
                var newSpec = CommandSpec.wrapWithoutInspection(orgSpec.userObject());
                newSpec.name(orgSpec.name());
                newSpec.aliases(orgSpec.aliases());
                newSpec.resourceBundle(orgSpec.resourceBundle());
                return newSpec;
            }
        }
        
        private static final String[] parseArgs(String args) {
            var argsWithoutFcli = args.replaceFirst("^fcli\s+", "");
            List<String> argsList = new ArrayList<String>();
            Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(argsWithoutFcli);
            while (m.find()) { argsList.add(m.group(1).replace("\"", "")); }
            return argsList.toArray(String[]::new);
        }
    }
}