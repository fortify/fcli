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
package com.fortify.cli.common.json.producer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.json.producer.pipeline.QueryFilterStage;
import com.fortify.cli.common.json.producer.pipeline.TransformationPipelineRunnerConfig;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.util.Break;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Base reusable implementation for {@link IObjectNodeProducer} instances. It concentrates:
 * <ul>
 *   <li>Input transformations: applied once to the full input node (request response or provided JsonNode)</li>
 *   <li>Record transformations: applied for each record node before passing to consumer</li>
 *   <li>Query filtering: optional {@link QueryFilterStage} applied as last record stage</li>
 * </ul>
 * Subclasses only need to provide the raw input JsonNode(s) by invoking {@link #process(JsonNode, IObjectNodeConsumer)}.
 */
public abstract class AbstractObjectNodeProducer implements IObjectNodeProducer {
    @Getter private final List<UnaryOperator<JsonNode>> inputTransformers;
    @Getter private final List<UnaryOperator<JsonNode>> recordTransformers;
    @Getter private final QueryFilterStage queryFilterStage;
    @Getter(AccessLevel.PROTECTED) private final TransformationPipelineRunnerConfig pipelineConfig; // kept for compatibility if needed later

    protected AbstractObjectNodeProducer(List<UnaryOperator<JsonNode>> inputTransformers,
            List<UnaryOperator<JsonNode>> recordTransformers, QueryFilterStage queryFilterStage) {
        this.inputTransformers = inputTransformers != null ? inputTransformers : new ArrayList<>();
        this.recordTransformers = recordTransformers != null ? recordTransformers : new ArrayList<>();
        this.queryFilterStage = queryFilterStage; // may be null
        this.pipelineConfig = buildPipelineConfig();
    }

    private TransformationPipelineRunnerConfig buildPipelineConfig() {
        var cfg = TransformationPipelineRunnerConfig.builder().build();
        inputTransformers.forEach(cfg::inputTransformer);
        recordTransformers.forEach(cfg::recordTransformer);
        if ( queryFilterStage != null ) { cfg.recordStage(queryFilterStage); }
        return cfg;
    }

    /**
     * Template method used by subclasses to feed input JSON to this base class for processing.
     */
    protected final void process(JsonNode input, IObjectNodeConsumer consumer) {
        if ( input==null ) { return; }
        JsonNode transformed = applyInputTransformers(input);
        if ( transformed==null || transformed.isNull() ) { return; }
        if ( transformed.isObject() ) {
            handleRecordNode((ObjectNode)transformed, consumer);
        } else if ( transformed.isArray() ) {
            var array = (ArrayNode)transformed;
            for ( var it = array.elements(); it.hasNext(); ) {
                var n = it.next();
                if ( n.isObject() ) {
                    if ( Break.TRUE == handleRecordNode((ObjectNode)n, consumer) ) { break; }
                }
            }
        } else {
            // Non container nodes are ignored
        }
    }

    private JsonNode applyInputTransformers(JsonNode input) {
        JsonNode current = input;
        for ( var t : inputTransformers ) { current = t.apply(current); if ( current==null ) { break; } }
        return current;
    }

    private Break handleRecordNode(ObjectNode node, IObjectNodeConsumer consumer) {
        ObjectNode current = node;
        for ( var t : recordTransformers ) {
            var transformed = t.apply(current);
            if ( transformed==null || transformed.isNull() ) { return Break.FALSE; }
            if ( transformed.isObject() ) {
                current = (ObjectNode)transformed;
            } else { // If transformer changed type we ignore & keep original
                continue;
            }
        }
        if ( queryFilterStage!=null ) {
            var outcome = queryFilterStage.apply(null, current); // context not required currently
            if ( outcome==null || outcome.node()==null ) { return Break.FALSE; }
            // skip or stop decisions
            switch ( outcome.decision() ) {
                case SKIP: return Break.FALSE;
                case STOP: return Break.TRUE;
                case CONTINUE: break;
            }
            if ( outcome.node().isObject() ) { current = (ObjectNode)outcome.node(); }
        }
        return Objects.requireNonNullElse(consumer.accept(current), Break.FALSE);
    }

    // Convenience builder customizations ------------------------------------------------------
    @SuppressWarnings("unchecked")
    public abstract static class AbstractObjectNodeProducerBuilder<C extends AbstractObjectNodeProducer, B extends AbstractObjectNodeProducerBuilder<C,B>> {
        protected List<UnaryOperator<JsonNode>> inputTransformers = new ArrayList<>();
        protected List<UnaryOperator<JsonNode>> recordTransformers = new ArrayList<>();
        protected com.fortify.cli.common.json.producer.pipeline.QueryFilterStage queryFilterStage;
        protected IProductHelper productHelper;
        public B inputTransformer(UnaryOperator<JsonNode> transformer) { this.inputTransformers.add(transformer); return (B)this; }
        public B recordTransformer(UnaryOperator<JsonNode> transformer) { this.recordTransformers.add(transformer); return (B)this; }
        public B addInputTransformers(Iterable<? extends IInputTransformer> transformers) { transformers.forEach(t->this.inputTransformers.add(t::transformInput)); return (B)this; }
        public B addRecordTransformers(Iterable<? extends IRecordTransformer> transformers) { transformers.forEach(t->this.recordTransformers.add(r->t.transformRecord(r))); return (B)this; }
        public B queryFilter(com.fortify.cli.common.json.producer.pipeline.QueryFilterStage stage) { this.queryFilterStage = stage; return (B)this; }
        public B productHelper(IProductHelper productHelper) { this.productHelper = productHelper; return (B)this; }
    protected abstract B self();
    public abstract C build();

        // --- Spec application API ---
        public B applyFromSpec(picocli.CommandLine.Model.CommandSpec spec) { 
            applyInputTransformationsFromSpec(spec);
            applyRecordTransformationsFromSpec(spec);
            applyQueryFromSpec(spec);
            return self();
        }
        public B applyInputTransformationsFromSpec(picocli.CommandLine.Model.CommandSpec spec) {
            FcliCommandSpecHelper.getAllMixinsStream(spec).map(m->m.userObject()).forEach(this::addInputTransformersFromObject);
            addInputTransformersFromObject(productHelper);
            addInputTransformersFromObject(spec.userObject());
            return self();
        }
        public B applyRecordTransformationsFromSpec(picocli.CommandLine.Model.CommandSpec spec) {
            FcliCommandSpecHelper.getAllMixinsStream(spec).map(m->m.userObject()).forEach(this::addRecordTransformersFromObject);
            addRecordTransformersFromObject(productHelper);
            addRecordTransformersFromObject(spec.userObject());
            return self();
        }
        public B applyQueryFromSpec(picocli.CommandLine.Model.CommandSpec spec) {
            if ( this.queryFilterStage==null ) {
                FcliCommandSpecHelper.getQueryExpression(spec).ifPresent(qe -> this.queryFilterStage = new QueryFilterStage(qe));
            }
            return self();
        }
        private void addInputTransformersFromObject(Object o) {
            if ( o instanceof IInputTransformer it ) { inputTransformer(it::transformInput); }
        }
        private void addRecordTransformersFromObject(Object o) {
            if ( o instanceof IRecordTransformer rt ) { recordTransformer(n->rt.transformRecord(n)); }
        }
    }
}
