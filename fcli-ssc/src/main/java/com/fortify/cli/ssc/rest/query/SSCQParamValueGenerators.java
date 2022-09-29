package com.fortify.cli.ssc.rest.query;

public class SSCQParamValueGenerators {
    public static final String wrapInQuotes(String value) {
        return String.format("\"%s\"", value);
    }
    
    public static final String plain(String value) {
        return value;
    }
}
