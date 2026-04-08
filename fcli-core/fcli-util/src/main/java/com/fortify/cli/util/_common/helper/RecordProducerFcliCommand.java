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

import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.util.OutputHelper.Result;

import lombok.RequiredArgsConstructor;

/**
 * {@link IRecordProducer} that runs a fcli command and streams its output records.
 *
 * @author Ruud Senden
 */
@RequiredArgsConstructor
public final class RecordProducerFcliCommand implements IRecordProducer {
    private final String command;
    private final Map<String, String> defaultOptions;

    public RecordProducerFcliCommand(String command) {
        this(command, null);
    }

    @Override
    public Result produce(Consumer<JsonNode> recordConsumer) {
        return FcliRunnerHelper.collectRecords(command, on -> recordConsumer.accept(on), defaultOptions);
    }
}
