package com.fortify.cli.aviator.fpr.utils;

import com.fortify.cli.aviator.fpr.jaxb.EngineData;
import com.fortify.cli.aviator.fpr.jaxb.MetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Utility class for XML-related operations, such as safe parsing of numbers
 * with defaults, and extracting meta information from rule elements.
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

    /**
     * Extracts meta information (accuracy, impact, probability, audience) from a rule element.
     *
     * @param ruleElement JAXB Rule element
     * @return Array of [accuracy, impact, probability, audience] as strings, defaults to "0"/""
     */
    public static String[] getMetaInfoFromRule(EngineData.RuleInfo.Rule ruleElement) {
        String[] metaInfo = new String[]{"0", "0", "0", ""};
        if (ruleElement == null) {
            return metaInfo;
        }

        MetaInfo metaInfoElem = ruleElement.getMetaInfo();
        if (metaInfoElem != null) {
            for (MetaInfo.Group group : metaInfoElem.getGroup()) {
                String groupName = group.getName().toLowerCase();
                String content = group.getValue().trim();

                switch (groupName) {
                    case "accuracy":
                        metaInfo[0] = content;
                        break;
                    case "impact":
                        metaInfo[1] = content;
                        break;
                    case "probability":
                        metaInfo[2] = content;
                        break;
                    case "audience":
                        metaInfo[3] = content;
                        break;
                }
            }
        }
        return metaInfo;
    }
}