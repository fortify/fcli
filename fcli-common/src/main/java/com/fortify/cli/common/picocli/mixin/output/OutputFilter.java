package com.fortify.cli.common.picocli.mixin.output;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation allows for filtering the output, based on input property names.
 * By default, this annotation will use the name of the instance field on which this
 * annotation is declared as the JSON property name on which to filter. The 
 * {@link OutputJsonProperty} annotation can be used to specify an alternative 
 * JSON property name.
 * @author rsenden
 *
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface OutputFilter {}
