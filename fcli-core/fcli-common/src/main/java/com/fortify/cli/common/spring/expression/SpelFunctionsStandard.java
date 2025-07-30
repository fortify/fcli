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
import com.fortify.cli.common.action.schema.annotations.MethodDescription;
import com.fortify.cli.common.action.schema.annotations.ParamDescription;
import com.fortify.cli.common.action.schema.annotations.ReturnDescription;
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
    
    @MethodDescription("Checks whether debug logging is currently enabled.")
    public static final @ReturnDescription("`true` if debug logging is enabled; `false` otherwise") boolean isDebugEnabled() {
        return DebugHelper.isDebugEnabled();
    }

	@MethodDescription("Generates a new random UUID (Universally Unique Identifier) as a string.")
	public static final @ReturnDescription("a randomly generated UUID string in standard 36-character format") String uuid() {
		return UUID.randomUUID().toString();
	}
    
	@MethodDescription("Formats a string using the specified format string and arguments, "
			+ "returning the formatted string.")
	public static final @ReturnDescription("the formatted string resulting from applying `fmt` to the `input` arguments") String fmt(
			@ParamDescription("the format string") String fmt,
			@ParamDescription("the arguments referenced by the format specifiers in the format string") Object... input) {
		return String.format(fmt, input);
	}
    
	@MethodDescription("Parses the given string `s` into an {@link OffsetDateTime}. "
			+ "If the string can be parsed as an OffsetDateTime, it is returned directly. "
			+ "If parsing as an OffsetDateTime fails, the string is parsed as a {@link LocalDate}, "
			+ "and the resulting date is converted to an OffsetDateTime at the start of the day with UTC offset. "
			+ "If the input string `s` is `null`, this method returns `null`.")
	public static final @ReturnDescription("the parsed {@link OffsetDateTime} instance representing the date/time encoded in the string, "
			+ "or `null` if `s` is `null`") OffsetDateTime date(
					@ParamDescription("the string to parse into an OffsetDateTime; may be `null`") String s) {
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
    
	@MethodDescription("Returns the current {@link OffsetDateTime}, optionally adjusted by a period string. "
			+ "If no argument is provided or the argument is blank, returns the current date-time. "
			+ "If a single argument starting with '+' is provided, returns the current date-time plus the specified period. "
			+ "If a single argument starting with '-' is provided, returns the current date-time minus the specified period. "
			+ "If more than one argument is provided, or if the period format is invalid, an exception is thrown.")
	public static final @ReturnDescription("the current {@link OffsetDateTime} adjusted by the given period, "
			+ "or the current date-time if no valid period is provided") OffsetDateTime now(
					@ParamDescription("an optional single-element array with a period string "
							+ "starting with '+' or '-' to adjust the current time") String... s) {
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
    
	@MethodDescription("Retrieves the contents of a variable by its name from the FCLI variable helper. "
			+ "The retrieval enforces that the variable must exist, throwing an error if not found.")
	public static final @ReturnDescription("the {@link JsonNode} contents of the variable identified by `name`") JsonNode var(
			@ParamDescription("the name of the variable to retrieve; must not be null or empty") String name) {
		return FcliVariableHelper.getVariableContents(name, true);
	}
    
	@MethodDescription("Retrieves the value of the specified environment variable by name. "
			+ "Throws an exception if the provided variable name is blank or null. "
			+ "If the environment variable exists but its value is blank, this method returns `null`.")
	public static final @ReturnDescription("the value of the environment variable identified by `name`, "
			+ "or `null` if the environment variable is not set or its value is blank") String env(
					@ParamDescription("the name of the environment variable to retrieve; must not be blank or null") String name) {
		if (StringUtils.isBlank(name)) {
			throw new FcliSimpleException("Environment variable name passed to #env may not be null");
		}
		var result = EnvHelper.env(name);
		// Return null in case of blank string
		return StringUtils.isBlank(result) ? null : result;
	}

	@MethodDescription("Encrypts the given string using the configured encryption helper.")
	public static final @ReturnDescription("the encrypted form of the input string") String encrypt(
			@ParamDescription("the string to encrypt") String s) {
		return EncryptionHelper.encrypt(s);
	}

	@MethodDescription("Decrypts the given encrypted string using the configured decryption helper.")
	public static final @ReturnDescription("the decrypted form of the input string") String decrypt(
			@ParamDescription("the encrypted string to decrypt") String s) {
		return EncryptionHelper.decrypt(s);
	}
    
}
