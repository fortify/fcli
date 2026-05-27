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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Meta-annotation that marks an annotation as contributing command metadata
 * to {@link CommandSpecDescriptor#getCommandSpecNode()}. Annotated annotation
 * types are discovered generically; their elements are serialized to JSON under
 * the {@code "metadata"} key using the following rules:
 * <ul>
 *   <li>The JSON property name is derived from the annotation's simple name by
 *       stripping the {@code "Fcli"} prefix and lower-casing the first character
 *       (e.g. {@code @FcliModuleCategory} → {@code "moduleCategory"}).</li>
 *   <li>If the annotation has a single {@code value()} element, its value is
 *       serialized directly (scalar string/boolean/number, enum name, or array).</li>
 *   <li>If the annotation has multiple elements, the value is a nested object
 *       with one field per element.</li>
 * </ul>
 */
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
public @interface FcliCommandMetadata {}
