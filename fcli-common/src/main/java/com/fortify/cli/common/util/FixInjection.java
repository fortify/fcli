package com.fortify.cli.common.util;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

/**
 * For some reason, Micronaut/picocli fail to inject fields annotated
 * with {@link Inject} in (abstract) superclasses used by command
 * implementation. Although unclear why, having this annotation present
 * fixes this issue. See https://github.com/remkop/picocli/issues/1794
 * for details.
 * 
 * @author rsenden
 *
 */
@Qualifier
@Retention(RUNTIME)
@Target(TYPE)
@Inherited
public @interface FixInjection {}
