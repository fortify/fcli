/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.util;

public class StringUtils {
    private StringUtils() {}

    public static final boolean isBlank(String s) {
        return s==null || s.isBlank();
    }

    public static final boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    public static String ifBlank(String s, String defaultValue) {
        return isBlank(s) ? defaultValue : s;
    }

    public static final String substringBefore(String str, String separator) {
        final int pos = str.indexOf(separator);
        return pos==-1 ? str : str.substring(0, pos);
    }

    public static final String substringAfter(String str, String separator) {
        final int pos = str.indexOf(separator);
        return pos==-1 ? "" : str.substring(pos + separator.length());
    }

    public static final String substringAfterLast(String str, String separator) {
        final int pos = str.lastIndexOf(separator);
        return pos==-1 ? "" : str.substring(pos + separator.length());
    }

    public static final String capitalize(String str) {
        return str==null
                ? null
                : str.substring(0,1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public static String abbreviate(String input, int maxLength) {
        if (input.length() <= maxLength) {
            return input;
        } else {
            return input.substring(0, maxLength);
        }
    }
}
