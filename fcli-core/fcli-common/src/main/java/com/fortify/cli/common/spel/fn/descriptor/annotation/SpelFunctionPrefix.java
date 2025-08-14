/**
 * 
 */
package com.fortify.cli.common.spel.fn.descriptor.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation to be placed on SpEL function classes to define optional prefix for
 * those functions. 
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface SpelFunctionPrefix {
	String value() default "";
}
