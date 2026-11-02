/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.common.fpr.utils;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for safe parsing of numbers from XML text values.
 */
public class XmlUtils {
    private static final Logger logger = LoggerFactory.getLogger(XmlUtils.class);

    /**
     * Safely parses a string to an integer, returning a default value on failure.
     *
     * @param value        String to parse
     * @param defaultValue Default if parsing fails
     * @return Parsed integer or default
     */
    public static int safeParseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse int: '{}', using default: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Safely parses a string to a float, returning a default value on failure.
     *
     * @param value        String to parse
     * @param defaultValue Default if parsing fails
     * @return Parsed float or default
     */
    public static float safeParseFloat(String value, float defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse float: '{}', using default: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Safely parses a string to a double, returning a default value on failure.
     *
     * @param value        String to parse
     * @param defaultValue Default if parsing fails
     * @return Parsed double or default
     */
    public static double safeParseDouble(String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse double: '{}', using default: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Safely parses a string to a BigDecimal, returning a default value on failure.
     *
     * @param value        String to parse
     * @param defaultValue Default if parsing fails
     * @return Parsed BigDecimal or default
     */
    public static BigDecimal safeParseBigDecimal(String value, BigDecimal defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse BigDecimal: '{}', using default: {}", value, defaultValue);
            return defaultValue;
        }
    }
}