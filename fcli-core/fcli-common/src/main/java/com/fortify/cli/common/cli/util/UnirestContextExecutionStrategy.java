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

import com.fortify.cli.common.rest.unirest.IUnirestContextAware;
import com.fortify.cli.common.rest.unirest.UnirestContext;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionStrategy;
import picocli.CommandLine.ParseResult;

/**
 * Execution strategy that wraps command execution with UnirestContext management.
 */
@Slf4j
public class UnirestContextExecutionStrategy implements IExecutionStrategy {
    private final IExecutionStrategy delegate;

    public UnirestContextExecutionStrategy(IExecutionStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public int execute(ParseResult parseResult) throws CommandLine.ExecutionException {
        try (UnirestContext context = new UnirestContext()) {
            log.debug("Starting command execution with {}", context.identity());
            injectContext(parseResult, context);
            var result = delegate.execute(parseResult);
            log.debug("Finished command execution with {}", context.identity());
            return result;
        }
    }

    private void injectContext(ParseResult parseResult, UnirestContext context) {
        var leaf = getLeafParseResult(parseResult);
        var leafSpec = leaf.commandSpec();
        FcliCommandSpecHelper.getAllUserObjectsStream(leafSpec)
            .filter(o -> o instanceof IUnirestContextAware)
            .map(o -> (IUnirestContextAware)o)
        .peek(a -> log.debug("Injecting {} into: {}", context.identity(), a.getClass().getName()))
            .forEach(a -> a.setUnirestContext(context));
    }

    private ParseResult getLeafParseResult(ParseResult pr) {
        while (pr.subcommand() != null) { pr = pr.subcommand(); }
        return pr;
    }
}
