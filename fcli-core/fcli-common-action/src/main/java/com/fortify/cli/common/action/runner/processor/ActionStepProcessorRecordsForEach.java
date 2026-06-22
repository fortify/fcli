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

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStepRecordsForEach;
import com.fortify.cli.common.action.model.ActionStepRecordsForEach.IActionStepForEachProcessor;
import com.fortify.cli.common.action.runner.ActionRunnerContextLocal;
import com.fortify.cli.common.action.runner.FcliActionStepException;
import com.fortify.cli.common.json.JsonHelper;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorRecordsForEach extends AbstractActionStepProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ActionStepProcessorRecordsForEach.class);
    private final ActionRunnerContextLocal ctx;
    private final ActionStepRecordsForEach step;

    @Override
    public void process() {
    // TODO Clean up this method
        var from = getVars().eval(step.getFrom(), Object.class);
        if (LOG.isTraceEnabled()) {
            LOG.trace("records.for-each 'from' evaluated to: {} (class={})", from, from==null?"null":from.getClass().getName());
        }
        if ( from==null ) { return; }
        if ( from instanceof POJONode ) {
            from = ((POJONode) from).getPojo();
        }
        if ( from instanceof IActionStepForEachProcessor ) {
            ((IActionStepForEachProcessor)from).process(node->processForEachStepNode(step, node));
            return;
        }
        if ( from instanceof Collection<?> ) {
            from = JsonHelper.getObjectMapper().valueToTree(from);
        }
        if ( from instanceof ArrayNode ) {
            JsonHelper.stream((ArrayNode)from)
                .allMatch(value->processForEachStepNode(step, value));
        } else {
            LOG.trace("records.for-each unexpected 'from' type: {}", from.getClass().getName());
            throw new FcliActionStepException("steps:records.for-each:from must evaluate to either an array or IActionStepForEachProcessor instance; actual type: " + from.getClass().getName());
        }
    }
}
