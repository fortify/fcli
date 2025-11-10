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
package com.fortify.cli.common.action.runner.processor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStepRestCallEntry;
import com.fortify.cli.common.action.model.ActionStepRestCallEntry.ActionStepRequestForEachResponseRecord;
import com.fortify.cli.common.action.model.ActionStepRestCallEntry.ActionStepRequestType;
import com.fortify.cli.common.action.model.ActionStepRestCallEntry.ActionStepRestCallLogProgressDescriptor;
import com.fortify.cli.common.action.model.FcliActionValidationException;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.FcliActionStepException;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.ActionRequestDescriptor;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import kong.unirest.UnirestException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorRestCall extends AbstractActionStepProcessor {
    private final ActionRunnerContext ctx;
    private final ActionRunnerVars vars;
    private final LinkedHashMap<String, ActionStepRestCallEntry> requests;

    @Override
    public void process() {
        if ( requests!=null ) {
            var requestsProcessor = new ActionStepRequestsProcessor(ctx);
            requestsProcessor.addRequests(requests, this::processResponse, this::processFailure, vars);
            requestsProcessor.executeRequests();
        }
    }
    
    private final void processResponse(ActionStepRestCallEntry requestDescriptor, JsonNode rawBody) {
        var name = requestDescriptor.getKey();
        var body = ctx.getRequestHelper(requestDescriptor.getTarget()).transformInput(rawBody);
        vars.setLocal(name+"_raw", rawBody);
        vars.setLocal(name, body);
        processOnResponse(requestDescriptor);
        processRequestStepForEach(requestDescriptor);
    }
    
    private final void processFailure(ActionStepRestCallEntry requestDescriptor, UnirestException e) {
        var onFailSteps = requestDescriptor.getOnFail();
        if ( onFailSteps==null ) { throw e; }
        vars.setLocal(requestDescriptor.getKey()+"_exception", new POJONode(e));
        processSteps(onFailSteps);
    }
    
    private final void processOnResponse(ActionStepRestCallEntry requestDescriptor) {
        var onResponseSteps = requestDescriptor.getOnResponse();
        processSteps(onResponseSteps);
    }

    private final void processRequestStepForEach(ActionStepRestCallEntry requestDescriptor) {
        var forEach = requestDescriptor.getForEach();
        if ( forEach!=null ) {
            var input = vars.get(requestDescriptor.getKey());
            if ( input!=null ) {
                if ( input instanceof ArrayNode ) {
                    updateRequestStepForEachTotalCount(forEach, (ArrayNode)input);
                    processRequestStepForEachEmbed(forEach, (ArrayNode)input);
                    processRequestStepForEach(forEach, (ArrayNode)input, this::processRequestStepForEachEntryDo);
                } else {
                    var extraInfo = input instanceof ObjectNode ? "\n  "+input.toString() : "";
                    throw new FcliActionStepException(
                            String.format("Error processing:\n%s\nCaused by: forEach not supported on node type %s%s",
                                    getEntryAsString(requestDescriptor), input.getNodeType(), extraInfo));
                }
            }
        }
    }
    
    private final void processRequestStepForEachEmbed(ActionStepRequestForEachResponseRecord forEach, ArrayNode source) {
        var requestExecutor = new ActionStepRequestsProcessor(ctx);
        processRequestStepForEach(forEach, source, getRequestForEachEntryEmbedProcessor(requestExecutor));
        requestExecutor.executeRequests();
    }
    
    @FunctionalInterface
    private interface IRequestStepForEachEntryProcessor {
        void process(ActionStepRequestForEachResponseRecord forEach, JsonNode currentNode, ActionRunnerVars vars);
    }
    
    private final void processRequestStepForEach(ActionStepRequestForEachResponseRecord forEach, ArrayNode source, IRequestStepForEachEntryProcessor entryProcessor) {
        for ( int i = 0 ; i < source.size(); i++ ) {
            var currentNode = source.get(i);
            var newVars = vars.createChild();
            newVars.setLocal(forEach.getVarName(), currentNode);
            var breakIf = forEach.getBreakIf();
            if ( breakIf!=null && newVars.eval(breakIf, Boolean.class) ) {
                break;
            }
            var _if = forEach.get_if(); 
            if ( _if==null || newVars.eval(_if, Boolean.class) ) {
                entryProcessor.process(forEach, currentNode, newVars);
            }
        }
    }
    
    private void updateRequestStepForEachTotalCount(ActionStepRequestForEachResponseRecord forEach, ArrayNode array) {
        var totalCountName = String.format("total%sCount", StringUtils.capitalize(forEach.getVarName()));
        var totalCount = vars.get(totalCountName);
        if ( totalCount==null ) { totalCount = new IntNode(0); }
        vars.setLocal(totalCountName, new IntNode(totalCount.asInt()+array.size()));
    }

    private void processRequestStepForEachEntryDo(ActionStepRequestForEachResponseRecord forEach, JsonNode currentNode, ActionRunnerVars newVars) {
        new ActionStepProcessorSteps(ctx, newVars, forEach.get_do()).process();
    }
    
    private IRequestStepForEachEntryProcessor getRequestForEachEntryEmbedProcessor(ActionStepRequestsProcessor requestExecutor) {
        return (forEach, currentNode, newVars) -> {
            if ( !currentNode.isObject() ) {
                // TODO Improve exception message?
                throw new FcliActionStepException("Cannot embed data on non-object nodes: "+forEach.getVarName());
            }
            requestExecutor.addRequests(forEach.getEmbed(), 
                    (rd,r)->((ObjectNode)currentNode).set(rd.getKey(), ctx.getRequestHelper(rd.getTarget()).transformInput(r)), 
                    this::processFailure, newVars);
        };
    }
    
    @RequiredArgsConstructor
    private static final class ActionStepRequestsProcessor {
        private final ActionRunnerContext ctx;
        private final Map<String, List<IActionRequestHelper.ActionRequestDescriptor>> simpleRequests = new LinkedHashMap<>();
        private final Map<String, List<IActionRequestHelper.ActionRequestDescriptor>> pagedRequests = new LinkedHashMap<>();
        
        final void addRequests(Map<String, ActionStepRestCallEntry> requestDescriptors, BiConsumer<ActionStepRestCallEntry, JsonNode> responseConsumer, BiConsumer<ActionStepRestCallEntry, UnirestException> failureConsumer, ActionRunnerVars vars) {
            if ( requestDescriptors!=null ) {
                requestDescriptors.values().forEach(r->addRequest(r, responseConsumer, failureConsumer, vars));
            }
        }
        
        private final void addRequest(ActionStepRestCallEntry requestDescriptor, BiConsumer<ActionStepRestCallEntry, JsonNode> responseConsumer, BiConsumer<ActionStepRestCallEntry, UnirestException> failureConsumer, ActionRunnerVars vars) {
            var _if = requestDescriptor.get_if();
            if ( _if==null || vars.eval(_if, Boolean.class) ) {
                var method = requestDescriptor.getMethod();
                var uri = vars.eval(requestDescriptor.getUri(), String.class);
                checkUri(uri);
                var query = vars.eval(requestDescriptor.getQuery(), Object.class);
                var body = requestDescriptor.getBody()==null ? null : vars.eval(requestDescriptor.getBody(), Object.class);
                var requestData = new IActionRequestHelper.ActionRequestDescriptor(method, uri, query, body, r->responseConsumer.accept(requestDescriptor, r), e->failureConsumer.accept(requestDescriptor, e));
                addLogProgress(requestData, requestDescriptor.getLogProgress(), vars);
                if ( requestDescriptor.getType()==ActionStepRequestType.paged ) {
                    pagedRequests.computeIfAbsent(requestDescriptor.getTarget(), s->new ArrayList<IActionRequestHelper.ActionRequestDescriptor>()).add(requestData);
                } else {
                    simpleRequests.computeIfAbsent(requestDescriptor.getTarget(), s->new ArrayList<IActionRequestHelper.ActionRequestDescriptor>()).add(requestData);
                }
            }
        }

        private void checkUri(String uriString) {
            try {
                var uri = new URI(uriString);
                // We don't allow absolute URIs, as this could expose authorization
                // headers and other data to systems other than the predefined target
                // system.
                if ( uri.isAbsolute() ) {
                    throw new FcliActionValidationException("Absolute request uri is not allowed: "+uriString);
                }
            } catch ( URISyntaxException e ) {
                throw new FcliActionValidationException("Invalid request uri: "+uriString);
            }
        }

        private void addLogProgress(ActionRequestDescriptor requestData, ActionStepRestCallLogProgressDescriptor pagingProgress, ActionRunnerVars vars) {
            if ( pagingProgress!=null ) {
                addPagingProgress(pagingProgress.getPrePageLoad(), requestData::setPrePageLoad, vars);
                addPagingProgress(pagingProgress.getPostPageLoad(), requestData::setPostPageLoad, vars);
                addPagingProgress(pagingProgress.getPostPageProcess(), requestData::setPostPageProcess, vars);
            }
        }
        
        private void addPagingProgress(TemplateExpression expr, Consumer<Runnable> consumer, ActionRunnerVars vars) {
            if ( expr!=null ) {
                consumer.accept(()->ctx.getProgressWriter().writeProgress(asSingleLineString(vars.eval(expr, String.class))));
            }
        }
        
        final void executeRequests() {
            simpleRequests.entrySet().forEach(e->executeRequest(e.getKey(), e.getValue(), false));
            pagedRequests.entrySet().forEach(e->executeRequest(e.getKey(), e.getValue(), true));
        }
        
        private void executeRequest(String target, List<ActionRequestDescriptor> requests, boolean isPaged) {
            var requestHelper = ctx.getRequestHelper(target);
            if ( isPaged ) {
                requests.forEach(r->requestHelper.executePagedRequest(r));
            } else {
                requestHelper.executeSimpleRequests(requests);
            }
        }
    }
}
