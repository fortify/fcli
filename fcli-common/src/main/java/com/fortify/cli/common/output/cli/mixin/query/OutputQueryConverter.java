package com.fortify.cli.common.output.cli.mixin.query;

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
            return new OutputQuery(m.group(1), OutputQueryOperator.valueOfOperator(m.group(2)), m.group(3));
        }
    }

    private static final Pattern generateQueryPattern() {
        String patternString = String.format("^\\{(.+?)\\}(%s){1}+(.+?)$", generateOperatorsPattern());
        return Pattern.compile(patternString);
    }

    private static final String generateOperatorsPattern() {
        return String.join("|", OutputQueryOperator.operators());
    }

}
