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
package com.fortify.cli.common.log;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.fortify.cli.common.cli.mixin.CommonOptionMixins.AbstractTextResolverMixin;

import picocli.CommandLine.Option;

/**
 * <p>This annotation is used to indicate which values should be masked in the fcli
 * log file, depending on configured sensitivity as configured through the generic
 * fcli <pre>--log-mask</pre> option.</p>
 * 
 * <p>This annotation can be applied on the following:</p>
 * <ul>
 *  <li>Fields representing CLI options as indicated by the picocli {@link Option} annotation</li>
 *  <li>Fields in session descriptors (in the future, potentially we can extend this to all fields 
 *      that get deserialized through Jackson)<li>
 *  <li>Subclasses of {@link AbstractTextResolverMixin}, to mask the resolved text</li> 
 * </ul>
 * 
 * Note that values to be masked must be either a <code>String</code> or <code>char[]</code>. 
 *
 * @author Ruud Senden
 */
@Retention(RUNTIME)
@Target({ TYPE, FIELD })
public @interface MaskValue {
    /** Pattern to be used with {@link #pattern()} to extract host name from URL */
    public static final String URL_HOSTNAME_PATTERN = "https?://([^/]+).*";
    /** Sensitivity of the data to be masked, masking the data only if sensitivity is equal
     *  to or higher than the sensitivity configured through <pre>--log-mask</pre> option */
    public LogSensitivityLevel sensitivity() default LogSensitivityLevel.high;
    /** Short description of the data being masked, to be displayed in masked log messages */ 
    public String description() default "DATA";
    /** Pattern to extract a substring to be masked from the original value. This pattern must 
     *  containing a single capturing group that represents the substring to be masked. Most
     *  common usage is to extract host name from URL, for which {@link #URL_HOSTNAME_PATTERN} 
     *  can be used. If the input value doesn't match the given pattern, the full value will
     *  be masked. */
    public String pattern() default "";
}
