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
package com.fortify.cli.common.cli.util;

import java.lang.reflect.Field;

import com.fortify.cli.common.cli.mixin.ICommandAware;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.log.LogMaskHelper;
import com.fortify.cli.common.log.LogMaskSource;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.common.util.FcliDockerHelper;
import com.fortify.cli.common.util.JavaHelper;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionStrategy;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParseResult;

/**
 * Picocli execution strategy that initialises every command and owns the
 * {@link FcliExecutionContext} lifecycle for plain CLI invocations.
 *
 * <h3>Execution context lifecycle</h3>
 * <p>This strategy is the single place responsible for deciding whether a new
 * {@link FcliExecutionContext} must be created and who owns its lifetime. There
 * are three cases, each handled by a dedicated private method:</p>
 * <ol>
 *   <li><b>Context-manager commands</b> ({@link IFcliExecutionContextManager}) — long-running
 *       server-start commands (MCP, RPC) that manage their own per-request context lifecycle.
 *       The strategy must <em>not</em> push any context for these commands and will throw
 *       {@link com.fortify.cli.common.exception.FcliBugException} if one is already on the
 *       stack (which would indicate the server-start command was incorrectly invoked from
 *       within an existing context).</li>
 *   <li><b>Root commands</b> (stack empty at entry) — a plain, top-level CLI invocation such
 *       as {@code fcli ssc session login}. The strategy pushes a brand-new
 *       {@link FcliExecutionContext} (fresh {@link com.fortify.cli.common.rest.unirest.UnirestContext}
 *       and fresh {@link FcliActionState}), executes the command, then pops and closes the
 *       context, shutting down all Unirest connection pools.</li>
 *   <li><b>Nested commands</b> (stack non-empty at entry) — a sub-command triggered by an
 *       action step's {@code run.fcli}. The strategy reuses the existing context so that the
 *       sub-command inherits the parent's open connections and {@code global.*} action
 *       variables.</li>
 * </ol>
 *
 * <h3>Other responsibilities</h3>
 * <p>Regardless of the execution path, this strategy also:</p>
 * <ul>
 *   <li>Registers log masks for sensitive option values</li>
 *   <li>Injects {@code CommandSpec} into all {@link ICommandAware} mixins</li>
 *   <li>Logs the fcli version and command arguments</li>
 * </ul>
 */
@Slf4j
public final class FcliExecutionStrategy implements IExecutionStrategy {
    private final IExecutionStrategy delegate;

    public FcliExecutionStrategy(IExecutionStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public int execute(ParseResult parseResult) throws CommandLine.ExecutionException {
        var leaf = getLeafParseResult(parseResult);
        var leafSpec = leaf.commandSpec();
        if (leafSpec.userObject() instanceof IFcliExecutionContextManager) {
            // Server-start command: manages its own per-request context; strategy must not interfere
            return executeContextManager(parseResult, leafSpec);
        } else if (FcliExecutionContextHolder.stackDepth() == 0) {
            // Top-level CLI invocation: push a fresh context and close it after the command exits
            return executeRootCommand(parseResult, leafSpec);
        } else {
            // Nested invocation via run.fcli: reuse the parent context so connections and
            // global.* variables are shared with the calling action step
            return executeNestedCommand(parseResult, leafSpec);
        }
    }

    private int executeContextManager(ParseResult parseResult, CommandSpec leafSpec) throws CommandLine.ExecutionException {
        int stackDepth = FcliExecutionContextHolder.stackDepth();
        if (stackDepth > 0) {
            throw new FcliBugException(
                "IFcliExecutionContextManager command '%s' must not be invoked from within an existing execution context (stack depth=%d); these commands manage their own context lifecycle",
                leafSpec.qualifiedName(), stackDepth);
        }
        log.debug("Starting command execution (context-manager); command={}", leafSpec.qualifiedName());
        try {
            initializeCommand(leafSpec);
            return delegate.execute(parseResult);
        } finally {
            log.debug("Finished command execution (context-manager); command={}", leafSpec.qualifiedName());
        }
    }

    private int executeRootCommand(ParseResult parseResult, CommandSpec leafSpec) throws CommandLine.ExecutionException {
        try (var frame = FcliExecutionContextHolder.pushNew()) {
            var execCtx = frame.context();
            log.debug("Starting command execution; execInfo={} command={}", execCtx.info(), leafSpec.qualifiedName());
            try {
                initializeCommand(leafSpec);
                return delegate.execute(parseResult);
            } finally {
                log.debug("Finished command execution; execInfo={} command={}", execCtx.info(), leafSpec.qualifiedName());
            }
        }
    }

    private int executeNestedCommand(ParseResult parseResult, CommandSpec leafSpec) throws CommandLine.ExecutionException {
        var execCtx = FcliExecutionContextHolder.current();
        log.debug("Starting command execution; execInfo={} command={}", execCtx.info(), leafSpec.qualifiedName());
        try {
            initializeCommand(leafSpec);
            return delegate.execute(parseResult);
        } finally {
            log.debug("Finished command execution; execInfo={} command={}", execCtx.info(), leafSpec.qualifiedName());
        }
    }

    private void initializeCommand(CommandSpec commandSpec) {
        // Register log masks for option values (cheap operation separate from main iteration)
        registerLogMasks(commandSpec);
        // Log version & args before actual command execution
        logVersionAndArgs(commandSpec);
        // Single pass over all user objects to inject CommandSpec.
        FcliCommandSpecHelper.getAllUserObjectsStream(commandSpec)
            .forEach(o -> {
                if ( o instanceof ICommandAware cAware ) {
                    cAware.setCommandSpec(commandSpec);
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
        log.debug("Running in Docker container: {}", FcliDockerHelper.isRunningInContainer());
    }

    private static void registerLogMasks(CommandSpec commandSpec) {
        for ( var option : commandSpec.options() ) {
            var value = option.getValue();
            if ( value!=null ) {
                JavaHelper.as(option.userObject(), Field.class)
                    .ifPresent(field->registerLogMask(field, value));
            }
        }
        for ( var positionalParameter : commandSpec.positionalParameters() ) {
            var value = positionalParameter.getValue();
            if ( value!=null ) {
                JavaHelper.as(positionalParameter.userObject(), Field.class)
                    .ifPresent(field->registerLogMask(field, value));
            }
        }
    }

    private static void registerLogMask(Field field, Object value) {
        LogMaskHelper.INSTANCE.registerValue(field.getAnnotation(MaskValue.class), LogMaskSource.CLI_OPTION, value);
    }
}
