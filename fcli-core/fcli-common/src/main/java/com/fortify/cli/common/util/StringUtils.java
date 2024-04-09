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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.formkiq.graalvm.annotations.Reflectable;

@Reflectable // Required for using these functions in fcli actions
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
        if ( str==null ) { return null; }
        final int pos = str.indexOf(separator);
        return pos==-1 ? str : str.substring(0, pos);
    }

    public static final String substringAfter(String str, String separator) {
        if ( str==null ) { return null; }
        final int pos = str.indexOf(separator);
        return pos==-1 ? "" : str.substring(pos + separator.length());
    }

    public static final String substringAfterLast(String str, String separator) {
        if ( str==null ) { return null; }
        final int pos = str.lastIndexOf(separator);
        return pos==-1 ? "" : str.substring(pos + separator.length());
    }

    public static final String capitalize(String str) {
        return str==null
                ? null
                : str.substring(0,1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public static final String abbreviate(String str, int maxLength) {
        if ( str==null ) { return null; }
        if (str.length() <= maxLength) {
            return str;
        } else {
            return str.substring(0, maxLength-3) + "...";
        }
    }
    
    public static final String indent(String str, String indentStr) {
        if ( str==null ) { return null; }
        return Stream.of(str.split("\n")).collect(Collectors.joining("\n"+indentStr, indentStr, ""));
    }
    
    // For use in SpEL expressions
    public static final String fmt(String fmt, Object... input) {
        return String.format(fmt, input);
    }
}
