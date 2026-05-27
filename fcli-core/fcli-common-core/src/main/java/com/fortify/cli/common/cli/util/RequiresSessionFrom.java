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
 * Indicates that this module requires a session from the specified product module.
 * Applied on product module {@code *Commands} classes whose session is provided by
 * another module (e.g. sc-sast, sc-dast, and aviator all authenticate via SSC).
 *
 * <p>Import {@link FcliModules} constants statically for concise usage:
 * <pre>
 *   import static com.fortify.cli.common.cli.util.FcliModules.SSC;
 *   &#64;RequiresSessionFrom(SSC)
 * </pre>
 *
 * <p>Serialized to {@code metadata.requiresSessionFrom} in the command spec node
 * produced by {@link CommandSpecDescriptor}.
 */
@FcliCommandMetadata
@Retention(RUNTIME)
@Target(TYPE)
public @interface RequiresSessionFrom {
    FcliModules value();
}
