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
package com.fortify.cli.common.concurrent.job.task;

import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.concurrent.job.IAsyncTask;
import com.fortify.cli.common.concurrent.job.exec.FcliRunnerHelper;
import com.fortify.cli.common.util.OutputHelper.Result;

/**
 * {@link IAsyncTask} that runs a fcli command in the background. When
 * {@code collectRecords} is {@code true} the command's structured records are
 * fed to the consumer; when {@code false} stdout is captured instead and the
 * consumer is never called.
 *
 * @author Ruud Senden
 */
public final class AsyncTaskFcliCommand implements IAsyncTask {
    private final String command;
    private final String[] args;
    private final Map<String, String> defaultOptions;
    private final boolean collectRecords;

    public AsyncTaskFcliCommand(String command, boolean collectRecords) {
        this(command, null, collectRecords);
    }

    public AsyncTaskFcliCommand(String command, Map<String, String> defaultOptions) {
        this(command, defaultOptions, true);
    }

    public AsyncTaskFcliCommand(String command, Map<String, String> defaultOptions, boolean collectRecords) {
        this.command = command;
        this.args = null;
        this.defaultOptions = defaultOptions;
        this.collectRecords = collectRecords;
    }

    public AsyncTaskFcliCommand(String[] args, Map<String, String> defaultOptions) {
        this.command = null;
        this.args = args.clone();
        this.defaultOptions = defaultOptions;
        this.collectRecords = true;
    }

    @Override
    public Result run(Consumer<JsonNode> recordConsumer) {
        if (collectRecords) {
            return args==null
                    ? FcliRunnerHelper.collectRecords(command, recordConsumer::accept, defaultOptions)
                    : FcliRunnerHelper.collectRecords(args, recordConsumer::accept, defaultOptions);
        } else {
            return args==null
                    ? FcliRunnerHelper.collectStdout(command, defaultOptions)
                    : FcliRunnerHelper.collectStdout(args, defaultOptions);
        }
    }
}
