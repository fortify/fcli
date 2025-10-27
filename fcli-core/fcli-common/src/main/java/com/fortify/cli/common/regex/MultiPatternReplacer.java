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
package com.fortify.cli.common.regex;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fortify.cli.common.exception.FcliBugException;

/**
 * <p>This class allows for performing zero or more replacements on given input strings. The replacement
 * operations to be performed can be configured through the {@link #registerValue(String, String)} and
 * {@link #registerPattern(String, String)} methods. For now, this class is only used for log masking,
 * hence there's currently no functionality for unregistering a given value or pattern, although that
 * shouldn't be too difficult to implement if needed.</p>
 * 
 * <p>Based on the configured values and replacement patterns, this class builds one long OR-based regular
 * expression that is then matched against the input string, replacing every matching substring with a
 * corresponding replacement value. This is loosely based on 
 * https://medium.com/@josemarmolejos/using-a-list-of-patterns-to-apply-multiple-regex-replacements-on-a-string-ec16dd5290e4</p>
 *
 * @author Ruud Senden
 */
public final class MultiPatternReplacer {
    private final Set<String> globalMatchPatterns = new HashSet<>();
    private Pattern globalMatchPattern = null;
    private final Map<String,String> valuesToReplace = new HashMap<>();
    private final Map<Pattern, String> patternsToReplace = new HashMap<>();
    
    /**
     * Register a value to be replaced, together with the replacement string. If the given
     * value is found in a given input string, it will be replaced with the given replacement
     * value.
     */
    public final MultiPatternReplacer registerValue(String valueToReplace, String replacement) {
        valuesToReplace.put(valueToReplace, replacement);
        registerGlobalMatchPattern(Pattern.quote(valueToReplace));
        return this;
    }
    
    /**
     * <p>Register a regular expression pattern to search for in a given input string. If the pattern
     * contains one or more capturing groups, each matching capturing group will be considered as 
     * a value to be replaced. If the pattern doesn't contain any capturing groups, the full substring
     * that matches the pattern will be considered as the value to be replaced.</p>
     * 
     * <p>If any of the value(s) to be replaced match a value that was previously registered through 
     * {@link #registerValue(String, String)}, the corresponding replacement value will be applied.
     * If the value wasn't previously registered through {@link #registerValue(String, String)}, the
     * value will be replaced with the given default replacement value.</p>
     * 
     * <p>Any matching value and the corresponding replacement string will (by default) also be registered
     * as a value to be replaced through the {@link #registerValue(String, String)} method. This allows
     * values that are found through a registered pattern to also be replaced on future input strings.</p>
     * 
     * <p>Taking log masking as an example, suppose we've registered a pattern for identifying and replacing
     * sensitive tokens in server response log messages. If the sensitive token then re-appears in future
     * log messages, for example because that token is used to authenticate against the server, we want
     * to replace that token again, but the pattern that was originally used to identify the token likely
     * won't match the future log message. As such, we want to register the token itself as a value to be
     * replaced.</p>
     */
    public final MultiPatternReplacer registerPattern(String patternString, String defaultReplacement) {
        patternsToReplace.put(Pattern.compile(patternString), defaultReplacement);
        registerGlobalMatchPattern(patternString);
        return this;
    }
    
    /**
     * Add the given pattern string to the global match pattern, allowing us to perform all replacement
     * operations using a single pattern matching operation.
     */
    private final void registerGlobalMatchPattern(String patternString) {
        globalMatchPatterns.add(patternString);
        globalMatchPattern = Pattern.compile(String.join("|", globalMatchPatterns), Pattern.MULTILINE); // TODO Make flags configurable?
    }

    /**
     * Apply the registered replacements on the give value. This simply calls {@link #applyReplacements(String, BiConsumer)}
     * with our own {@link #registerValue(String, String)} method, to have any values identified by
     * patterns to be registered for replacement in future input strings. 
     */
    public final String applyReplacements(String value) {
        return applyReplacements(value, this::registerValue);
    }
    
    /**
     * Apply the registered replacements on the given value. Any values identified by patterns will be passed
     * to the given {@link BiConsumer}. In general, this {@link BiConsumer} can be used to do one of three
     * things:</p>
     * <ul>
     *  <li>Do nothing; values identified by patterns will not be replaced in future input strings<li>
     *  <li>Call {@link #registerValue(String, String)} on this {@link MultiPatternReplacer} instance,
     *      to register any values identified by patterns for replacement in future input strings</li>
     *  <li>Call {@link #registerValue(String, String)} on multiple {@link MultiPatternReplacer} instances,
     *      to make each instance aware of values identified by patterns that should be replaced in 
     *      future input strings.</li>
     * </ul>
     */
    public final String applyReplacements(String value, BiConsumer<String,String> replacementValueConsumer) {
        if ( globalMatchPattern==null ) { return value; }
        return globalMatchPattern.matcher(value).replaceAll(mr->applyReplacement(mr, replacementValueConsumer));
    }
    
    /**
     * This method is invoked for each substring in the given input string that matches any of the
     * registered values or patterns. We retrieve the matching substring through {@link MatchResult#group()}.
     * If it matches any of the registered values, we return the corresponding replacement value, otherwise we
     * apply pattern-based (group) replacements on the matching substring. Given that the return value of this
     * method is used in {@link Matcher#replaceAll(java.util.function.Function)}, the return value must be
     * quoted in order to apply the replacement as-is, without backslashes or dollar signs being interpreted
     * by the regex engine.
     */
    private final String applyReplacement(MatchResult mr, BiConsumer<String,String> replacementValueConsumer) {
        var matchingValue = mr.group();
        var result = valuesToReplace.get(matchingValue);
        if ( result == null ) { result = applyGroupReplacements(matchingValue, replacementValueConsumer); }
        return Matcher.quoteReplacement(result);
    }
    
    /**
     * This method first finds the matcher that matches the given value, then looks for any capturing groups
     * in the match result. If there are no capturing groups, the full matching value will be consider as the
     * value to be replaced. In this case, the same pattern will match the same substring again in future
     * input strings, so no need to call the replacement value consumer. If there are capturing groups,
     * we rebuild the given value, replacing each capturing group value with the appropriate replacement
     * value. Each capturing group value is also passed to the given replacement value consumer, to allow
     * each capturing group value to be considered for replacement in future input messages.
     */
    private String applyGroupReplacements(String value, BiConsumer<String,String> replacementValueConsumer) {
        var entry = findMatchingMatcher(value);
        var matcher = entry.getKey();
        var defaultReplacement = entry.getValue();
        if ( matcher.groupCount()==0 ) {
            return valuesToReplace.getOrDefault(value, defaultReplacement);
        } else {
            StringBuilder sb = new StringBuilder(value);
            for ( var i = 1 ; i <= matcher.groupCount() ; i++ ) {
                var groupValue = matcher.group(i);
                var replacement = valuesToReplace.getOrDefault(groupValue, defaultReplacement);
                sb.replace(matcher.start(i), matcher.end(i), replacement);
                replacementValueConsumer.accept(groupValue, replacement); // Replace value on future calls
            }
            return sb.toString();
        }
    }
    
    /**
     * This method iterates over all registered patterns to find a pattern that exactly matches
     * the given value. If no matching pattern is found, an {@link FcliBugException} will be 
     * thrown as we should always be able to find a matching pattern. 
     */
    private final Map.Entry<Matcher, String> findMatchingMatcher(String value) {
        for ( var entry : patternsToReplace.entrySet() ) {
            var matcher = entry.getKey().matcher(value);
            if ( matcher.matches() ) {
                return new AbstractMap.SimpleEntry<>(matcher, entry.getValue());
            }
        }
        throw new FcliBugException("Can't find pattern that matches "+value);
    }
}
