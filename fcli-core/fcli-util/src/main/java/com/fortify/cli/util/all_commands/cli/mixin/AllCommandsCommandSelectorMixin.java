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
package com.fortify.cli.util.all_commands.cli.mixin;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.CommandSpecDescriptor;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.StreamingObjectNodeProducer;
import com.fortify.cli.common.spel.query.QueryExpression;
import com.fortify.cli.common.spel.query.QueryExpressionTypeConverter;

import lombok.Getter;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

/**
 * Selector mixin that produces command descriptors via {@link CommandSpecDescriptor}.
 */
public class AllCommandsCommandSelectorMixin {
    @Option(names = {"-q", "--query"}, order=1, converter = QueryExpressionTypeConverter.class, paramLabel = "<SpEL expression>")
    @Getter private QueryExpression queryExpression;

    public final IObjectNodeProducer getObjectNodeProducer() {
        return StreamingObjectNodeProducer.builder()
                .streamSupplier(this::createObjectNodeStream)
                .build();
    }

    public final Stream<ObjectNode> createObjectNodeStream() {
        return createStream().map(CommandSpecDescriptor::getCommandSpecNode);
    }

    public final Stream<CommandSpec> createCommandSpecStream() {
        return createStream().map(CommandSpecDescriptor::getSpec);
    }

    private final Stream<CommandSpecDescriptor> createStream() {
        return CommandSpecDescriptor.rootDescriptorStream()
                .filter(d -> d.matches(queryExpression))
                .distinct();
    }
}
