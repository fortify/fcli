/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.rest.wait;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.rest.wait.WaitType.AnyOrAll;
import com.fortify.cli.common.rest.wait.WaitType.LoopType;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;

import kong.unirest.core.UnirestInstance;
import lombok.Builder;
import lombok.Getter;

@Builder
public class WaitHelper {
    private static final DateTimePeriodHelper periodHelper = DateTimePeriodHelper.byRange(Period.SECONDS, Period.DAYS);
    private final Function<UnirestInstance, Collection<JsonNode>> recordsSupplier;
    private final Function<JsonNode, String> currentState;
    private final Function<JsonNode, JsonNode> recordTransformer;
    private final String[] knownStates;
    private final String[] failureStates;
    private final Set<String> matchStates;
    @Builder.Default private final WaitUnknownStateRequestedAction onUnknownStateRequested = WaitUnknownStateRequestedAction.fail;
    @Builder.Default private final WaitUnknownOrFailureStateAction onFailureState = WaitUnknownOrFailureStateAction.fail;
    @Builder.Default private final WaitUnknownOrFailureStateAction onUnknownState = WaitUnknownOrFailureStateAction.fail;
    @Builder.Default private final WaitTimeoutAction onTimeout = WaitTimeoutAction.fail;
    private final String intervalPeriod;
    private final String timeoutPeriod;
    private final IWaitHelperProgressMonitor progressMonitor;
    private final Consumer<Map<ObjectNode, WaitStatus>> onFinish; 
    private final WaitType waitType;
    @Getter private final Map<ObjectNode, WaitStatus> result = new LinkedHashMap<>(); 
    
    public static final ArrayNode plainRecordsAsArrayNode(Map<ObjectNode, WaitStatus> recordsWithWaitStatus) {
        return recordsWithWaitStatus.keySet().stream().collect(JsonHelper.arrayNodeCollector());
    }
    
    public static final ArrayNode recordsWithActionAsArrayNode(Map<ObjectNode, WaitStatus> recordsWithWaitStatus) {
        return recordsWithWaitStatus.keySet().stream()
                .map(n->n.put(IActionCommandResultSupplier.actionFieldName, recordsWithWaitStatus.get(n).name()))
                .collect(JsonHelper.arrayNodeCollector());
    }
    
    public final WaitHelper wait(UnirestInstance unirest) {
        wait(unirest, new StateEvaluator(matchStates, waitType.getLoopType()), waitType.getAnyOrAll());
        return this;
    }
    
    private final void wait(UnirestInstance unirest, StateEvaluator evaluator, AnyOrAll anyOrAll) {
        if ( currentState==null ) {
            throw new RuntimeException("No currentState function or currentStateProperty set");
        }
        if ( result.size()>0 ) {
            throw new RuntimeException("Only one of the public wait methods may be invoked with a non-empty set of states");
        }
        long intervalMillis = periodHelper.parsePeriodToMillis(intervalPeriod);
        OffsetDateTime timeout = periodHelper.getCurrentOffsetDateTimePlusPeriod(timeoutPeriod);
        Map<ObjectNode, String> recordsWithCurrentState = getRecordsWithCurrentState(unirest);
        Map<ObjectNode, WaitStatus> recordsWithWaitStatus = evaluator.getWaitStatuses(recordsWithCurrentState);
        updateProgress(recordsWithWaitStatus);
        try {
            boolean continueWait = true;
            while ( timeout.isAfter(OffsetDateTime.now()) && (continueWait = continueWait(recordsWithWaitStatus, anyOrAll)) ) {
                try {
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Wait operation interrupted", e);
                }
                recordsWithCurrentState = getRecordsWithCurrentState(unirest);
                recordsWithWaitStatus = evaluator.getWaitStatuses(recordsWithCurrentState);
                updateProgress(recordsWithWaitStatus);
            }
            if ( continueWait && onTimeout==WaitTimeoutAction.fail ) {
                recordsWithWaitStatus.replaceAll((k,v)->v!=WaitStatus.WAITING ? v : WaitStatus.TIMEOUT);
                throw new IllegalStateException("Time-out exceeded");
            }
        } finally {
            result.putAll(recordsWithWaitStatus);
            finishProgressMonitoring(recordsWithWaitStatus);
            if ( onFinish!=null ) {
                onFinish.accept(result);
            }
        }
    }
    
    public <T> T getResult(Function<Map<ObjectNode, WaitStatus>, T> f) {
        return f.apply(result);
    }
    
    private final boolean continueWait(Map<ObjectNode, WaitStatus> waitStatuses, AnyOrAll anyOrAll) {
        boolean containsFailureState = waitStatuses.containsValue(WaitStatus.FAILURE_STATE_DETECTED);
        if ( onFailureState==WaitUnknownOrFailureStateAction.fail && containsFailureState ) {
            throw new IllegalStateException("Failure state detected for one or more records");
        } else if ( onFailureState==WaitUnknownOrFailureStateAction.terminate && containsFailureState ) {
            return false;
        }
        boolean containsUnknownState = waitStatuses.containsValue(WaitStatus.UNKNOWN_STATE_DETECTED);
        if ( onUnknownState==WaitUnknownOrFailureStateAction.fail && containsUnknownState ) {
            throw new IllegalStateException("Failure state detected for one or more records");
        } else if ( onUnknownState==WaitUnknownOrFailureStateAction.terminate && containsUnknownState ) {
            return false;
        }
        switch (anyOrAll) {
        case any_match: return !waitStatuses.containsValue(WaitStatus.WAIT_COMPLETE);
        case all_match: return !waitStatuses.values().stream().allMatch(WaitStatus.WAIT_COMPLETE::equals);
        default: throw new RuntimeException("This exception shouldn't occur; please submit a bug"); 
        }
    }
    
    private final Map<ObjectNode, String> getRecordsWithCurrentState(UnirestInstance unirest) {
        if ( recordsSupplier==null ) {
            throw new RuntimeException("No records supplier has been configured");
        }
        Map<ObjectNode, String> nodesWithStatus = new LinkedHashMap<>();
        for ( JsonNode record : recordsSupplier.apply(unirest) ) {
            if ( record instanceof ArrayNode ) {
                addNodesWithStatus(nodesWithStatus, (ArrayNode)record);
            } else {
                addNodeWithStatus(nodesWithStatus, record);
            }
        }
        return nodesWithStatus;
    }

    private void addNodesWithStatus(Map<ObjectNode, String> nodesWithStatus, ArrayNode nodes) {
        nodes.forEach(node->addNodeWithStatus(nodesWithStatus, node));
    }

    private final void addNodeWithStatus(Map<ObjectNode, String> nodesWithStatus, JsonNode node) {
        if ( recordTransformer!=null ) {
            node = recordTransformer.apply(node);
        }
        if ( !(node instanceof ObjectNode) ) {
            throw new RuntimeException("Cannot process node of type "+node.getClass().getName()+"; please report a bug");
        }
        String status = currentState.apply(node);
        nodesWithStatus.put((ObjectNode)node, status);
    }
    
    private final void updateProgress(Map<ObjectNode, WaitStatus> recordsWithWaitStatus) {
        if ( progressMonitor!=null ) {
            progressMonitor.updateProgress(recordsWithWaitStatus);
        }
    }
    
    private final void finishProgressMonitoring(Map<ObjectNode, WaitStatus> recordsWithWaitStatus) {
        if ( progressMonitor!=null ) {
            progressMonitor.finish(recordsWithWaitStatus);
        }
    }
    
    public static enum WaitStatus {
        WAITING, WAIT_COMPLETE, UNKNOWN_STATE_DETECTED, FAILURE_STATE_DETECTED, TIMEOUT 
    }
    
    private final class StateEvaluator {
        private final Set<String> statesSet;
        private final Set<String> knownStatesSet;
        private final Set<String> failureStatesSet;
        private final LoopType evaluatorType;
        
        public StateEvaluator(Set<String> statesSet, LoopType evaluatorType) {
            this.statesSet = statesSet;
            if ( statesSet==null || statesSet.isEmpty() ) {
                throw new IllegalStateException("No states to be matched have been specified");
            }
            this.evaluatorType = evaluatorType;
            this.knownStatesSet = knownStates==null ? null : new HashSet<>(Set.of(knownStates));
            if ( this.knownStatesSet!=null ) {
                // In addition to the provided known states, we consider all supplied states as known as well
                this.knownStatesSet.addAll(statesSet);
            }
            this.failureStatesSet = failureStates==null ? null : new HashSet<>(Set.of(failureStates));
            if ( this.failureStatesSet!=null ) {
                // When explicitly requesting failure states, we don't want to fail on those states
                this.failureStatesSet.removeAll(statesSet);
            }
            checkRequestedStates();
        }

        public Map<ObjectNode, WaitStatus> getWaitStatuses(Map<ObjectNode, String> nodesWithStatus) {
            Map<ObjectNode, WaitStatus> result = new LinkedHashMap<ObjectNode, WaitHelper.WaitStatus>(nodesWithStatus.size());
            for ( Map.Entry<ObjectNode, String> entry : nodesWithStatus.entrySet() ) {
                ObjectNode node = entry.getKey(); 
                String currentState = entry.getValue();
                if ( failUnknownStateCheck(currentState) ) {
                    result.put(node, WaitStatus.UNKNOWN_STATE_DETECTED);
                } else if ( failFailureStateCheck(currentState) ) {
                    result.put(node, WaitStatus.FAILURE_STATE_DETECTED);
                } else {
                    result.put(node, evaluatorType.isWaiting(statesSet, currentState) ? WaitStatus.WAITING : WaitStatus.WAIT_COMPLETE);
                }
            }
            return result;
        }
        
        private void checkRequestedStates() {
            if ( onUnknownStateRequested!=WaitUnknownStateRequestedAction.ignore || isEmpty(knownStatesSet) ) {
            } else if ( !knownStatesSet.containsAll(statesSet) ) {
                throw new IllegalArgumentException("Unknown states specified in one of the --until* or --while* options: "+removeAll(statesSet, knownStatesSet));
            }
        }

        private boolean failFailureStateCheck(String currentState) {
            return failureStatesSet==null ? false : failureStatesSet.contains(currentState);
        }

        private boolean failUnknownStateCheck(String currentState) {
            return knownStatesSet==null ? false : !knownStatesSet.contains(currentState);
        }

        private final boolean isEmpty(Collection<?> collection) {
            return collection==null || collection.isEmpty();
        }
        
        private final <T> Collection<T> removeAll(Collection<T> originalCollection, Collection<T> itemsToRemove) {
            Collection<T> result = new ArrayList<>(originalCollection);
            result.removeAll(itemsToRemove);
            return result;
        }
    }
    
    public static class WaitHelperBuilder {
        public WaitHelperBuilder currentStateProperty(String currentStateProperty) {
            this.currentState = node->JsonHelper.evaluateSpelExpression(node, currentStateProperty, String.class);
            return this;
        }
        
        public WaitHelperBuilder recordSupplier(Function<UnirestInstance, JsonNode> recordSupplier) {
            return recordsSupplier(u->Collections.singletonList(recordSupplier.apply(u)));
        }
        
        public WaitHelperBuilder failureStates(String... failureStates) {
            this.failureStates = failureStates;
            return this;
        }
        
        public WaitHelperBuilder knownStates(String... knownStates) {
            this.knownStates = knownStates;
            return this;
        }
        
        public <T> WaitHelperBuilder onFinish(FunctionAndThenConsumer<Map<ObjectNode, WaitStatus>, T> converter, Consumer<T> consumer) {
            this.onFinish = converter.andThen(consumer);
            return this;
        }
        
        /**
         * Allow for setting interval, timeout and failure actions with a single method call
         * @param controlProperties
         * @return
         */
        public WaitHelperBuilder controlProperties(IWaitHelperControlProperties controlProperties) {
            return intervalPeriod(controlProperties.getIntervalPeriod())
                    .onFailureState(controlProperties.getOnFailureState())
                    .onTimeout(controlProperties.getOnTimeout())
                    .onUnknownState(controlProperties.getOnUnknownState())
                    .onUnknownStateRequested(controlProperties.getOnUnknownStateRequested())
                    .timeoutPeriod(controlProperties.getTimeoutPeriod());
        }
    }
    
    @FunctionalInterface
    public interface FunctionAndThenConsumer<T, R> extends Function<T, R> {
        default Consumer<T> andThen(Consumer<R> after) {
            Objects.requireNonNull(after);
            return (T t) -> {after.accept(apply(t));};
        }
    }
}
