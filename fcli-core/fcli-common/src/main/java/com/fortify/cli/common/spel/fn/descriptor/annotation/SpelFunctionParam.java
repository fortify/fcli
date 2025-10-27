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
package com.fortify.cli.common.spel.fn.descriptor.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation to be placed on SpEL Function class functions' parameter(/s) to provide the
 * description of the parameter to be included in the documentation.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface SpelFunctionParam {
    String name();
    String desc();
    String type() default "";
    boolean optional() default false;
}
