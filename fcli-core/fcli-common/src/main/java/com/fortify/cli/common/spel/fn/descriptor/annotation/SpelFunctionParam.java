/**
 * 
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
