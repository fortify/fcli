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

import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.util.JavaHelper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class provides methods for registering values and patterns to be masked in the
 * fcli log file, and applying those masks to log messages.
 *
 * <p>Registration always targets {@link LogMaskContext#activeContext()}: the GLOBAL context
 * in plain CLI mode, the pre-scope capture context during MCP HTTP request setup, or the
 * current scope's context during tool execution.  Masking is applied in two phases:
 * {@link LogMaskContext#GLOBAL} first, then the active context (if different), so that
 * globally-registered patterns catch values that are then re-registered scope-locally.</p>
 *
 * @author Ruud Senden
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LogMaskHelper {
    /** Singleton instance of this class. */
    public static final LogMaskHelper INSTANCE = new LogMaskHelper();

    /** The log mask level to apply to logging entries. This is set by FortifyCLIDynamicInitializer based
     *  on the value of the generic fcli <pre>--log-mask</pre> option. */
    @Setter private LogMaskLevel logMaskLevel;

    /**
     * Register a value to be masked, based on the semantics as described in {@link MaskValue}. If
     * {@link MaskValue} is <code>null</code>, the given value will not be registered for masking.
     */
    public final LogMaskHelper registerValue(MaskValue maskAnnotation, LogMaskSource source, Object value) {
        if ( maskAnnotation!=null ) {
            registerValue(maskAnnotation.sensitivity(), source, maskAnnotation.description(), value, maskAnnotation.pattern());
        }
        return this;
    }

    /**
     * Register a value to be masked, based on the semantics as described in {@link MaskValue}. If
     * {@link MaskValueDescriptor} is <code>null</code>, the given value will not be registered for masking.
     */
    public final LogMaskHelper registerValue(MaskValueDescriptor maskDescriptor, LogMaskSource source, Object value) {
        if ( maskDescriptor!=null ) {
            registerValue(maskDescriptor.sensitivity(), source, maskDescriptor.description(), value, maskDescriptor.pattern());
        }
        return this;
    }

    /**
     * Register a value to be masked, based on the same semantics as described in {@link MaskValue} but passing
     * each attribute of that annotation as a separate method argument.
     */
    public final LogMaskHelper registerValue(LogSensitivityLevel sensitivityLevel, LogMaskSource source, String description, Object value, String patternString) {
        var valueString = valueAsString(value);
        if ( StringUtils.isNotBlank(patternString) ) {
            var matcher = Pattern.compile(patternString).matcher(valueString);
            if ( matcher.matches() ) {
                if ( matcher.groupCount()!=1 ) {
                    throw new FcliBugException("Pattern string passed to LogMaskHelper::registerValue must contain exactly one capturing group");
                }
                valueString = matcher.group(1);
            }
        }
        return registerValue(sensitivityLevel, valueString, String.format("<REDACTED %s (%s)>", description.toUpperCase(), source));
    }

    private final String valueAsString(Object value) {
        return JavaHelper.as(value, String.class).orElseGet(
            ()->JavaHelper.as(value, char[].class).map(ca->String.valueOf(ca))
            .orElseThrow(()->new FcliBugException("MaskValue annotation can only be used on String or char[] fields, actual type: "+value.getClass().getSimpleName())));
    }

    /**
     * Register a value to be masked with the given {@link LogSensitivityLevel}, for the given log
     * message type(s). Delegates to {@link LogMaskContext#activeContext()}.
     */
    public final LogMaskHelper registerValue(LogSensitivityLevel sensitivityLevel, String valueToMask, String replacement, LogMessageType... logMessageTypes) {
        LogMaskContext.activeContext().registerValue(sensitivityLevel, valueToMask, replacement, logMessageTypes);
        return this;
    }

    /**
     * Register a value to be masked in stdio (stdout/stderr) only. Delegates to {@link LogMaskContext#activeContext()}.
     */
    public final LogMaskHelper registerStdioValue(LogSensitivityLevel sensitivityLevel, String valueToMask, String replacement) {
        LogMaskContext.activeContext().registerStdioValue(sensitivityLevel, valueToMask, replacement);
        return this;
    }

    /**
     * Register a pattern that describes one or more values to be masked. Delegates to {@link LogMaskContext#activeContext()}.
     */
    public final LogMaskHelper registerPattern(LogSensitivityLevel sensitivityLevel, String patternString, String replacement, LogMessageType... logMessageTypes) {
        LogMaskContext.activeContext().registerPattern(sensitivityLevel, patternString, replacement, logMessageTypes);
        return this;
    }

    /**
     * Register a pattern into {@link LogMaskContext#GLOBAL}, for patterns that must persist
     * across all execution contexts (e.g. startup-time HTTP header/response patterns registered
     * by {@code FortifyCLIDynamicInitializer}). Unlike {@link #registerPattern}, this always
     * targets GLOBAL regardless of what execution context is current.
     */
    public final LogMaskHelper registerGlobalPattern(LogSensitivityLevel sensitivityLevel, String patternString, String replacement, LogMessageType... logMessageTypes) {
        LogMaskContext.GLOBAL.registerPattern(sensitivityLevel, patternString, replacement, logMessageTypes);
        return this;
    }

    /**
     * Mask the given log message.  Applied in two phases: {@link LogMaskContext#GLOBAL} first,
     * then the active context if it differs from GLOBAL.  The BiConsumer for discovered values
     * routes back through {@link #registerValue} so that newly-found values land in
     * {@link LogMaskContext#activeContext()} rather than always in GLOBAL.
     */
    public final String mask(LogMessageType logMessageType, String msg) {
        if ( msg == null ) { return null; }
        var consumer = discoveredValueConsumer();
        try {
            var result = LogMaskContext.GLOBAL.mask(logMessageType, msg, consumer);
            var active = LogMaskContext.activeContext();
            if ( active != LogMaskContext.GLOBAL ) {
                result = active.mask(logMessageType, result, consumer);
            }
            return result;
        } catch ( FcliBugException e ) {
            return "<MASKED DUE TO FCLI BUG>";
        }
    }

    /**
     * Mask the given stdio (stdout/stderr) output.  Applied in two phases like {@link #mask}.
     */
    public final String maskStdio(String msg) {
        if ( StringUtils.isBlank(msg) ) { return msg; }
        var consumer = discoveredStdioValueConsumer();
        try {
            var result = LogMaskContext.GLOBAL.maskStdio(msg, consumer);
            var active = LogMaskContext.activeContext();
            if ( active != LogMaskContext.GLOBAL ) {
                result = active.maskStdio(result, consumer);
            }
            return result;
        } catch ( Exception e ) {
            return "<MASKED DUE TO ERROR>";
        }
    }

    // -------------------------------------------------------------------------
    // Package-visible helpers used by LogMaskContext
    // -------------------------------------------------------------------------

    /**
     * @return true if masking is needed based on comparing the given {@link LogSensitivityLevel}
     * against the configured {@link LogMaskLevel}, and given value is not blank/too short,
     * false otherwise.
     */
    final boolean isMaskingNeeded(LogSensitivityLevel sensitivityLevel, String valueToMask) {
        return isMaskingNeededForSensitivityLevel(sensitivityLevel)
                && StringUtils.isNotBlank(valueToMask)
                && valueToMask.length()>4; // Avoid masking very short values, as these are not considered secure anyway
    }

    /**
     * @return true if masking is needed based on comparing the given {@link LogSensitivityLevel}
     * against the configured {@link LogMaskLevel}, false otherwise.
     */
    final boolean isMaskingNeededForSensitivityLevel(LogSensitivityLevel sensitivityLevel) {
        return logMaskLevel != null && logMaskLevel.getSensitivityLevels().contains(sensitivityLevel);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * BiConsumer that routes newly-discovered log-masked values to the active context.
     * We don't know the original sensitivity level here, but if the value was replaced
     * before it should always be replaced again, so we use sensitivity level 'high'.
     */
    private BiConsumer<String, String> discoveredValueConsumer() {
        return (v, r) -> registerValue(LogSensitivityLevel.high, v, r);
    }

    private BiConsumer<String, String> discoveredStdioValueConsumer() {
        return (v, r) -> registerStdioValue(LogSensitivityLevel.high, v, r);
    }
}
