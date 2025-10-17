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
package com.fortify.cli.common.rest.unirest;

import java.util.function.Consumer;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;

/**
 * Picocli mixin providing access to an injected {@link UnirestContext}.
 * Commands can include this mixin or implement {@link IUnirestContextAware} directly.
 */
public class UnirestContextMixin implements IUnirestContextAware {
    @Getter @Setter private UnirestContext unirestContext;

    public final UnirestInstance getUnirestInstance(String key, Consumer<UnirestInstance> configurer) {
        return unirestContext.getUnirestInstance(key, configurer);
    }
}
