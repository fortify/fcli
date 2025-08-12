package com.fortify.cli.common.spring.expression.fn.metadata.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation to be placed on SpEL Function class functions' parameter(/s) to provide the
 * description of the parameter to be included in the documentation.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface SpelFunctionReturnDescription {
	String value() default "";
}
