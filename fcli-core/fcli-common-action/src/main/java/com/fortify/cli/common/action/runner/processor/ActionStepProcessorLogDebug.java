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
package com.fortify.cli.common.action.runner.processor;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.MessageWithCause;
import com.fortify.cli.common.action.runner.ActionRunnerContextLocal;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorLogDebug extends AbstractActionStepProcessor {
    private final ActionRunnerContextLocal ctx;
    private final MessageWithCause msgWithCause;

    public final void process() {
        var evaluated = evaluateMessageWithCause(msgWithCause, getVars());
        
        if (evaluated.cause() != null) {
            LOG.debug(evaluated.message(), evaluated.cause());
        } else {
            LOG.debug(evaluated.message());
        }
    }
}
