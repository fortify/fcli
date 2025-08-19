/**
 * 
 */
package com.fortify.cli.common.spel.fn.descriptor.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation to be placed on SpEL Function class functions/return type to provide the
 * description of the function and return type/value to be included in the documentation.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface SpelFunction {
    SpelFunctionCategory cat();
	String desc() default "";
	String returns();
	
	public static enum SpelFunctionCategory {
	    txt, date, workflow, fortify, fcli, util
	}
}
