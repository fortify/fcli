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
package com.fortify.cli.common.action.schema;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.fortify.cli.common.util.ReflectionHelper.AnnotationDefaultClassValue;

/**
 * This annotation may be placed on action model classes or
 * properties to provide one or more sample YAML snippets 
 * to be included in the documentation. Sample snippets 
 * for step properties or types should include all parent 
 * instructions, like <code><pre>
 * steps:
 *   - rest.call:
 *       MyRestCall:
 *         uri: ...
 * </pre></code>
 * Note that sample snippets should only demonstrate the 
 * most commonly used properties, not all supported properties.
 */
@Retention(RUNTIME)
@Target({TYPE, FIELD})
public @interface SampleYamlSnippets {
    String[] value() default {};
    Class<?>[] copyFrom() default AnnotationDefaultClassValue.class;
}
