/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
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
