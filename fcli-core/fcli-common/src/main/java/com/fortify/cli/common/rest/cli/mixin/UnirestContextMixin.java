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
package com.fortify.cli.common.rest.cli.mixin;

import java.util.function.Consumer;

import com.fortify.cli.common.rest.unirest.IUnirestContextAware;
import com.fortify.cli.common.rest.unirest.UnirestContext;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.Command;

/**
 * Picocli mixin providing access to an injected {@link UnirestContext}.
 * As a best practice, commands or mixins should use this mixin rather 
 * than implementing {@link IUnirestContextAware} directly.
 */
@Command
public class UnirestContextMixin implements IUnirestContextAware {
    @Getter @Setter private UnirestContext unirestContext;

    public final UnirestInstance getUnirestInstance(String key, Consumer<UnirestInstance> configurer) {
        return unirestContext.getUnirestInstance(key, configurer);
    }
    
    public final void close(String key) {
        unirestContext.close(key);
    }
}
