package com.fortify.cli.common.output.cli.mixin.filter;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import picocli.CommandLine.Option;

/**
 * This annotation allows for specifying a target name for an {@link Option}.
 * @author rsenden
 *
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface OptionTargetName {
    String value();
}
