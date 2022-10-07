package com.fortify.cli.common.util;

public class StringUtils {
    private StringUtils() {}
    
    public static final boolean isBlank(String s) {
        return s==null || s.isBlank();
    }
    
    public static final boolean isNotBlank(String s) {
        return !isBlank(s);
    }
}
