package com.fortify.cli.common.picocli.mixin.output;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation allows for filtering the output, based on input property names.
 * @author rsenden
 *
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface OutputFilter {
	String value();
}
