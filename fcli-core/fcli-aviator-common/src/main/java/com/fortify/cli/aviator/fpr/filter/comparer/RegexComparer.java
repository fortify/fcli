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