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
package com.fortify.cli.util._common.helper;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.model.ActionStepRecordsForEach.IActionStepForEachProcessor;
import com.fortify.cli.common.action.runner.ActionFunctionExecutor;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.OutputHelper.Result;

import lombok.RequiredArgsConstructor;

/**
 * {@link IAsyncTask} that executes an action function and feeds its records to the
 * consumer. For streaming functions the {@link IActionStepForEachProcessor} is iterated;
 * for non-streaming functions the single return value is emitted as one record.
 *
 * @author Ruud Senden
 */
@RequiredArgsConstructor
public final class AsyncTaskActionFunction implements IAsyncTask {
    private final ActionFunctionExecutor executor;
    private final ObjectNode argsNode;

    @Override
    public Result run(Consumer<JsonNode> recordConsumer) {
        var result = executor.execute(argsNode);
        if (result instanceof IActionStepForEachProcessor p) {
            p.process(node -> {
                if (!Thread.currentThread().isInterrupted()) {
                    recordConsumer.accept(node);
                    return true;
                }
                return false;
            });
        } else if (result instanceof JsonNode jn) {
            recordConsumer.accept(jn);
        } else if (result != null) {
            recordConsumer.accept(JsonHelper.getObjectMapper().valueToTree(result));
        }
        return new Result(0, "", "");
    }
}
