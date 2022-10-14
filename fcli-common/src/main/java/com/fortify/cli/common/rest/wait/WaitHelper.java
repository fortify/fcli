package com.fortify.cli.common.rest.wait;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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

@Builder()
public class WaitHelper {
    private static final DateTimePeriodHelper periodHelper = DateTimePeriodHelper.byRange(Period.SECONDS, Period.DAYS);
    private final List<Function<UnirestInstance, JsonNode>> requests;
    private final Function<JsonNode, String> currentState;
    private final Function<JsonNode, JsonNode> recordTransformer;
    private final String[] knownStates;
    private final String[] failureStates;
    @Builder.Default private final boolean terminateOnFailureState = true;
    @Builder.Default private final boolean terminateOnUnknownState = true;
    @Builder.Default private final boolean failOnFailureState = true;
    @Builder.Default private final boolean failOnUnknownState = true;
    @Builder.Default private final boolean failOnTimeout = true;
    private final String intervalPeriod;
    private final String timeoutPeriod;
    @Getter private final ArrayNode result = new ObjectMapper().createArrayNode(); 
    
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
        if ( result.size()>0 ) {
            throw new RuntimeException("Only one of the public wait methods may be invoked with a non-empty set of states");
        }
        long intervalMillis = periodHelper.parsePeriodToMillis(intervalPeriod);
        OffsetDateTime timeout = periodHelper.getCurrentOffsetDateTimePlusPeriod(timeoutPeriod);
        Map<ObjectNode, String> recordsWithCurrentState = getRecordsWithCurrentState(unirest);
        boolean _continue = true;
        while ( timeout.isAfter(OffsetDateTime.now()) && (_continue = evaluator._continue(recordsWithCurrentState)) ) {
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                throw new RuntimeException("Wait operation interrupted", e);
            }
            recordsWithCurrentState = getRecordsWithCurrentState(unirest);
        }
        if ( _continue && failOnTimeout ) {
            throw new IllegalStateException("Time-out exceeded");
        }
        recordsWithCurrentState.keySet().forEach(result::add);
    }
    
    private final Map<ObjectNode, String> getRecordsWithCurrentState(UnirestInstance unirest) {
        if ( requests==null || requests.size()==0 ) {
            throw new RuntimeException("No requests have been configured");
        }
        Map<ObjectNode, String> nodesWithStatus = new LinkedHashMap<>();
        for ( Function<UnirestInstance, JsonNode> request: requests ) {
            JsonNode requestResult = request.apply(unirest);
            if ( requestResult instanceof ArrayNode ) {
                addNodesWithStatus(nodesWithStatus, (ArrayNode)requestResult);
            } else {
                addNodeWithStatus(nodesWithStatus, requestResult);
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
            return currentStates.containsAll(statesToMatch);
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
            this.knownStatesSet = knownStates==null ? null : Set.of(knownStates);
            this.failureStatesSet = failureStates==null ? null : new HashSet<>(Set.of(failureStates));
            if ( this.failureStatesSet!=null ) {
                this.failureStatesSet.removeAll(statesSet); // When explicitly requesting failure states, we don't want to fail on those states
            }
        }
        
        public boolean _continue(Map<ObjectNode, String> nodesWithStatus) {
            Collection<String> currentStates = nodesWithStatus.values();
            return !failUnknownStateCheck(currentStates)
                    && !failFailureStateCheck(currentStates)
                    && evaluatorType._continue(statesSet, currentStates);
        }

        private boolean failFailureStateCheck(Collection<String> currentStates) {
            boolean failFailureStateCheck =
                    (terminateOnFailureState || failOnFailureState)
                    && failureStatesSet!=null && !failureStatesSet.isEmpty()
                    && !Collections.disjoint(currentStates, failureStatesSet);
            throwOptionalError(failOnFailureState && failFailureStateCheck, ()->"Failure state(s) found: "+retainAll(currentStates, failureStatesSet));
            return failFailureStateCheck;
        }

        private boolean failUnknownStateCheck(Collection<String> currentStates) {
            boolean failUnknownStateCheck =
                    (terminateOnUnknownState || failOnUnknownState)
                    && knownStatesSet!=null && !knownStatesSet.isEmpty()
                    && !knownStatesSet.containsAll(currentStates);
            throwOptionalError(failOnUnknownState && failUnknownStateCheck, ()->"Unknown state(s) found: "+removeAll(currentStates, knownStatesSet));
            return failUnknownStateCheck;
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
        
        private void throwOptionalError(boolean throwError, Supplier<String> messageSupplier) {
            if ( throwError ) {
                throw new IllegalStateException(messageSupplier.get());
            }
        }
    }
    
    public static class WaitHelperBuilder {
        public WaitHelperBuilder currentStateProperty(String currentStateProperty) {
            this.currentState = node->JsonHelper.evaluateJsonPath(node, currentStateProperty, String.class);
            return this;
        }
        
        public WaitHelperBuilder request(Function<UnirestInstance, JsonNode> request) {
            if ( this.requests==null ) {
                this.requests = new ArrayList<>();
            }
            this.requests.add(request);
            return this;
        }
        
        public WaitHelperBuilder failureStates(String... failureStates) {
            this.failureStates = failureStates;
            return this;
        }
        
        public WaitHelperBuilder knownStates(String... knownStates) {
            this.knownStates = knownStates;
            return this;
        }
    }
}
