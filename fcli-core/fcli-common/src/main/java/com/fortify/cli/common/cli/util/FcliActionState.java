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

import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Getter;

/**
 * Mutable state shared by related action or function invocations.
 *
 * <p>This state intentionally lives outside {@link FcliExecutionContext} so that
 * callers can share {@code global.*} values across imported function invocations
 * while still creating a fresh execution frame for each invocation.</p>
 */
public final class FcliActionState {
    @Getter private final ObjectNode globalActionValues = new ObjectNode(JsonNodeFactory.instance, new ConcurrentHashMap<>());
}
