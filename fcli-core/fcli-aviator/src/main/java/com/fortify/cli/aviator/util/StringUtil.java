package com.fortify.cli.aviator.util;

import java.util.UUID;

public class StringUtil {

    private StringUtil() {
    }

    public static boolean isEmpty(String test) {
        return test == null || test.length() == 0;
    }

    public static boolean isValidUUID(String str) {
        if (str == null) {
            return false;
        }

        if (str.length() != 36) {
            return false;
        }

        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
