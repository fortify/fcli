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
package com.fortify.cli.common.log;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.regex.MultiPatternReplacer;

/**
 * Holds a set of log-masking rules (values and patterns) scoped to a single execution context.
 *
 * <p>Each {@link com.fortify.cli.common.cli.util.FcliExecutionContext} owns one instance.
 * {@link #activeContext()} resolves the currently active context for the calling thread by
 * inspecting the execution-context stack: if a context is pushed, its {@code logMaskContext}
 * is returned; otherwise {@link #GLOBAL} is returned.</p>
 *
 * <p>{@link #GLOBAL} is used in plain CLI mode (where all option/session values are registered
 * once into the global context) and as the baseline for all executions (globally-registered
 * startup patterns such as HTTP response scanners are registered here).</p>
 *
 * <p>For server modes (MCP HTTP, MCP stdio, RPC), each request receives a fresh
 * {@link com.fortify.cli.common.cli.util.FcliExecutionContext} with its own
 * {@code LogMaskContext}, providing per-request isolation. Values discovered by
 * global patterns during a request are registered into the per-request context via
 * {@link #activeContext()}, so they are not retained beyond the request lifetime.</p>
 *
 * @author Ruud Senden
 */
public final class LogMaskContext {
    /** The single global context used in CLI mode and as baseline for all executions. */
    public static final LogMaskContext GLOBAL = new LogMaskContext();

    private final Map<LogMessageType, MultiPatternReplacer> patternReplacers = new ConcurrentHashMap<>();
    private final MultiPatternReplacer stdioReplacer = new MultiPatternReplacer();

    // -------------------------------------------------------------------------
    // Static lifecycle
    // -------------------------------------------------------------------------

    /**
     * @return the active context for the current thread: the {@link LogMaskContext} of the
     *   currently pushed {@link com.fortify.cli.common.cli.util.FcliExecutionContext},
     *   or {@link #GLOBAL} if no context is present.
     */
    public static LogMaskContext activeContext() {
        var ctx = FcliExecutionContextHolder.tryCurrentContext();
        return ctx != null ? ctx.getLogMaskContext() : GLOBAL;
    }

    // -------------------------------------------------------------------------
    // Instance methods (package-private; accessed by LogMaskHelper only)
    // -------------------------------------------------------------------------

    final LogMaskContext registerValue(LogSensitivityLevel sensitivityLevel, String valueToMask, String replacement, LogMessageType... logMessageTypes) {
        if ( LogMaskHelper.INSTANCE.isMaskingNeeded(sensitivityLevel, valueToMask) ) {
            var encodedValue = URLEncoder.encode(valueToMask, StandardCharsets.UTF_8);
            for ( var logMessageType : getLogMessageTypesOrDefault(logMessageTypes) ) {
                getOrCreateReplacer(logMessageType)
                        .registerValue(valueToMask, replacement)
                        .registerValue(encodedValue, replacement);
            }
            stdioReplacer
                    .registerValue(valueToMask, replacement)
                    .registerValue(encodedValue, replacement);
        }
        return this;
    }

    final LogMaskContext registerStdioValue(LogSensitivityLevel sensitivityLevel, String valueToMask, String replacement) {
        if ( LogMaskHelper.INSTANCE.isMaskingNeeded(sensitivityLevel, valueToMask) ) {
            stdioReplacer.registerValue(valueToMask, replacement);
        }
        return this;
    }

    final LogMaskContext registerPattern(LogSensitivityLevel sensitivityLevel, String patternString, String replacement, LogMessageType... logMessageTypes) {
        if ( LogMaskHelper.INSTANCE.isMaskingNeededForSensitivityLevel(sensitivityLevel) ) {
            for ( var logMessageType : getLogMessageTypesOrDefault(logMessageTypes) ) {
                getOrCreateReplacer(logMessageType).registerPattern(patternString, replacement);
            }
        }
        return this;
    }

    final String mask(LogMessageType logMessageType, String msg, BiConsumer<String, String> valueConsumer) {
        var replacer = patternReplacers.get(logMessageType);
        if ( replacer == null ) { return msg; }
        return replacer.applyReplacements(msg, valueConsumer);
    }

    final String maskStdio(String msg, BiConsumer<String, String> valueConsumer) {
        return stdioReplacer.applyReplacements(msg, valueConsumer);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private MultiPatternReplacer getOrCreateReplacer(LogMessageType logMessageType) {
        return patternReplacers.computeIfAbsent(logMessageType, t -> new MultiPatternReplacer());
    }

    private LogMessageType[] getLogMessageTypesOrDefault(LogMessageType... logMessageTypes) {
        return logMessageTypes != null && logMessageTypes.length != 0 ? logMessageTypes : LogMessageType.all();
    }
}
