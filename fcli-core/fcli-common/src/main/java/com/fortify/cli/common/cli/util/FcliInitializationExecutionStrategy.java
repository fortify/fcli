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
import java.lang.reflect.Field;

import com.fortify.cli.common.cli.mixin.ICommandAware;
import com.fortify.cli.common.log.LogMaskHelper;
import com.fortify.cli.common.log.LogMaskSource;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.rest.unirest.IUnirestContextAware;
import com.fortify.cli.common.rest.unirest.UnirestContext;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.common.util.JavaHelper;
import com.fortify.cli.common.util.NonClosingPrintStream;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionStrategy;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParseResult;

/**
 * Combined execution strategy performing command initialization formerly handled by
 * AbstractRunnableCommand::initialize(), together with UnirestContext lifecycle management.
 *
 * Responsibilities executed exactly once per leaf command invocation:
 * <ul>
 *   <li>Register log masks for option values</li>
 *   <li>Inject CommandSpec into all ICommandAware mixins</li>
 *   <li>Log fcli version and arguments</li>
 *   <li>Create & inject UnirestContext into all IUnirestContextAware components</li>
 * </ul>
 * A single iteration over all user objects is used to inject both CommandSpec and UnirestContext
 * for performance.
 */
@Slf4j
public final class FcliInitializationExecutionStrategy implements IExecutionStrategy {
    private final IExecutionStrategy delegate;

    public FcliInitializationExecutionStrategy(IExecutionStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public int execute(ParseResult parseResult) throws CommandLine.ExecutionException {
        var leaf = getLeafParseResult(parseResult);
        var leafSpec = leaf.commandSpec();
        // Perform initialization (command spec injection & logging) and Unirest context management
        try (var outHandler = new NonClosingOutHandler(); var unirestContext = new UnirestContext()) {
            log.debug("Starting command execution with {}: {}", unirestContext.identity(), leafSpec.qualifiedName());
            initializeCommand(leafSpec, unirestContext);
            var result = delegate.execute(parseResult);
            log.debug("Finished command execution with {}: {}", unirestContext.identity(), leafSpec.qualifiedName());
            return result;
        }
    }

    private void initializeCommand(CommandSpec commandSpec, UnirestContext unirestContext) {
        // Register log masks for option values (cheap operation separate from main iteration)
        registerLogMasks(commandSpec);
        // Log version & args before actual command execution
        logVersionAndArgs(commandSpec);
        // Single pass over all user objects to inject CommandSpec and UnirestContext
        FcliCommandSpecHelper.getAllUserObjectsStream(commandSpec)
            .forEach(o -> {
                if ( o instanceof ICommandAware cAware ) {
                    cAware.setCommandSpec(commandSpec);
                }
                if ( o instanceof IUnirestContextAware uAware ) {
                    log.debug("Injecting {} into: {}", unirestContext.identity(), o.getClass().getName());
                    uAware.setUnirestContext(unirestContext);
                }
            });
    }

    private ParseResult getLeafParseResult(ParseResult pr) {
        while (pr.subcommand() != null) { pr = pr.subcommand(); }
        return pr;
    }

    private static void logVersionAndArgs(CommandSpec commandSpec) {
        log.info("fcli version: {} ", FcliBuildProperties.INSTANCE.getFcliBuildInfo());
        log.info("fcli arguments: {} {} ", commandSpec.qualifiedName(), commandSpec.commandLine().getParseResult().expandedArgs());
    }

    private static void registerLogMasks(CommandSpec commandSpec) {
        for ( var option : commandSpec.options() ) {
            var value = option.getValue();
            if ( value!=null ) {
                JavaHelper.as(option.userObject(), Field.class)
                    .ifPresent(field->registerLogMask(field, value));
            }
        }
    }

    private static void registerLogMask(Field field, Object value) {
        LogMaskHelper.INSTANCE.registerValue(field.getAnnotation(MaskValue.class), LogMaskSource.CLI_OPTION, value);
    }
    
    
    private static final class NonClosingOutHandler implements AutoCloseable {
        private final PrintStream orgOut;
        private final PrintStream orgErr;
        
        public NonClosingOutHandler() {
            // Avoid any fcli code from closing stdout/stderr streams
            log.debug("Installing NonClosingPrintStream for stdout/stderr");
            this.orgOut = System.out;
            this.orgErr = System.err;
            System.setOut(new NonClosingPrintStream("System.out", orgOut));
            System.setErr(new NonClosingPrintStream("System.err", orgErr));
        }
        
        @Override
        public void close() {
            System.setOut(orgOut);
            System.setErr(orgErr);
            log.debug("Restored original stdout/stderr PrintStreams");
        }
    }
}
