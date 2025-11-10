/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.common.util;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class DateTimePeriodHelper {
    private final Pattern periodPattern;
    
    // TODO Consider renaming this enum, as java.time also has a Period class
    @RequiredArgsConstructor @Getter
    public static enum Period {
        MILLISECONDS("ms", ChronoUnit.MILLIS),
        SECONDS("s", ChronoUnit.SECONDS),
        MINUTES("m", ChronoUnit.MINUTES),
        HOURS("h", ChronoUnit.HOURS),
        DAYS("d", ChronoUnit.DAYS), 
        WEEKS("w", ChronoUnit.WEEKS), 
        MONTHS("M", ChronoUnit.MONTHS),
        YEARS("y", ChronoUnit.YEARS)
        ;
        
        private final String type;
        private final TemporalUnit unit;
        
        public static final Period[] getRange(Period min, Period max) {
            return Stream.of(Period.values())
                .filter(p->p.ordinal()>=min.ordinal())
                .filter(p->p.ordinal()<=max.ordinal())
                .toArray(Period[]::new);
        }
        
        public static final Period getByType(String type) {
            return Stream.of(Period.values()).filter(p->p.getType().equals(type)).findFirst().get();
        }
    }
    
    public DateTimePeriodHelper(Period... periods) {
        this.periodPattern = buildPeriodPattern(periods);
    }
    
    public static final DateTimePeriodHelper byRange(Period min, Period max) {
        return new DateTimePeriodHelper(Period.getRange(min, max));
    }
    
    public static final DateTimePeriodHelper all() {
        // Includes MILLISECONDS through YEARS; callers may restrict with byRange() if needed.
        return new DateTimePeriodHelper(Period.getRange(Period.MILLISECONDS,Period.YEARS));
    }
    
    private Pattern buildPeriodPattern(Period... periods) {
        // Build alternation pattern instead of character class so multi-char tokens like 'ms' are handled correctly.
        // Sort by descending length to ensure longer tokens (e.g. 'ms') are matched before their single-letter suffixes.
        String alternation = Stream.of(periods)
            .map(Period::getType)
            .distinct()
            .sorted((a,b)->Integer.compare(b.length(), a.length()))
            .collect(Collectors.joining("|"));
        String patternString = String.format("(\\d+)(%s)", alternation);
        return Pattern.compile(patternString);
    }

    public long parsePeriodToEpochMillis(String periodString){
        if(periodString == null || periodString.isBlank()) return 0;
        periodString = trimPeriodString(periodString);
        Matcher matcher = periodPattern.matcher(periodString);
        Instant instant=Instant.EPOCH;
        int lastEnd = 0;
        while(matcher.find()){
            if ( matcher.start()!=lastEnd ) {
                throw new IllegalArgumentException("Invalid period segment: '"+periodString.substring(lastEnd, matcher.start())+"' in '"+periodString+"'");
            }
            int num = Integer.parseInt(matcher.group(1));
            String type = matcher.group(2);
            Period period = Period.getByType(type);
            instant = instant.plusMillis(toMillis(num, period));
            lastEnd = matcher.end();
        }
        if ( lastEnd!=periodString.length() ) {
            throw new IllegalArgumentException("Invalid trailing characters in period: '"+periodString.substring(lastEnd)+"' in '"+periodString+"'");
        }
        return instant.toEpochMilli();
    }

    /**
     * Trim whitespace and strip surrounding quotes from period string for backward compatibility.
     * The old implementation used {@code Matcher.find()} which would locate period patterns anywhere
     * in the string, ignoring surrounding characters. This method preserves that lenient behavior
     * while allowing stricter validation of the actual period content.
     */
    private static String trimPeriodString(String periodString) {
        periodString = periodString.trim();
        if ( (periodString.startsWith("'") && periodString.endsWith("'")) ||
             (periodString.startsWith("\"") && periodString.endsWith("\"")) ) {
            if ( periodString.length() >= 2 ) {
                periodString = periodString.substring(1, periodString.length()-1);
            }
        }
        return periodString;
    }

    private static long toMillis(int amount, Period period) {
        var unit = period.getUnit();
        // For ChronoUnits with exact duration, rely on Duration.
        // WEEKS are exact (7 days) so Duration.of works. MONTHS & YEARS are estimated; use approximations.
        if ( unit == ChronoUnit.MONTHS ) { return amount * 30L * 24L * 60L * 60L * 1000L; }
        if ( unit == ChronoUnit.YEARS ) { return amount * 365L * 24L * 60L * 60L * 1000L; }
        if ( unit == ChronoUnit.WEEKS ) { return amount * 7L * 24L * 60L * 60L * 1000L; }
        return Duration.of(amount, unit).toMillis();
    }
    
    public long parsePeriodToMillis(String periodString) {
        return parsePeriodToEpochMillis(periodString)-Instant.EPOCH.toEpochMilli();
    }
    
    public final Date getCurrentDatePlusPeriod(String period) {
        return new Date(System.currentTimeMillis() + parsePeriodToEpochMillis(period));
    }
    
    public final OffsetDateTime getCurrentOffsetDateTimePlusPeriod(String period) {
        return OffsetDateTime.now(ZoneOffset.UTC).plusNanos(parsePeriodToEpochMillis(period)*1000000);
    }
    
    public final OffsetDateTime getCurrentOffsetDateTimeMinusPeriod(String period) {
        return OffsetDateTime.now(ZoneOffset.UTC).minusNanos(parsePeriodToEpochMillis(period)*1000000);
    }
}
