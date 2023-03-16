package com.fortify.cli.common.cli.util;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation allows for defining an environment variable suffix
 * on options and positional parameters for resolving default values
 * from environment variables.
 * 
 * @author rsenden
 *
 */
@Retention(RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface EnvSuffix {
    String value();
}
