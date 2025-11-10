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
package com.fortify.cli.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FcliExecutionExceptionHandler implements IExecutionExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FcliExecutionExceptionHandler.class);
    public static final FcliExecutionExceptionHandler INSTANCE = new FcliExecutionExceptionHandler();
    
    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) throws Exception {
        return handleException(ex, commandLine);
    }

    public int handleException(Exception ex, CommandLine commandLine) {
        if ( LOG.isDebugEnabled() ) {
            LOG.debug("fcli terminating with exception", ex);
        }
        var formattedException = FcliExceptionHelper.formatException(ex);
        if ( formattedException!=null ) {
            var err = commandLine.getErr();
            err.println(commandLine.getColorScheme().errorText(formattedException));
            err.flush();
        }
        return getExitCode(ex, commandLine);
    }

    private int getExitCode(Exception ex, CommandLine commandLine) {
        if ( ex instanceof AbstractFcliException ) {
            var exitCode = ((AbstractFcliException)ex).exitCode();
            if ( exitCode!=null ) {
                return exitCode;
            }
        }
        return commandLine.getCommandSpec().exitCodeOnExecutionException();
    }
}
