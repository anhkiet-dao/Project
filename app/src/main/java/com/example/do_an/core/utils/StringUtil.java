package com.example.do_an.core.util;

public final class StringUtil {
    private StringUtil() {}

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String orDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }
}
