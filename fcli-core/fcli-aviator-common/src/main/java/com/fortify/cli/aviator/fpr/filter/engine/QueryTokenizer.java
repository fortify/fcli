package com.fortify.cli.aviator.fpr.filter.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs Lexical Analysis on a filter query string.
 * Its only job is to break the raw string into a flat list of tokens.
 */
public final class QueryTokenizer {

    private static final Pattern TOKEN_BOUNDARY_FINDER = Pattern.compile(
            "\\b(AND|OR)\\b|(!?\\s*(?:\\[[^\\]]+\\]|\\w+):)",
            Pattern.CASE_INSENSITIVE
    );

    public static List<String> tokenize(String query) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_BOUNDARY_FINDER.matcher(query);
        int lastEnd = 0;

        while (matcher.find()) {
            // Add the text that came BEFORE this token (which is a value)
            if (matcher.start() > lastEnd) {
                tokens.add(query.substring(lastEnd, matcher.start()).trim());
            }
            // Add the token that was found (an operator or a modifier)
            tokens.add(matcher.group().trim());
            lastEnd = matcher.end();
        }

        // Add any remaining text after the last token (the final value)
        if (lastEnd < query.length()) {
            tokens.add(query.substring(lastEnd).trim());
        }

        return tokens;
    }
}