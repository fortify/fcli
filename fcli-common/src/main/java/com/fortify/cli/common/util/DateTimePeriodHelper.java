/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
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
    
    // TODO Rename this enum, as java.time also has a Period class
    @RequiredArgsConstructor @Getter
    public static enum Period {
        SECONDS("s", ChronoUnit.SECONDS),
        MINUTES("m", ChronoUnit.MINUTES),
        HOURS("h", ChronoUnit.HOURS),
        DAYS("d", ChronoUnit.DAYS),
        // TODO Currently these units result in an exception (see TODO below)
        //WEEKS("w", ChronoUnit.WEEKS), 
        //MONTHS("M", ChronoUnit.MONTHS),
        //YEARS("y", ChronoUnit.YEARS)
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
        // TODO Once we re-add support for months and years, update this range
        return new DateTimePeriodHelper(Period.getRange(Period.SECONDS,Period.DAYS));
    }
    
    private Pattern buildPeriodPattern(Period... periods) {
        String patternString = String.format("([0-9]+)([%s])", Stream.of(periods).map(Period::getType).collect(Collectors.joining("")));
        return Pattern.compile(patternString);
    }

    public long parsePeriodToEpochMillis(String periodString){
        if(periodString == null) return 0;
        Matcher matcher = periodPattern.matcher(periodString);
        Instant instant=Instant.EPOCH;
        while(matcher.find()){
            int num = Integer.parseInt(matcher.group(1));
            String type = matcher.group(2);
            Period period = Period.getByType(type);
            // TODO This call doesn't work for estimated durations like year and month
            instant=instant.plus(Duration.of(num, period.getUnit()));
        }
        return instant.toEpochMilli();
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
