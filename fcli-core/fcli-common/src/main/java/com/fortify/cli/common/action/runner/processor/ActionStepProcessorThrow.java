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

import org.apache.commons.lang3.StringUtils;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.MessageWithCause;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.FcliActionStepException;
import com.fortify.cli.common.exception.AbstractFcliException;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorThrow extends AbstractActionStepProcessor {
    private final ActionRunnerContext ctx;
    private final ActionRunnerVars vars;
    private final MessageWithCause msgWithCause;

    public final void process() {
        var evaluated = evaluateMessageWithCause(msgWithCause, vars);
        
        if (StringUtils.isBlank(evaluated.message()) && evaluated.cause() != null) {
            // Only cause provided - rethrow if FcliException, otherwise wrap
            if (evaluated.cause() instanceof AbstractFcliException) {
                throw (AbstractFcliException) evaluated.cause();
            } else if (evaluated.cause() instanceof RuntimeException) {
                throw (RuntimeException) evaluated.cause();
            } else {
                throw new FcliActionStepException("Exception rethrown", evaluated.cause());
            }
        } else if (StringUtils.isNotBlank(evaluated.message())) {
            // Message provided - throw with optional cause
            throw new FcliActionStepException(evaluated.message(), evaluated.cause());
        } else {
            // No message or cause - throw error
            throw new FcliActionStepException("throw instruction requires either 'msg' or 'cause' property");
        }
    }
}
