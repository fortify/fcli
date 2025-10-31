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
package com.fortify.cli.common.log;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.regex.MultiPatternReplacer;
import com.fortify.cli.common.util.JavaHelper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class provides methods for registering values and patterns to be masked in the
 * fcli log file, and applying those masks to log messages.
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
    private final Map<LogMessageType, MultiPatternReplacer> multiPatternReplacers = new HashMap<>();
    
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
     * message type(s). If no log message types are provided, the mask will be applied to all log 
     * message types. See {@link MultiPatternReplacer#registerValue(String, String)} for details.
     */
    public final LogMaskHelper registerValue(LogSensitivityLevel sensitivityLevel, String valueToMask, String replacement, LogMessageType... logMessageTypes) {
        if ( isMaskingNeeded(sensitivityLevel) ) {
            for ( var logMessageType : getLogMessageTypesOrDefault(logMessageTypes) ) {
                getMultiPatternReplacer(logMessageType)
                    .registerValue(valueToMask, replacement)
                    .registerValue(URLEncoder.encode(valueToMask, StandardCharsets.UTF_8), replacement);
            }
        }
        return this;
    }
    
    /**
     * Register a pattern that describes one or more values to be masked, with the given sensitivity 
     * level and for the given log message type(s). If no log message types are provided, the pattern
     * will be registered for all log message types. See {@link MultiPatternReplacer#registerPattern(String, String)}
     * for details.
     */
    public final LogMaskHelper registerPattern(LogSensitivityLevel sensitivityLevel, String patternString, String replacement, LogMessageType... logMessageTypes) {
        if ( isMaskingNeeded(sensitivityLevel) ) {
            for ( var logMessageType : getLogMessageTypesOrDefault(logMessageTypes) ) {
                getMultiPatternReplacer(logMessageType).registerPattern(patternString, replacement);
            }
        }
        return this;
    }

    /**
     * Return either the given log message types, or all log message types if no log message types given.
     */
    private LogMessageType[] getLogMessageTypesOrDefault(LogMessageType... logMessageTypes) {
        return logMessageTypes!=null && logMessageTypes.length!=0 ? logMessageTypes : LogMessageType.all();
    }
    
    /**
     * Get the {@link MultiPatternReplacer} instance for the given {@link LogMessageType}.
     */
    private final MultiPatternReplacer getMultiPatternReplacer(LogMessageType logMessageType) {
        return multiPatternReplacers.computeIfAbsent(logMessageType, t->new MultiPatternReplacer());
    }
    
    /**
     * @return true if masking is needed based on comparing the given {@link LogSensitivityLevel}
     * against the configured {@link LogMaskLevel}, false otherwise.
     */
    private final boolean isMaskingNeeded(LogSensitivityLevel sensitivityLevel) {
        return logMaskLevel.getSensitivityLevels().contains(sensitivityLevel);
    }

    /**
     * Mask the given log message using the registered values and patterns for the
     * given {@link LogMessageType}.
     */
    public final String mask(LogMessageType logMessageType, String msg) {
        var multiPattern = getMultiPatternReplacer(logMessageType);
        if ( multiPattern==null ) { return null; }
        try {
            return multiPattern.applyReplacements(msg,
                // We want to register replacement values for all log message types, not just
                // the log message type on which the replacement was applied. We don't know the
                // original sensitivity level here, but if the value was replaced before, it should
                // always be replaced again, hence we use sensitivity level 'high'.        
                (v,r)->registerValue(LogSensitivityLevel.high, v, r));
        } catch ( FcliBugException e ) {
            // Exceptions will be eaten by the logging framework, causing the log message to be lost,
            // so instead we return a fixed string indicating that an fcli bug occurred.
            return "<MASKED DUE TO FCLI BUG>";
        }
    }
    

}
