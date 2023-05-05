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
}
