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
import com.fortify.cli.common.util.OutputHelper.Result;

/**
 * Produces records by invoking the given consumer for each record, and returns execution
 * metadata once production is complete. Used by {@link FcliRecordsCache} to abstract over
 * the source of records (fcli command, streaming action function, …).
 *
 * @author Ruud Senden
 */
@FunctionalInterface
public interface IRecordProducer {
    Result produce(Consumer<JsonNode> recordConsumer);
}
