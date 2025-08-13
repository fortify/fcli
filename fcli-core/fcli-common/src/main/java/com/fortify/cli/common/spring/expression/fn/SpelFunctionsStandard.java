/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.spring.expression.fn;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.crypto.helper.EncryptionHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.spring.expression.fn.descriptor.annotation.SpelFunctionDescription;
import com.fortify.cli.common.spring.expression.fn.descriptor.annotation.SpelFunctionParamDescription;
import com.fortify.cli.common.spring.expression.fn.descriptor.annotation.SpelFunctionReturnDescription;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DebugHelper;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.variable.FcliVariableHelper;

import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
public class SpelFunctionsStandard {
    private static final DateTimePeriodHelper PeriodHelper = DateTimePeriodHelper.all();

    @SpelFunctionDescription("Check whether the given string is null or blank.")
    @SpelFunctionReturnDescription("`true` if given string is null or blank, `false` otherwise.")
    public static final boolean isBlank(
            @SpelFunctionParamDescription("The string to check.") String s
    ) {
        return StringUtils.isBlank(s);
    }

    @SpelFunctionDescription("Check whether the given string is not null and not blank.")
    @SpelFunctionReturnDescription("`false` if given string is null or blank, `true` otherwise.")
    public static final boolean isNotBlank(
            @SpelFunctionParamDescription("The string to check.") String s
    ) {
        return StringUtils.isNotBlank(s);
    }

    @SpelFunctionDescription("Get the substring before the first occurrence of a separator. The separator is not returned.")
    @SpelFunctionReturnDescription("The substring before the first occurrence of the separator, or `null` if input string is `null`.")
    public static final String substringBefore(
            @SpelFunctionParamDescription("The string to get a substring from.") String s, 
            @SpelFunctionParamDescription("The separator to search for.") String separator
    ) {
        return StringUtils.substringBefore(s, separator);
    }

    @SpelFunctionDescription("Get the substring after the first occurrence of a separator. The separator is not returned.")
    @SpelFunctionReturnDescription("The substring after the first occurrence of the separator, or `null` if input string is `null`.")
    public static final String substringAfter(
            @SpelFunctionParamDescription("The string to get a substring from.") String s, 
            @SpelFunctionParamDescription("The separator to search for.") String separator
    ) {
        return StringUtils.substringAfter(s, separator);
    }

    @SpelFunctionDescription("Abbreviate the given string to the given maximum length using ellipses, like 'This is...'.")
    @SpelFunctionReturnDescription("The original string abbreviated to the given maximum length if necessary.")
    public static final String abbreviate(
            @SpelFunctionParamDescription("The string to abbreviate.") String s,
            @SpelFunctionParamDescription("The maximum length of the result string, must be at least 4.") int maxLength
    ) {
        return StringUtils.abbreviate(s, maxLength);
    }

    @SpelFunctionDescription("Check whether debug logging is currently enabled.")
    @SpelFunctionReturnDescription("`true` if debug logging is enabled; `false` otherwise.")
    public static final boolean isDebugEnabled() {
        return DebugHelper.isDebugEnabled();
    }

    @SpelFunctionDescription("Generate a new random UUID (Universally Unique Identifier) as a string.")
    @SpelFunctionReturnDescription("A randomly generated UUID string in standard 36-character format.") 
    public static final String uuid() {
        return UUID.randomUUID().toString();
    }

    @SpelFunctionDescription("""
            Format a string using the specified format string and arguments, returning the formatted string.
            See https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Formatter.html#syntax
            for details on format string syntax.
            """)
    @SpelFunctionReturnDescription("The formatted string resulting from applying `fmt` to the `input` arguments.")
    public static final String fmt(
            @SpelFunctionParamDescription("The format string.") String fmt,
            @SpelFunctionParamDescription("The arguments referenced by the format specifiers in the format string.") Object... input
    ) {
        return String.format(fmt, input);
    }

    @SpelFunctionDescription("""
            Parse the given string as a Java `OffsetDateTime` object, for example to allow for date/time
            comparisons, formatting, or retrieval of individual elements like month or year. See 
            https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/OffsetDateTime.html for
            information on methods that can be called on the returned object.
            """)
    @SpelFunctionReturnDescription("""
            The parsed `OffsetDateTime` instance representing the date/time encoded in the string, 
            or `null` if input is `null`.
            """)
    public static final OffsetDateTime date(
            @SpelFunctionParamDescription("The string to parse into an `OffsetDateTime`; may be `null`.") String s
    ) {
        if (s == null) {
            return null;
        }
        OffsetDateTime dt = null;
        try {
            dt = OffsetDateTime.parse(s);
        } catch (DateTimeParseException e) {
            LocalDate d = LocalDate.parse(s);
            dt = OffsetDateTime.of(d.atStartOfDay(), ZoneOffset.UTC);
        }
        return dt;
    }

    @SpelFunctionDescription("""
            Return the current date/time as a Java `OffsetDateTime` object, optionally adjusted by a given period.
            """)
    @SpelFunctionReturnDescription("""
            The current date/time as a Java `OffsetDateTime` object, optionally adjusted by the given period.
            """)
    public static final OffsetDateTime now(
            @SpelFunctionParamDescription("Optional period string like +1d or -5m to adjust the current time.") String... s
    ) {
        if (s != null && s.length > 1) {
            throw new FcliSimpleException("#now(period) only takes a single argument");
        } else if (s == null || s.length == 0 || StringUtils.isBlank(s[0])) {
            return OffsetDateTime.now();
        } else if (s[0].startsWith("+") && s[0].length() > 1) {
            return PeriodHelper.getCurrentOffsetDateTimePlusPeriod(s[0].substring(1));
        } else if (s[0].startsWith("-") && s[0].length() > 1) {
            return PeriodHelper.getCurrentOffsetDateTimeMinusPeriod(s[0].substring(1));
        } else {
            throw new FcliSimpleException("Period passed to #now function is not valid: " + s[0]);
        }
    }

    @SpelFunctionDescription("""
            Retrieve the contents of an fcli variable that was stored through the --store option
            on a previous fcli invocation, throwing an error if the variable does not exist.
            """)
    @SpelFunctionReturnDescription("The JSON contents for the given fcli variable name.")
    public static final JsonNode var(
            @SpelFunctionParamDescription("The name of the variable to retrieve.") String name
    ) {
        return FcliVariableHelper.getVariableContents(name, true);
    }

    @SpelFunctionDescription("""
            Retrieve the value of the given environment variable.
            """)
    @SpelFunctionReturnDescription("""
            The value of the given environment variable, or `null` if the environment 
            variable is not set or its value is blank.
            """)
    public static final String env(
            @SpelFunctionParamDescription("The name of the environment variable to retrieve.") String name
    ) {
        if (StringUtils.isBlank(name)) {
            throw new FcliSimpleException("Environment variable name passed to #env may not be null");
        }
        var result = EnvHelper.env(name);
        // Return null in case of blank string
        return StringUtils.isBlank(result) ? null : result;
    }

    @SpelFunctionDescription("Encrypt the given string using the configured encryption helper.")
    @SpelFunctionReturnDescription("The encrypted form of the input string.")
    public static final String encrypt(
            @SpelFunctionParamDescription("The string to encrypt.") String s
    ) {
        return EncryptionHelper.encrypt(s);
    }

    @SpelFunctionDescription("Decrypt the given encrypted string using the configured decryption helper.")
    @SpelFunctionReturnDescription("The decrypted form of the input string.")
    public static final String decrypt(
            @SpelFunctionParamDescription("The encrypted string to decrypt.") String s
    ) {
        return EncryptionHelper.decrypt(s);
    }

}
