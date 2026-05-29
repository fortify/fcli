/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.cli.util;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to mark ArgGroup classes that should be excluded from wrapper help output
 * (when -Xwrapped option is used). This is useful for generic options that are specific
 * to fcli itself and not relevant to wrapper tools.
 *
 * @author Ruud Senden
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface FcliWrappedHelpExclude {}
