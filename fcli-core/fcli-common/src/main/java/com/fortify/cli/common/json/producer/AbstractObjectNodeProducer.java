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

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.mixin.ICommandHelper;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.json.transform.fields.AddFieldsTransformer;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.spel.query.QueryExpression;
import com.fortify.cli.common.util.Break;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Base reusable implementation for {@link IObjectNodeProducer} instances. It concentrates:
 * <ul>
 *   <li>Input transformations: applied once to the full input node (request response or provided JsonNode)</li>
 *   <li>Record transformations: applied for each record node before passing to consumer</li>
 *   <li>Query filtering: optional {@link QueryExpression} applied as last record stage</li>
 * </ul>
 * Subclasses only need to provide the raw input JsonNode(s) by invoking {@link #process(JsonNode, IObjectNodeConsumer)}.
 */
@SuperBuilder
public abstract class AbstractObjectNodeProducer implements IObjectNodeProducer {
    @Getter @Singular private final List<UnaryOperator<JsonNode>> inputTransformers;
    @Getter @Singular private final List<UnaryOperator<JsonNode>> recordTransformers;
    @Getter private final QueryExpression queryExpression;

    /**
     * Template method used by subclasses to feed input JSON to this base class for processing.
     */
    protected final void process(JsonNode input, IObjectNodeConsumer consumer) {
        if ( input==null ) { return; }
        JsonNode transformed = applyInputTransformers(input);
        if ( transformed==null || transformed.isNull() ) { return; }
        if ( transformed.isObject() ) {
            processSingleRecord((ObjectNode)transformed, consumer);
        } else if ( transformed.isArray() ) {
            var array = (ArrayNode)transformed;
            for ( var it = array.elements(); it.hasNext(); ) {
                var n = it.next();
                if ( n.isObject() ) {
                    if ( Break.TRUE == processSingleRecord((ObjectNode)n, consumer) ) { break; }
                } else if ( !n.isNull() && !n.isMissingNode() ) {
                    // We only allow object elements; any other non-null element is unexpected
                    throw new FcliBugException("Unsupported record node type in array: "+n.getNodeType());
                }
            }
        } else {
            // Transformed root must be object or array; if it's some other non-null/non-missing node, that's unexpected
            throw new FcliBugException("Unsupported transformed input node type: "+transformed.getNodeType());
        }
    }

    private JsonNode applyInputTransformers(JsonNode input) {
        JsonNode current = input;
        for ( var t : inputTransformers ) { current = t.apply(current); if ( current==null ) { break; } }
        return current;
    }

    protected Break processSingleRecord(ObjectNode node, IObjectNodeConsumer consumer) {
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
        if ( queryExpression!=null && !queryExpression.matches(current) ) { return Break.FALSE; }
        return Objects.requireNonNullElse(consumer.accept(current), Break.FALSE);
    }

    // Convenience builder customizations ------------------------------------------------------
    public abstract static class AbstractObjectNodeProducerBuilder<C extends AbstractObjectNodeProducer, B extends AbstractObjectNodeProducerBuilder<C,B>> {
        @Getter(AccessLevel.PROTECTED) private IProductHelper explicitProductHelper;
        private ICommandHelper commandHelper;
        /**
         * Configure an explicit {@link IProductHelper} instance to use when applying
         * {@link ObjectNodeProducerApplyFrom#PRODUCT} or {@link ObjectNodeProducerApplyFrom#SPEC}.
         * <p>
         * If set, this overrides implicit resolution through {@code ICommandHelper#getCommandAs(IProductHelperSupplier)}.
         * This allows callers (like commands building producers for cross-product operations) to inject
         * a product-specific helper even if the current command doesn't implement {@link com.fortify.cli.common.output.product.IProductHelperSupplier}.
         * <p>
         * Precedence:
         * <ol>
         *   <li>If an explicit product helper is configured via this method, it is always used.</li>
         *   <li>Otherwise the enum logic attempts to resolve a helper from the command (falling back to {@link com.fortify.cli.common.output.product.NoOpProductHelper}).</li>
         * </ol>
         * Typical usage pattern:
         * <pre>{@code
         * SimpleObjectNodeProducer.builder()
         *     .commandHelper(ch)
         *     .productHelper(customHelper) // optional
         *     .applyAllFrom(ObjectNodeProducerApplyFrom.SPEC)
         *     .build();
         * }</pre>
         * @param productHelper Explicit product helper instance; may be null to allow implicit resolution.
         * @return This builder instance for fluent chaining.
         */
        public B productHelper(IProductHelper productHelper) { this.explicitProductHelper = productHelper; return self(); }
        public B commandHelper(ICommandHelper commandHelper) { this.commandHelper = commandHelper; return self(); }
        
        public B applyAllFrom(ObjectNodeProducerApplyFrom applyFrom) {
            if ( applyFrom==null || applyFrom==ObjectNodeProducerApplyFrom.NONE ) { return self(); }
            getRequiredCommandHelper(); // ensure configured for SPEC or PRODUCT
            applyInputTransformationsFrom(applyFrom);
            applyRecordTransformationsFrom(applyFrom);
            applyQueryFrom(applyFrom);
            applyActionCommandResultSupplierFrom(applyFrom);
            return self();
        }
        private void applyActionCommandResultSupplierFrom(ObjectNodeProducerApplyFrom applyFrom) {
            if ( applyFrom!=ObjectNodeProducerApplyFrom.NONE && getRequiredCommandHelper().getCommand() instanceof IActionCommandResultSupplier s ) {
                recordTransformer(n -> new AddFieldsTransformer(IActionCommandResultSupplier.actionFieldName, s.getActionCommandResult()).transform(n));
            }
        }
        public B applyInputTransformationsFrom(ObjectNodeProducerApplyFrom applyFrom) {
            applyFrom.getSourceStream(getRequiredCommandHelper(), explicitProductHelper).forEach(this::addInputTransformersFromObject);
            return self();
        }
        public B applyRecordTransformationsFrom(ObjectNodeProducerApplyFrom applyFrom) {
            applyFrom.getSourceStream(getRequiredCommandHelper(), explicitProductHelper).forEach(this::addRecordTransformersFromObject);
            return self();
        }
        public B applyQueryFrom(ObjectNodeProducerApplyFrom applyFrom) {
            if ( applyFrom==ObjectNodeProducerApplyFrom.SPEC ) { // Only SPEC provides query expression from spec
                var spec = getRequiredCommandSpec();
                if ( this.queryExpression==null ) { FcliCommandSpecHelper.getQueryExpression(spec).ifPresent(qe -> this.queryExpression = qe); }
            }
            return self();
        }
        protected ICommandHelper getRequiredCommandHelper() {
            if ( commandHelper==null ) {
                throw new FcliBugException("CommandHelper not configured; call commandHelper(<helper>) before apply*From()");
            }
            return commandHelper;
        }
        protected CommandSpec getRequiredCommandSpec() {
            return getRequiredCommandHelper().getCommandSpec();
        }
        private void addInputTransformersFromObject(Object o) {
            if ( o instanceof IInputTransformer it ) { inputTransformer(it::transformInput); }
        }
        private void addRecordTransformersFromObject(Object o) {
            if ( o instanceof IRecordTransformer rt ) { recordTransformer(n->rt.transformRecord(n)); }
        }
    }
}
