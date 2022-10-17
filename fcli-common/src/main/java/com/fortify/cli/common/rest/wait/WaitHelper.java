package com.fortify.cli.common.rest.wait;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.util.StringUtils;

import kong.unirest.UnirestInstance;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Builder
public class WaitHelper {
    private static final DateTimePeriodHelper periodHelper = DateTimePeriodHelper.byRange(Period.SECONDS, Period.DAYS);
    private final Function<UnirestInstance, Collection<JsonNode>> recordsSupplier;
    private final Function<JsonNode, String> currentState;
    private final Function<JsonNode, JsonNode> recordTransformer;
    private final String[] knownStates;
    private final String[] failureStates;
    @Builder.Default private final WaitUnknownStateRequestedAction onUnknownStateRequested = WaitUnknownStateRequestedAction.fail;
    @Builder.Default private final WaitUnknownOrFailureStateAction onFailureState = WaitUnknownOrFailureStateAction.fail;
    @Builder.Default private final WaitUnknownOrFailureStateAction onUnknownState = WaitUnknownOrFailureStateAction.fail;
    @Builder.Default private final WaitTimeoutAction onTimeout = WaitTimeoutAction.fail;
    private final String intervalPeriod;
    private final String timeoutPeriod;
    @Getter private final ArrayNode result = new ObjectMapper().createArrayNode(); 
    
    public final WaitHelper wait(UnirestInstance unirest, IWaitHelperWaitDefinitionSupplier waitDefinitionSupplier) {
        return wait(unirest, waitDefinitionSupplier.getWaitDefinition());
    }
    
    public final WaitHelper wait(UnirestInstance unirest, IWaitHelperWaitDefinition waitDefinition) {
        return waitUntilAll(unirest, waitDefinition.getUntilAll())
                .waitUntilAny(unirest, waitDefinition.getUntilAny())
                .waitWhileAll(unirest, waitDefinition.getWhileAll())
                .waitWhileAny(unirest, waitDefinition.getWhileAny());
    }
    
    public final WaitHelper waitUntilAll(UnirestInstance unirest, String untilStates) {
        if ( StringUtils.isNotBlank(untilStates) ) {
            wait(unirest, new StateEvaluator(untilStates, EvaluatorType.UntilAll));
        }
        return this;
    }
    
    public final WaitHelper waitUntilAny(UnirestInstance unirest, String untilStates) {
        if ( StringUtils.isNotBlank(untilStates) ) {
            wait(unirest, new StateEvaluator(untilStates, EvaluatorType.UntilAny));
        }
        return this;
    }
    
    public final WaitHelper waitWhileAll(UnirestInstance unirest, String whileStates) {
        if ( StringUtils.isNotBlank(whileStates) ) {
            wait(unirest, new StateEvaluator(whileStates, EvaluatorType.WhileAll));
        }
        return this;
    }
    
    public final WaitHelper waitWhileAny(UnirestInstance unirest, String whileStates) {
        if ( StringUtils.isNotBlank(whileStates) ) {
            wait(unirest, new StateEvaluator(whileStates, EvaluatorType.WhileAny));
        }
        return this;
    }

    private final void wait(UnirestInstance unirest, StateEvaluator evaluator) {
        if ( currentState==null ) {
            throw new RuntimeException("No currentState function or currentStateProperty set");
        }
        if ( result.size()>0 ) {
            throw new RuntimeException("Only one of the public wait methods may be invoked with a non-empty set of states");
        }
        long intervalMillis = periodHelper.parsePeriodToMillis(intervalPeriod);
        OffsetDateTime timeout = periodHelper.getCurrentOffsetDateTimePlusPeriod(timeoutPeriod);
        Map<ObjectNode, String> recordsWithCurrentState = getRecordsWithCurrentState(unirest);
        try {
            boolean continueWait = true;
            while ( timeout.isAfter(OffsetDateTime.now()) && (continueWait = evaluator.continueWait(recordsWithCurrentState)) ) {
                try {
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Wait operation interrupted", e);
                }
                recordsWithCurrentState = getRecordsWithCurrentState(unirest);
            }
            if ( continueWait && onTimeout==WaitTimeoutAction.fail ) {
                throw new IllegalStateException("Time-out exceeded");
            }
        } finally {
            recordsWithCurrentState.keySet().forEach(result::add);
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
    
    @RequiredArgsConstructor
    private enum EvaluatorType {
        UntilAll(false, EvaluatorType::allMatch),
        UntilAny(false, EvaluatorType::someMatch),
        WhileAll(true, EvaluatorType::allMatch),
        WhileAny(true, EvaluatorType::someMatch);
        
        private final boolean continueIfMatching;
        private final BiFunction<Set<String>, Collection<String>, Boolean> matcher;
        
        public boolean _continue(Set<String> statesToMatch, Collection<String> currentStates) {
            return continueIfMatching == matcher.apply(statesToMatch, currentStates);
        }
        
        private static boolean someMatch(Set<String> statesToMatch, Collection<String> currentStates) {
            return !Collections.disjoint(statesToMatch, currentStates);
        }
        
        private static boolean allMatch(Set<String> statesToMatch, Collection<String> currentStates) {
            return statesToMatch.containsAll(currentStates);
        }
    }
    
    private final class StateEvaluator {
        private final Set<String> statesSet;
        private final Set<String> knownStatesSet;
        private final Set<String> failureStatesSet;
        private final EvaluatorType evaluatorType;
        
        public StateEvaluator(String statesString, EvaluatorType evaluatorType) {
            this.statesSet = Set.of(statesString.split("\\|"));
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

        public boolean continueWait(Map<ObjectNode, String> nodesWithStatus) {
            Collection<String> currentStates = nodesWithStatus.values();
            return !failUnknownStateCheck(currentStates)
                    && !failFailureStateCheck(currentStates)
                    && evaluatorType._continue(statesSet, currentStates);
        }
        
        private void checkRequestedStates() {
            if ( onUnknownStateRequested!=WaitUnknownStateRequestedAction.ignore || isEmpty(knownStatesSet) ) {
            } else if ( !knownStatesSet.containsAll(statesSet) ) {
                throw new IllegalArgumentException("Unknown states specified in one of the --until* or --while* options: "+removeAll(statesSet, knownStatesSet));
            }
        }

        private boolean failFailureStateCheck(Collection<String> currentStates) {
            if ( onFailureState==WaitUnknownOrFailureStateAction.wait || isEmpty(failureStatesSet) ) { return false; }
            boolean failFailureStateCheck = !Collections.disjoint(currentStates, failureStatesSet);
            throwOptionalError(onFailureState, failFailureStateCheck, ()->"Failure state(s) found: "+retainAll(currentStates, failureStatesSet));
            return failFailureStateCheck;
        }

        private boolean failUnknownStateCheck(Collection<String> currentStates) {
            if ( onUnknownState==WaitUnknownOrFailureStateAction.wait || isEmpty(knownStatesSet) ) { return false; }
            boolean failUnknownStateCheck = !knownStatesSet.containsAll(currentStates);
            throwOptionalError(onUnknownState, failUnknownStateCheck, ()->"Unknown state(s) found: "+removeAll(currentStates, knownStatesSet));
            return failUnknownStateCheck;
        }

        private final boolean isEmpty(Collection<?> collection) {
            return collection==null || collection.isEmpty();
        }
        
        private final <T> Collection<T> removeAll(Collection<T> originalCollection, Collection<T> itemsToRemove) {
            Collection<T> result = new ArrayList<>(originalCollection);
            result.removeAll(itemsToRemove);
            return result;
        }
        
        private final <T> Collection<T> retainAll(Collection<T> originalCollection, Collection<T> itemsToRemove) {
            Collection<T> result = new ArrayList<>(originalCollection);
            result.retainAll(itemsToRemove);
            return result;
        }
        
        private void throwOptionalError(WaitUnknownOrFailureStateAction action, boolean failStateCheck, Supplier<String> messageSupplier) {
            if ( action==WaitUnknownOrFailureStateAction.fail && failStateCheck ) {
                throw new IllegalStateException(messageSupplier.get());
            }
        }
    }
    
    public static class WaitHelperBuilder {
        public WaitHelperBuilder currentStateProperty(String currentStateProperty) {
            this.currentState = node->JsonHelper.evaluateJsonPath(node, currentStateProperty, String.class);
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
}
