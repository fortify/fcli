package com.fortify.cli.aviator.fpr.filter.comparer;


import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles numeric range comparisons like "[3,5]" and "(2.0, 4.5)". The range is stored internally.
 */
public class NumberRangeComparer implements SearchComparer {
    private static final Pattern RANGE_PATTERN = Pattern.compile("^([\\[(])\\s*([\\d.]+)\\s*,\\s*([\\d.]+)\\s*([])])$");
    private final BigDecimal lowerBound;
    private final BigDecimal upperBound;
    private final boolean lowerInclusive;
    private final boolean upperInclusive;
    private final String originalTerm;

    public NumberRangeComparer(String rangeTerm) {
        this.originalTerm = rangeTerm;
        Matcher matcher = RANGE_PATTERN.matcher(rangeTerm.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid range format: " + rangeTerm);
        }
        this.lowerInclusive = matcher.group(1).equals("[");
        this.lowerBound = new BigDecimal(matcher.group(2));
        this.upperBound = new BigDecimal(matcher.group(3));
        this.upperInclusive = matcher.group(4).equals("]");
    }

    @Override
    public boolean matches(Object attributeValue) {
        if (!(attributeValue instanceof Number)) {
            return false;
        }
        BigDecimal value = new BigDecimal(attributeValue.toString());

        int lowerCmp = value.compareTo(lowerBound);
        int upperCmp = value.compareTo(upperBound);

        boolean meetsLower = lowerInclusive ? (lowerCmp >= 0) : (lowerCmp > 0);
        boolean meetsUpper = upperInclusive ? (upperCmp <= 0) : (upperCmp < 0);

        boolean result = meetsLower && meetsUpper;

        return result;
    }
}