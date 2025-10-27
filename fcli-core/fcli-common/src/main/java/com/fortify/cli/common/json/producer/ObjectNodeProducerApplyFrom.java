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

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import com.fortify.cli.common.cli.mixin.ICommandHelper;
import com.fortify.cli.common.cli.util.FcliCommandSpecHelper;
import com.fortify.cli.common.json.producer.AbstractObjectNodeProducer.AbstractObjectNodeProducerBuilder;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.common.output.product.NoOpProductHelper;

import lombok.RequiredArgsConstructor;

/**
 * Enum defining how an {@link AbstractObjectNodeProducerBuilder} applies transformers and query logic.
 */
@RequiredArgsConstructor
public enum ObjectNodeProducerApplyFrom {
    /** Apply from full command spec including product helper and user objects */
    SPEC(ObjectNodeProducerApplyFrom::specSourceStream),
    /** Apply only from product helper (if available) */
    PRODUCT(ObjectNodeProducerApplyFrom::productSourceStream),
    /** Do not apply anything */
    NONE((ch, ph)->Stream.empty());

    private final BiFunction<ICommandHelper, IProductHelper, Stream<Object>> sourceStreamProvider;

    public Stream<Object> getSourceStream(ICommandHelper commandHelper, IProductHelper explicitProductHelper) {
        return sourceStreamProvider.apply(commandHelper, explicitProductHelper);
    }

    private static Stream<Object> productSourceStream(ICommandHelper commandHelper, IProductHelper explicitProductHelper) {
        var productHelper = resolveProductHelper(commandHelper, explicitProductHelper);
        return Stream.of(productHelper).filter(Objects::nonNull).map(o->(Object)o);
    }

    private static Stream<Object> specSourceStream(ICommandHelper commandHelper, IProductHelper explicitProductHelper) {
        var productHelper = resolveProductHelper(commandHelper, explicitProductHelper);
        var spec = commandHelper.getCommandSpec();
        return Stream.concat(Stream.of(productHelper), FcliCommandSpecHelper.getAllUserObjectsStream(spec)).filter(Objects::nonNull);
    }

    private static IProductHelper resolveProductHelper(ICommandHelper commandHelper, IProductHelper explicitProductHelper) {
        if ( explicitProductHelper!=null ) { return explicitProductHelper; }
        return commandHelper.getCommandAs(IProductHelperSupplier.class)
                .map(IProductHelperSupplier::getProductHelper)
                .orElse(NoOpProductHelper.instance());
    }
}
