package com.fortify.cli.fod.rest.query;

public class FoDFiltersParamValueGenerators {
    public static final String wrapInQuotes(String value) {
        return String.format("\"%s\"", value);
    }
    
    public static final String plain(String value) {
        return value;
    }
}
