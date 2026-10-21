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

import java.util.ArrayList;
import java.util.List;

import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fortify.cli.common.spel.wrapper.SimpleExpression;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

/**
 * SpEL parser that promotes overflowing unsuffixed decimal and hexadecimal integer literals to longs.
 * Small integer literals retain their standard SpEL type.
 *
 * <p>Spring's parse errors identify the exact numeric token to retry with an {@code L} suffix,
 * avoiding custom tokenization or replacements inside quoted strings. Each promoted literal
 * requires another parse attempt; successfully parsed expressions have no additional evaluation cost.
 * Values outside the range of a long literal remain errors. Since SpEL parses a minus sign separately,
 * {@code Long.MIN_VALUE} must be expressed as {@code (-9223372036854775807 - 1)}.</p>
 *
 * <p>Instances are reusable and thread-safe; all recovery state is local to a parse invocation.</p>
 */
public final class FcliSpelExpressionParser extends SpelExpressionParser {
    private static final TemplateParserContext TEMPLATE_CONTEXT = new TemplateParserContext("${", "}");
    private final SpelParserConfiguration configuration;

    public FcliSpelExpressionParser() {
        this(new SpelParserConfiguration());
    }

    public FcliSpelExpressionParser(SpelParserConfiguration configuration) {
        super(configuration);
        this.configuration = configuration;
    }

    /**
     * Parses a non-template expression, retaining its original source in the fcli wrapper.
     */
    public SimpleExpression parseSimpleExpression(String source) {
        return new SimpleExpression(source, parseExpression(source));
    }

    /**
     * Parses a template with {@code ${...}} expression fragments; text outside the fragments remains literal.
     */
    public TemplateExpression parseTemplateExpression(String source) {
        return new TemplateExpression(source, parseExpression(source, TEMPLATE_CONTEXT));
    }

    /**
     * Parses a single expression, retrying only integer-literal overflow failures.
     * Spring also invokes this hook separately for each trimmed expression fragment in a template,
     * so error positions and recovery state are relative to that fragment, not the whole template.
     */
    @Override
    protected SpelExpression doParseExpression(String expressionString, ParserContext context) {
        var source = expressionString;
        List<Integer> insertions = new ArrayList<>();
        while (true) {
            try {
                var parsed = super.doParseExpression(source, context);
                // Keep the standard SpEL AST for visitors, but expose the source without inserted suffixes.
                // AST node positions still refer to the internally rewritten source.
                return insertions.isEmpty() ? parsed
                        : new SpelExpression(expressionString, (SpelNodeImpl)parsed.getAST(), configuration);
            } catch (SpelParseException e) {
                int end = getIntegerTokenEnd(source, e);
                if (end < 0) {
                    throw mapParseException(expressionString, e, insertions);
                }
                source = source.substring(0, end) + "L" + source.substring(end);
                recordInsertion(insertions, end);
                // The next attempt either succeeds, reports another token, or fails with a non-recoverable error.
            }
        }
    }

    /**
     * Records suffix positions in the current rewritten source, shifting earlier entries if necessary.
     */
    private static void recordInsertion(List<Integer> insertions, int position) {
        for (int i = 0; i < insertions.size(); i++) {
            if (insertions.get(i) >= position) {
                insertions.set(i, insertions.get(i) + 1);
            }
        }
        insertions.add(position);
    }

    /**
     * Reports an unrecoverable parse error against the original source while retaining Spring's error as the cause.
     * Errors preceding any rewrite are returned unchanged.
     */
    private static SpelParseException mapParseException(String originalSource, SpelParseException error,
            List<Integer> insertions) {
        if (insertions.isEmpty()) {
            return error;
        }
        var mapped = new SpelParseException(originalSource, originalPosition(error.getPosition(), insertions),
                error.getMessageCode(), error.getInserts());
        mapped.initCause(error);
        return mapped;
    }

    /**
     * Returns the exclusive end of the overflowing integer token, or {@code -1} if recovery is unsupported.
     * Uses structured error metadata rather than localized message text; long overflow is deliberately not retried.
     */
    private static int getIntegerTokenEnd(String source, SpelParseException error) {
        if (error.getMessageCode() != SpelMessage.NOT_AN_INTEGER
                || error.getInserts().length != 1 || !(error.getInserts()[0] instanceof String token)
                || token.isEmpty()) {
            return -1;
        }
        int position = error.getPosition();
        if (position < 0 || position >= source.length()) {
            return -1;
        }
        return getIntegerTokenEnd(source, position, token);
    }

    /**
     * Verifies Spring's reported token against the current source before choosing where to insert the suffix.
     * Metadata mismatches and already-suffixed tokens are rejected rather than repaired speculatively.
     */
    private static int getIntegerTokenEnd(String source, int start, String token) {
        // Hexadecimal error payloads omit the prefix, but their source position includes it.
        if (source.startsWith("0x", start) || source.startsWith("0X", start)) {
            start += 2;
        }
        int end = start + token.length();
        if (!source.startsWith(token, start) || end > source.length()
                || (end < source.length() && (source.charAt(end) == 'L' || source.charAt(end) == 'l'))) {
            return -1;
        }
        return end;
    }

    /**
     * Maps a position back by discounting inserted suffixes strictly before it.
     * A negative position (Spring's unknown-position marker) is preserved.
     */
    private static int originalPosition(int position, List<Integer> insertions) {
        int result = position;
        for (int insertion : insertions) {
            if (insertion < position) {
                result--;
            }
        }
        return result;
    }
}
