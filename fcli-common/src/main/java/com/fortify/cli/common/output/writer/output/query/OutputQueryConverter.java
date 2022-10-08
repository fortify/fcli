package com.fortify.cli.common.output.writer.output.query;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import picocli.CommandLine.ITypeConverter;

public class OutputQueryConverter implements ITypeConverter<OutputQuery> {
    private static final Pattern QUERY_PATTERN = generateQueryPattern();  

    @Override
    public OutputQuery convert(String value) throws Exception {
        Matcher m = QUERY_PATTERN.matcher(value);
        if ( !m.matches()  ) {
            throw new IllegalArgumentException("Query expression '"+value+"' doesn't match pattern "+QUERY_PATTERN.pattern());
        } else {
            String propertyPath = m.group(1)!=null ? m.group(1) : m.group(2);
            OutputQueryOperator operator = OutputQueryOperator.valueOfOperator(m.group(3));
            String valueToMatch = m.group(4);
            return new OutputQuery(propertyPath, operator, valueToMatch);
        }
    }

    private static final Pattern generateQueryPattern() {
        // Match either any (JSON path) expression embedded in curly braces, or a simple (nested) property path,
        // followed by any of the available operators, followed by the value to be matched.
        String patternString = String.format("^(?:(?:\\{(.+?)\\})|([a-zA-Z0-9\\.]+?))(%s){1}+(.+?)$", generateOperatorsPattern());
        return Pattern.compile(patternString);
    }

    private static final String generateOperatorsPattern() {
        return String.join("|", OutputQueryOperator.operators());
    }

}
