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
package com.fortify.cli.common.spring.expression;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.annotations.MethodDescriptor;
import com.fortify.cli.common.action.schema.annotations.ParamDescriptor;
import com.fortify.cli.common.action.schema.annotations.ReturnDescriptor;
import com.fortify.cli.common.crypto.helper.EncryptionHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DebugHelper;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.variable.FcliVariableHelper;

import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
public class SpelFunctionsStandard {
    private static final DateTimePeriodHelper PeriodHelper = DateTimePeriodHelper.all();

    @MethodDescriptor("Check whether the given string is null or blank.")
    @ReturnDescriptor("`true` if given string is null or blank, `false` otherwise.")
    public static final boolean isBlank(
            @ParamDescriptor("The string to check.") String s
    ) {
        return StringUtils.isBlank(s);
    }

    @MethodDescriptor("Check whether the given string is not null and not blank.")
    @ReturnDescriptor("`false` if given string is null or blank, `true` otherwise.")
    public static final boolean isNotBlank(
            @ParamDescriptor("The string to check.") String s
    ) {
        return StringUtils.isNotBlank(s);
    }

    @MethodDescriptor("Get the substring before the first occurrence of a separator. The separator is not returned.")
    @ReturnDescriptor("The substring before the first occurrence of the separator, or `null` if input string is `null`.")
    public static final String substringBefore(
            @ParamDescriptor("The string to get a substring from.") String s, 
            @ParamDescriptor("The separator to search for.") String separator
    ) {
        return StringUtils.substringBefore(s, separator);
    }

    @MethodDescriptor("Get the substring after the first occurrence of a separator. The separator is not returned.")
    @ReturnDescriptor("The substring after the first occurrence of the separator, or `null` if input string is `null`.")
    public static final String substringAfter(
            @ParamDescriptor("The string to get a substring from.") String s, 
            @ParamDescriptor("The separator to search for.") String separator
    ) {
        return StringUtils.substringAfter(s, separator);
    }

    @MethodDescriptor("Abbreviate the given string to the given maximum length using ellipses, like 'This is...'.")
    @ReturnDescriptor("The original string abbreviated to the given maximum length if necessary.")
    public static final String abbreviate(
            @ParamDescriptor("The string to abbreviate.") String s,
            @ParamDescriptor("The maximum length of the result string, must be at least 4.") int maxLength
    ) {
        return StringUtils.abbreviate(s, maxLength);
    }

    @MethodDescriptor("Check whether debug logging is currently enabled.")
    @ReturnDescriptor("`true` if debug logging is enabled; `false` otherwise.")
    public static final boolean isDebugEnabled() {
        return DebugHelper.isDebugEnabled();
    }

    @MethodDescriptor("Generate a new random UUID (Universally Unique Identifier) as a string.")
    @ReturnDescriptor("A randomly generated UUID string in standard 36-character format.") 
    public static final String uuid() {
        return UUID.randomUUID().toString();
    }

    @MethodDescriptor("""
            Format a string using the specified format string and arguments, returning the formatted string.
            See https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Formatter.html#syntax
            for details on format string syntax.
            """)
    @ReturnDescriptor("The formatted string resulting from applying `fmt` to the `input` arguments.")
    public static final String fmt(
            @ParamDescriptor("The format string.") String fmt,
            @ParamDescriptor("The arguments referenced by the format specifiers in the format string.") Object... input
    ) {
        return String.format(fmt, input);
    }

    @MethodDescriptor("""
            Parse the given string as a Java `OffsetDateTime` object, for example to allow for date/time
            comparisons, formatting, or retrieval of individual elements like month or year. See 
            https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/OffsetDateTime.html for
            information on methods that can be called on the returned object.
            """)
    @ReturnDescriptor("""
            The parsed `OffsetDateTime` instance representing the date/time encoded in the string, 
            or `null` if input is `null`.
            """)
    public static final OffsetDateTime date(
            @ParamDescriptor("The string to parse into an `OffsetDateTime`; may be `null`.") String s
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

    @MethodDescriptor("""
            Return the current date/time as a Java `OffsetDateTime` object, optionally adjusted by a given period.
            """)
    @ReturnDescriptor("""
            The current date/time as a Java `OffsetDateTime` object, optionally adjusted by the given period.
            """)
    public static final OffsetDateTime now(
            @ParamDescriptor("Optional period string like +1d or -5m to adjust the current time.") String... s
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

    @MethodDescriptor("""
            Retrieve the contents of an fcli variable that was stored through the --store option
            on a previous fcli invocation, throwing an error if the variable does not exist.
            """)
    @ReturnDescriptor("The JSON contents for the given fcli variable name.")
    public static final JsonNode var(
            @ParamDescriptor("The name of the variable to retrieve.") String name
    ) {
        return FcliVariableHelper.getVariableContents(name, true);
    }

    @MethodDescriptor("""
            Retrieve the value of the given environment variable.
            """)
    @ReturnDescriptor("""
            The value of the given environment variable, or `null` if the environment 
            variable is not set or its value is blank.
            """)
    public static final String env(
            @ParamDescriptor("The name of the environment variable to retrieve.") String name
    ) {
        if (StringUtils.isBlank(name)) {
            throw new FcliSimpleException("Environment variable name passed to #env may not be null");
        }
        var result = EnvHelper.env(name);
        // Return null in case of blank string
        return StringUtils.isBlank(result) ? null : result;
    }

    @MethodDescriptor("Encrypt the given string using the configured encryption helper.")
    @ReturnDescriptor("The encrypted form of the input string.")
    public static final String encrypt(
            @ParamDescriptor("The string to encrypt.") String s
    ) {
        return EncryptionHelper.encrypt(s);
    }

    @MethodDescriptor("Decrypt the given encrypted string using the configured decryption helper.")
    @ReturnDescriptor("The decrypted form of the input string.")
    public static final String decrypt(
            @ParamDescriptor("The encrypted string to decrypt.") String s
    ) {
        return EncryptionHelper.decrypt(s);
    }

}
