package com.fortify.cli.aviator.fpr.filter.comparer;

import java.util.regex.Pattern;

public class RegexComparer implements SearchComparer {
    private final Pattern pattern;

    public RegexComparer(String regex) {
        pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE); // library case_insensitive
    }

    @Override
    public boolean matches(Object attributeValue) {
        if (attributeValue == null) return false;
        boolean result = pattern.matcher(str(attributeValue)).find(); // change to find
        return result;
    }

    @Override
    public String getSearchTerm() {
        return "";
    }

    private String str(Object o) {
        return o instanceof String ? (String) o : o.toString();
    }
}