/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.common.mcp;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({FIELD})

/**
 * Defines a default value for an option or positional parameter when exposed as an MCP tool.
 * <ul>
 *   <li>If applied to an included option/parameter, the value is applied only when the caller doesn't provide a value.</li>
 *   <li>If applied together with {@link MCPExclude}, the option/parameter is hidden from MCP but its default is always added.</li>
 * </ul>
 * Currently the value is always added as <code>--name=value</code> for options and ignored for positional parameters
 * (positional parameters with defaults aren't presently needed; can be added later if required).
 *
 * @author Ruud Senden
 */
public @interface MCPDefaultValue {
    String value();
}
