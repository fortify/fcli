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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.mixin.ICommandHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;

import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;

/**
 * Verifies simulated multi-page streaming using testPageBodies. Ensures records are delivered in order
 * without aggregation delay. This bypasses real HTTP calls and exercises page iteration logic.
 */
public class RequestObjectNodeProducerMultiPageStreamingTest {
    @Test
    void testSimulatedMultiPageStreaming() {
        UnirestInstance ui = Unirest.spawnInstance();
        // Create two pages, each with two records
        var page1 = JsonHelper.getObjectMapper().createArrayNode();
        var page2 = JsonHelper.getObjectMapper().createArrayNode();
        page1.add(object("id","1a"));
        page1.add(object("id","1b"));
        page2.add(object("id","2a"));
        page2.add(object("id","2b"));
        var supplier = new DummyUnirestSupplier(ui);
        ICommandHelper ch = new ICommandHelper() {
            @Override public picocli.CommandLine.Model.CommandSpec getCommandSpec() {
                var spec = picocli.CommandLine.Model.CommandSpec.create();
                spec.addMixin("sup", picocli.CommandLine.Model.CommandSpec.forAnnotatedObject(supplier));
                return spec;
            }
            @Override public Object getCommand() { return supplier; }
            @SuppressWarnings("unchecked")
            @Override public <T> java.util.Optional<T> getCommandAs(Class<T> type) {
                if ( type.isInstance(supplier) ) { return java.util.Optional.of((T)supplier); }
                return java.util.Optional.empty();
            }
        };
        List<String> received = new ArrayList<>();
        var producer = RequestObjectNodeProducer.builder()
                .commandHelper(ch)
                .applyAllFrom(ObjectNodeProducerApplyFrom.SPEC)
                .testPageBody(page1)
                .testPageBody(page2)
                .baseRequest(ui.get("https://example.com")) // dummy, unused in test-mode
                .build();
        assertNotNull(producer.getUnirestInstance(), "Unirest should auto-apply from supplier");
        producer.forEach(node -> { received.add(node.get("id").asText()); return null; });
        assertEquals(List.of("1a","1b","2a","2b"), received, "Records should stream in page order");
    }

    private static ObjectNode object(String k, String v) {
        var n = JsonHelper.getObjectMapper().createObjectNode();
        n.put(k, v); return n;
    }

    @picocli.CommandLine.Command(name="dummy")
    private static class DummyUnirestSupplier implements IUnirestInstanceSupplier {
        private final UnirestInstance ui;
        DummyUnirestSupplier(UnirestInstance ui) { this.ui = ui; }
        @Override public UnirestInstance getUnirestInstance() { return ui; }
    }
}
