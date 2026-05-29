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
package com.fortify.cli.common.spel.fn.descriptor.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marker annotation to be placed on classes whose public methods with {@link SpelFunction}
 * annotations should be automatically documented when returned by a SpEL function.
 * 
 * <p>When a method annotated with {@link SpelFunction} returns an instance of a class
 * annotated with {@link SpelFunctions}, the descriptor factory will automatically process
 * all public methods of that class to include them in the documentation with an appropriate
 * prefix derived from the factory method name.</p>
 * 
 * <p>Example:</p>
 * <pre>
 * // Factory method that returns the builder
 * {@literal @}SpelFunction(...)
 * public static JsonNodeMerger objectMerger() { return new JsonNodeMerger(); }
 * 
 * // Builder class with fluent API
 * {@literal @}SpelFunctions
 * public class JsonNodeMerger {
 *     {@literal @}SpelFunction(...)
 *     public JsonNodeMerger arraysConcat() { ... }
 *     
 *     {@literal @}SpelFunction(...)
 *     public JsonNode merge(JsonNode target, JsonNode source) { ... }
 * }
 * 
 * // Results in documentation for:
 * // - #objectMerger()
 * // - #objectMerger().arraysConcat()
 * // - #objectMerger().merge(target, source)
 * </pre>
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface SpelFunctions {
}
