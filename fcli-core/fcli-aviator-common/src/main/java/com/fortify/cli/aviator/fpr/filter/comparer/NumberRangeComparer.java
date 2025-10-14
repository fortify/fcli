package com.fortify.cli.aviator.fpr.filter.comparer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public class NumberRangeComparer implements SearchComparer {
    private static final Logger logger = LoggerFactory.getLogger(NumberRangeComparer.class);

    // Your existing regex pattern here
    private static final Pattern RANGE_PATTERN = Pattern.compile("^([\\[(])\\s*([\\d.]+)\\s*[,-]\\s*([\\d.]+)\\s*([]])?$");
    private final BigDecimal lowerBound;
    private final BigDecimal upperBound;
    private final boolean lowerInclusive;
    private final boolean upperInclusive;
    private final String originalTerm;

    public NumberRangeComparer(String rangeTerm) {
        this.originalTerm = rangeTerm;
        logger.trace("Creating NumberRangeComparer for term: '{}'", rangeTerm);

        String trimmed = rangeTerm.trim();
        if (trimmed.length() < 3) { // Must be at least 3 chars like (1,2)
            throw new IllegalArgumentException("Invalid range format: " + rangeTerm);
        }

        // Determine inclusiveness from the start and end characters
        this.lowerInclusive = trimmed.startsWith("[");
        this.upperInclusive = trimmed.endsWith("]");

        // Strip the outer brackets/parentheses to get the numbers inside
        String inner = trimmed.substring(1, trimmed.length() - 1);

        // Find the separator (comma is preferred, but dash is a fallback for simple cases)
        int sep = inner.indexOf(',');
        if (sep == -1) {
            sep = inner.indexOf('-');
        }

        if (sep == -1) {
            throw new IllegalArgumentException("Invalid range format (missing separator ',' or '-'): " + rangeTerm);
        }

        try {
            this.lowerBound = new BigDecimal(inner.substring(0, sep).trim());
            this.upperBound = new BigDecimal(inner.substring(sep + 1).trim());
        } catch (NumberFormatException e) {
            logger.error("Error parsing number in range term '{}'", rangeTerm, e);
            throw new IllegalArgumentException("Invalid number in range format: " + rangeTerm);
        }
    }

    @Override
    public boolean matches(Object attributeValue) {
        if (attributeValue == null) {
            logger.trace("DEBUG COMPARER: Attr value 'null' vs search term '{}'. Result: false", originalTerm);
            return false;
        }
        BigDecimal value;
        if (attributeValue instanceof Number) {
            value = new BigDecimal(attributeValue.toString());
        } else if (attributeValue instanceof String) {
            try {
                value = new BigDecimal((String) attributeValue);
            } catch (NumberFormatException e) {
                logger.trace("Invalid number string '{}'", attributeValue);
                logger.trace("DEBUG COMPARER: Attr value '" + attributeValue + "' is not a number. Search term '" + originalTerm + "'. Result: false");
                return false;
            }
        } else {
            logger.trace("Non-number attr '{}'", attributeValue);
            logger.trace("DEBUG COMPARER: Attr value '{}' is not a number. Search term '{}'. Result: false", attributeValue, originalTerm);
            return false;
        }

        int lowerCmp = value.compareTo(lowerBound);
        int upperCmp = value.compareTo(upperBound);

        boolean meetsLower = lowerInclusive ? (lowerCmp >= 0) : (lowerCmp > 0);
        boolean meetsUpper = upperInclusive ? (upperCmp <= 0) : (upperCmp < 0);

        boolean result = meetsLower && meetsUpper;

        logger.trace("DEBUG COMPARER: Attr value '" + attributeValue + "' vs search term '" + originalTerm + "'. Result: " + result);
        return result;
    }

    public String getSearchTerm() {
        return originalTerm;
    }
}