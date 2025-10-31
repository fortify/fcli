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
package com.fortify.cli.common.spel.fn;

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.date;
import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.fcli;
import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.txt;
import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.util;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.integration.json.JsonPropertyAccessor.JsonNodeWrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.crypto.helper.EncryptionHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DebugHelper;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.variable.FcliVariableHelper;

import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
public class SpelFunctionsStandard {
    private static final DateTimePeriodHelper PeriodHelper = DateTimePeriodHelper.all();

    @SpelFunction(cat=txt, returns="`true` if given string is null or blank, `false` otherwise")
    public static final boolean isBlank(
            @SpelFunctionParam(name="input", desc="the string to check") String s) 
    {
        return StringUtils.isBlank(s);
    }
    
    @SpelFunction(cat=txt, returns="The first string if it's not blank, otherwise the second string")
    public static final String ifBlank(
            @SpelFunctionParam(name="input", desc="the string to return if not blank") String s1,
            @SpelFunctionParam(name="default", desc="the string to return if first string is blank") String s2)
    {
        return StringUtils.defaultIfBlank(s1, s2);
    }
    
    @SpelFunction(cat=txt, returns="`false` if given string is null or blank, `true` otherwise")
    public static final boolean isNotBlank(
            @SpelFunctionParam(name="input", desc="the string to check") String s) 
    {
        return StringUtils.isNotBlank(s);
    }

    @SpelFunction(cat=txt, returns="The substring before the first occurrence of the separator, or `null` if input string is `null`")
    public static final String substringBefore(
            @SpelFunctionParam(name="input", desc="the string to get a substring from") String s, 
            @SpelFunctionParam(name="separator", desc="the separator to search for") String separator)
    {
        return StringUtils.substringBefore(s, separator);
    }

    @SpelFunction(cat=txt, returns="The substring after the first occurrence of the separator, or `null` if input string is `null`")
    public static final String substringAfter(
            @SpelFunctionParam(name="input", desc="the string to get a substring from") String s, 
            @SpelFunctionParam(name="separator", desc="the separator to search for") String separator) 
    {
        return StringUtils.substringAfter(s, separator);
    }

    @SpelFunction(cat=txt, returns="The input string abbreviated to the given maximum length, with any remaining text replaced by '...'")
    public static final String abbreviate(
            @SpelFunctionParam(name="input", desc="the string to abbreviate") String s,
            @SpelFunctionParam(name="maxLength", desc="the maximum length of the result string, must be at least 4") int maxLength)
    {
        return StringUtils.abbreviate(s, maxLength);
    }
    
    @SpelFunction(cat=txt, returns="String consisting of the joined elements, separated by the given delimiter")
    public static final String join(
            @SpelFunctionParam(name="delimiter", desc="the delimiter to be used between each element") String delimiter,
            @SpelFunctionParam(name="input", desc="the elements to join", type = "array") Object source)
    {
        switch (delimiter) {
        case "\\n":
            delimiter = "\n";
            break;
        case "\\t":
            delimiter = "\t";
            break;
        }
        Stream<?> stream = null;
        if (source instanceof Collection) {
            stream = ((Collection<?>) source).stream();
        } else if (source instanceof ArrayNode) {
            stream = JsonHelper.stream((ArrayNode) source);
        }
        return stream == null ? "" : stream.map(SpelFunctionsStandard::toString).collect(Collectors.joining(delimiter));
    }
    
    @SpelFunction(cat=txt, returns="String consisting of the joined elements separated by the given delimiter _if all elements are non-null_; otherwise `null`")
    public static final String joinOrNull(
            @SpelFunctionParam(name="delimiter", desc="the delimiter to be used between each element") String delimiter,
            @SpelFunctionParam(name="input", desc="the elements to join") String... parts) 
    {
        if (parts == null || Arrays.asList(parts).stream().anyMatch(Objects::isNull)) {return null;}
        return String.join(delimiter, parts);
    }
    
    @SpelFunction(cat=txt, desc = "Returns a literal regex pattern string for the given input string, escaping any characters that have a special meaning in regular expressions.",
            returns="The regex-quoted string")
    public static final String regexQuote(
            @SpelFunctionParam(name="input", desc="the string to be quoted") String s)
    {
        return Pattern.quote(s);
    }
    
    @SpelFunction(cat=txt, desc = """
            Replaces all occurrences in the input string based on regex patterns and replacement
            values provided in the mapping object.
            """,
            returns="The input string with all replacements applied")
    public static final String replaceAllFromRegExMap(
            @SpelFunctionParam(name="input", desc="the input string on which to apply replacements") String s,
            @SpelFunctionParam(name="replacements", desc="map containing regex patterns as keys and replacement strings as values", type = "map<string,string>") Object mappingObject)
    {
        var mappingNode = mappingObject instanceof ObjectNode ? (ObjectNode) mappingObject
                : mappingObject instanceof JsonNodeWrapper ? ((JsonNodeWrapper<?>) mappingObject).getRealNode()
                    : JsonHelper.getObjectMapper().valueToTree(mappingObject);
        if (!mappingNode.isObject()) {
            throw new FcliTechnicalException("replaceAllFromRegExMap must be called with Map or ObjectNode, actual type: "
                    + mappingObject.getClass().getSimpleName());
        }
        var fields = ((ObjectNode) mappingNode).fields();
        while (fields.hasNext()) {
            var field = fields.next();
            s = s.replaceAll(field.getKey(), field.getValue().asText());
        }
        return s;
    }
    
    @SpelFunction(cat=txt, desc = "Generates a numbered list from the given list of elements.",
            returns="Numbered list of input elements, each on a new line") 
    public static final String numberedList(
            @SpelFunctionParam(name="input", desc="the list of elements to be numbered and joined") List<Object> elts)
    {
        StringBuilder builder = new StringBuilder();
        for (var i = 0; i < elts.size(); i++) {
            builder.append(i + 1).append(". ").append(elts.get(i)).append('\n');
        }
        return builder.toString();
    }

    @SpelFunction(cat=fcli, returns="`true` if debug logging is enabled; `false` otherwise")
    public static final boolean isDebugEnabled() {
        return DebugHelper.isDebugEnabled();
    }

    @SpelFunction(cat=util, returns="A randomly generated UUID string in standard 36-character format") 
    public static final String uuid() {
        return UUID.randomUUID().toString();
    }

    @SpelFunction(cat=txt, desc = """
            Formats a string using the specified format string and arguments, returning the formatted string. \
            See https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Formatter.html#syntax \
            for details on format string syntax.
            """,
            returns="The formatted string")
    public static final String fmt(
            @SpelFunctionParam(name="fmt", desc="the format string") String fmt,
            @SpelFunctionParam(name="args", desc="the arguments referenced by the format specifiers in the format string") Object... args)
    {
        return String.format(fmt, args);
    }

    @SpelFunction(cat=date, desc = """
            Parses the given string as a Java `OffsetDateTime` object, for example to allow for date/time \
            comparisons, formatting, or retrieval of individual elements like month or year. See \
            https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/OffsetDateTime.html for \
            information on methods that can be invoked on the returned object.
            """,
            returns="""
            The parsed `OffsetDateTime` instance representing the date/time encoded in the string, \
            or `null` if input is `null`
            """)
    public static final OffsetDateTime date(
            @SpelFunctionParam(name="input", desc="the string to parse into an `OffsetDateTime`; may be `null`") String s)
    {
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

    @SpelFunction(cat=date, returns="""
            The current date/time as a Java `OffsetDateTime` object, optionally adjusted by the given period
            """)
    public static final OffsetDateTime now(
            @SpelFunctionParam(name="period", desc="optional period string like +1d or -5m to adjust the current time", type="string", optional=true) String... s)
    {
        if (s != null && s.length > 1) {
            throw new FcliSimpleException("#now([period]) only takes up to one argument");
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

    @SpelFunction(cat=fcli, desc = """
            Retrieves the contents of an fcli variable that was stored through the --store option \
            on a previous fcli invocation, throwing an error if the variable does not exist.
            """,
                returns="The JSON contents for the given fcli variable name")
    public static final JsonNode var(
            @SpelFunctionParam(name="name", desc="the name of the variable to retrieve") String name)
    {
        return FcliVariableHelper.getVariableContents(name, true);
    }

    @SpelFunction(cat=util, returns="""
            The value of the given environment variable, or `null` if the environment \
            variable is not set or its value is blank
            """)
    public static final String env(
            @SpelFunctionParam(name="name", desc="the name of the environment variable to retrieve") String name)
    {
        if (StringUtils.isBlank(name)) {
            throw new FcliSimpleException("Environment variable name passed to #env may not be null");
        }
        var result = EnvHelper.env(name);
        // Return null in case of blank string
        return StringUtils.isBlank(result) ? null : result;
    }

    @SpelFunction(cat=fcli, desc = "Encrypts the given string using the fcli encryption mechanism, compatible with the 'fcli util crypto' commands.",
                returns="The encrypted form of the input string")
    public static final String encrypt(
            @SpelFunctionParam(name="input", desc="the string to encrypt") String s)
    {
        return EncryptionHelper.encrypt(s);
    }

    @SpelFunction(cat=fcli, desc = "Decrypts the given encrypted string using the fcli decryption mechanism, compatible with the 'fcli util crypto' commands.",
                returns="The decrypted form of the input string")
    public static final String decrypt(
            @SpelFunctionParam(name="input", desc="the encrypted string to decrypt") String s)
    {
        return EncryptionHelper.decrypt(s);
    }
    
    private static final String toString(Object o) {
        if ( o==null ) {
            return "";
        } else if ( o instanceof JsonNode ) {
            return ((JsonNode)o).asText();
        } else {
            return o.toString();
        }
    }
}
