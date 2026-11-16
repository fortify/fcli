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
package com.fortify.cli.common.spel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.spel.wrapper.SimpleExpression;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

class FcliSpelExpressionParserTest {
    private final FcliSpelExpressionParser parser = new FcliSpelExpressionParser();

    static Stream<Arguments> integerLiterals() {
        return Stream.of(
                Arguments.of("0", 0),
                Arguments.of("2147483647", Integer.MAX_VALUE),
                Arguments.of("-2147483647", -Integer.MAX_VALUE),
                Arguments.of("2147483648", 2147483648L),
                Arguments.of("-2147483648", -2147483648L),
                Arguments.of("3000000000", 3000000000L),
                Arguments.of("+3000000000", 3000000000L),
                Arguments.of("- 3000000000", -3000000000L),
                Arguments.of("00003000000000", 3000000000L),
                Arguments.of("9223372036854775807", Long.MAX_VALUE),
                Arguments.of("-9223372036854775807", -Long.MAX_VALUE),
                Arguments.of("(-9223372036854775807 - 1)", Long.MIN_VALUE),
                Arguments.of("0x7fffffff", Integer.MAX_VALUE),
                Arguments.of("0x80000000", 2147483648L),
                Arguments.of("0XFFFFFFFF", 4294967295L),
                Arguments.of("-0x80000000", -2147483648L),
                Arguments.of("0x7fffffffffffffff", Long.MAX_VALUE),
                Arguments.of("1L", 1L),
                Arguments.of("3000000000l", 3000000000L));
    }

    @ParameterizedTest
    @MethodSource("integerLiterals")
    void parsesIntegerLiteralsWithoutChangingSmallIntegerTypes(String source, Number expected) {
        var expression = parser.parseRaw(source);
        assertEquals(expected, expression.getValue());
        assertEquals(source, expression.getExpressionString());
    }

    @Test
    void repairsOnlyNumericOccurrencesAndCanRepairTheSameValueRepeatedly() {
        var source = "{'3000000000', 3000000000, \"3000000000\", 3000000000, 0xFFFFFFFF, '0xFFFFFFFF'}";
        assertEquals(List.of("3000000000", 3000000000L, "3000000000", 3000000000L, 4294967295L, "0xFFFFFFFF"),
                parser.parseExpression(source).getValue());
    }

    @Test
    void preservesEscapedQuotesAndOtherNumericSyntax() {
        var source = "{'it''s 3000000000', \"say \"\"3000000000\"\"\", 3000000000, 3000000000.5, 3e9, 3e9F}";
        assertEquals(List.of("it's 3000000000", "say \"3000000000\"", 3000000000L, 3000000000.5, 3e9, 3e9F),
                parser.parseExpression(source).getValue());
    }

    @Test
    void preservesIdentifiersIndexesAndIntegerMapKeys() {
        var context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
        context.setVariable("id3000000000", 3000000000L);
        assertEquals(true, parser.parseExpression("#id3000000000 == 3000000000").getValue(context));
        assertEquals("first", parser.parseExpression("{'first', 'second'}[0]").getValue());
        assertEquals("value", parser.parseExpression("{1:'value'}[1]").getValue());
        assertEquals("3000000000", parser.parseExpression("3000000000.toString()").getValue());
    }

    @Test
    void supportsArithmeticAndMultipleTemplateFragments() {
        assertEquals(6000000001L, parser.parseExpression("3000000000 + 3000000001").getValue());
        var source = "3000000000: ${3000000000} / ${3000000001} / ${'3000000000'}";
        var expression = parser.parseTemplateExpression(source);
        assertEquals("3000000000: 3000000000 / 3000000001 / 3000000000", expression.getValue());
        assertEquals(source, expression.getExpressionString());
        assertEquals(source, expression.getOriginalExpressionString());
        assertEquals("3000000000: 3000000000",
                parser.parseExpression("3000000000: {3000000000}", new TemplateParserContext("{", "}")).getValue());
        assertEquals("3000000000", parser.parseTemplateExpression("3000000000").getValue());
        assertEquals("", parser.parseTemplateExpression("").getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"9223372036854775808", "-9223372036854775808", "999999999999999999999999",
            "0x8000000000000000", "-0x8000000000000000", "9223372036854775808L"})
    void rejectsValuesOutsideTheLongLiteralRange(String source) {
        var error = assertThrows(SpelParseException.class, () -> parser.parseExpression(source));
        assertEquals(SpelMessage.NOT_A_LONG, error.getMessageCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"'unterminated", "1 +", "0x", "3.4L", "1LL", "3000000000Lfoo"})
    void preservesOtherParseErrors(String source) {
        var expected = assertThrows(SpelParseException.class, () -> new SpelExpressionParser().parseExpression(source));
        var actual = assertThrows(SpelParseException.class, () -> parser.parseExpression(source));
        assertEquals(expected.getMessageCode(), actual.getMessageCode());
        assertEquals(expected.getPosition(), actual.getPosition());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1e", "1e+"})
    void propagatesNonSpelParserFailuresWithoutRecovery(String source) {
        var expected = assertThrows(RuntimeException.class, () -> new SpelExpressionParser().parseExpression(source));
        var actual = assertThrows(RuntimeException.class, () -> parser.parseExpression(source));
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected.getMessage(), actual.getMessage());
    }

    @Test
    void mapsErrorsAfterMultipleRepairsBackToTheOriginalSource() {
        var source = "{3000000000, 0xFFFFFFFF, 4000000000, )}";
        var normalized = "{3000000000L, 0xFFFFFFFFL, 4000000000L, )}";
        var expected = assertThrows(SpelParseException.class, () -> new SpelExpressionParser().parseExpression(normalized));
        var actual = assertThrows(SpelParseException.class, () -> parser.parseExpression(source));
        assertEquals(expected.getMessageCode(), actual.getMessageCode());
        assertEquals(source.indexOf(')'), actual.getPosition());
        assertEquals(source, actual.getExpressionString());
        assertInstanceOf(SpelParseException.class, actual.getCause());
    }

    @Test
    void mapsLongOverflowAfterEarlierRepairsBackToTheOriginalSource() {
        var source = "{3000000000, 9223372036854775808}";
        var error = assertThrows(SpelParseException.class, () -> parser.parseExpression(source));
        assertEquals(SpelMessage.NOT_A_LONG, error.getMessageCode());
        assertEquals(source.indexOf("922337"), error.getPosition());
    }

    @Test
    void retainsAstForValidationAndDoesNotBypassEvaluationRestrictions() {
        var expression = parser.parseRaw("3000000000 == 3000000000");
        assertEquals("OpEQ", expression.getAST().getClass().getSimpleName());
        var restricted = SimpleEvaluationContext.forReadOnlyDataBinding().build();
        assertThrows(SpelEvaluationException.class,
                () -> parser.parseExpression("T(java.lang.Long).valueOf(3000000000)").getValue(restricted));
    }

    @Test
    void honorsParserConfigurationAndInvalidInputs() {
        var configuration = new SpelParserConfiguration(null, null, false, false, 0, 11);
        var configured = new FcliSpelExpressionParser(configuration);
        assertEquals(3000000000L, configured.parseExpression("3000000000").getValue());
        assertThrows(SpelEvaluationException.class, () -> configured.parseExpression("300000000000"));
        assertThrows(IllegalArgumentException.class, () -> parser.parseExpression(""));
        assertThrows(IllegalArgumentException.class, () -> parser.parseExpression(" "));
        assertThrows(IllegalArgumentException.class, () -> parser.parseExpression(null));
    }

    @Test
    void roundTripsSimpleAndTemplateExpressionsWithoutAddingSuffixes() throws Exception {
        var mapper = new ObjectMapper();
        var simpleJson = mapper.writeValueAsString("3000000000");
        var simple = mapper.readValue(simpleJson, SimpleExpression.class);
        assertEquals(3000000000L, simple.getValue());
        assertEquals(simpleJson, mapper.writeValueAsString(simple));
        var templateJson = mapper.writeValueAsString("id: ${3000000000}");
        var template = mapper.readValue(templateJson, TemplateExpression.class);
        assertEquals("id: 3000000000", template.getValue());
        assertEquals(templateJson, mapper.writeValueAsString(template));
    }

    @Test
    void sharesTheParserSafelyAcrossThreads() {
        assertTrue(IntStream.range(0, 100).parallel().allMatch(i -> {
            var source = Long.toString(3000000000L + i);
            var expression = parser.parseRaw(source);
            return expression.getValue().equals(3000000000L + i) && source.equals(expression.getExpressionString());
        }));
    }
}
