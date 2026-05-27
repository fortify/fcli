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
package com.fortify.cli.common.rest.unirest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.fortify.cli.common.util.JavaHelper;

import kong.unirest.UnirestInstance;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages a command-scoped cache of Unirest instances.
 * Each instance is lazily initialized on first access and automatically
 * shut down when this context is closed. Instances of this class are managed 
 * by FcliInitializationExecutionStrategy. Command classes that need access to
 * Unirest instances should usually declare the UnirestContextMixin mixin, or
 * alternatively implement IUnirestContextAware directly. All Unirest instances
 * created by this context are wrapped in a NonClosingUnirestInstanceWrapper to
 * avoid accidental shutdown by client code, but will be automatically closed
 * after command execution through the try-with-resources block in 
 * FcliInitializationExecutionStrategy.
 */
@Slf4j
public class UnirestContext implements AutoCloseable {
    private final Map<String, NonClosingUnirestInstanceWrapper> cache = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    /**
     * Returns a stable identity string similar to the default Object.toString() output
     * (i.e. simple class name + '@' + identity hash). This deliberately
     * bypasses any overridden toString() implementation that might be added later.
     */
    public String identity() {
        return JavaHelper.identity(this);
    }

    /**
     * Gets or creates a Unirest instance for the given key.
     * The configurer function is only called when creating a new instance.
     *
     * @param key Unique identifier for this instance (e.g., "ssc-api", "sc-dast-api")
     * @param configurer Optional configuration function applied on first creation
     * @return The Unirest instance
     * @throws IllegalStateException if context has been closed
     */
    public UnirestInstance getUnirestInstance(String key, Consumer<UnirestInstance> configurer) {
        if (closed) {
            throw new IllegalStateException("UnirestContext has been closed");
        }
        return cache.computeIfAbsent(key, k -> {
            log.debug("Creating new Unirest instance for key: {} in {}", key, identity());
            return new NonClosingUnirestInstanceWrapper(UnirestHelper.createUnirestInstance(configurer));
        });
    }
    
    public void close(String key) {
        var instance = cache.remove(key);
        if ( instance != null ) {
            try {
                log.debug("Shutting down Unirest instance: {} in {}", key, identity());
                instance.getWrappee().shutDown();
            } catch (Exception e) {
                log.warn("Error shutting down Unirest instance {} in {}: {}", key, identity(), e.toString());
            }
        }
    }

    /**
     * Shuts down all managed Unirest instances.
     * This is automatically called when used with try-with-resources.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        log.debug("Closing {} with {} instances", identity(), cache.size());
        cache.keySet().forEach(this::close);
        cache.clear();
    }

    /**
     * Returns the number of cached instances (primarily for testing).
     */
    public int getCachedInstanceCount() {
        return cache.size();
    }
}
