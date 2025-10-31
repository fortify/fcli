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

/**
 * {@link IObjectNodeProducer} singleton that produces no records. Useful for commands
 * that wish to return an empty result set without building a full producer pipeline.
 */
public final class EmptyObjectNodeProducer implements IObjectNodeProducer {
    public static final EmptyObjectNodeProducer INSTANCE = new EmptyObjectNodeProducer();
    private EmptyObjectNodeProducer() {}
    @Override
    public void forEach(IObjectNodeConsumer consumer) { /* No records */ }
}
