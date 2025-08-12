package com.fortify.cli.common.action.schema.annotations;

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
public @interface ReturnDescriptor {
	String value() default "";
}
