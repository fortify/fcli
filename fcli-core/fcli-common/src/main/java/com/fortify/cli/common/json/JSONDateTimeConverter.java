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
package com.fortify.cli.common.json;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;

public final class JSONDateTimeConverter implements Converter<String, Date> {
    private final DateTimeFormatter fmtDateTime;
    private final ZoneId defaultZoneId;

    public JSONDateTimeConverter() {
        this(null, null);
    }

    public JSONDateTimeConverter(DateTimeFormatter fmtDateTime) {
        this(fmtDateTime, null);
    }

    public JSONDateTimeConverter(ZoneId defaultZoneId) {
        this(null, defaultZoneId);
    }

    public JSONDateTimeConverter(DateTimeFormatter fmtDateTime, ZoneId defaultZoneId) {
        this.fmtDateTime = fmtDateTime != null ? fmtDateTime : createDefaultDateTimeFormatter();
        this.defaultZoneId = defaultZoneId != null ? defaultZoneId : ZoneId.systemDefault();
    }

    public static final DateTimeFormatter createDefaultDateTimeFormatter() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd[['T'][' ']HH:mm:ss[.SSS][.SS][.S]][ZZZZ][Z][XXX][XX][X]");
    }

    @Override
    public Date convert(String source) {
        return parseDate(source);
    }

    public Date parseDate(String source) {
        return Date.from(parseZonedDateTime(source).toInstant());
    }

    public ZonedDateTime parseZonedDateTime(String source) {
        TemporalAccessor temporalAccessor = parseTemporalAccessor(source);
        if (temporalAccessor instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporalAccessor);
        }
        if (temporalAccessor instanceof LocalDateTime) {
            return ((LocalDateTime) temporalAccessor).atZone(defaultZoneId);
        }
        return ((LocalDate) temporalAccessor).atStartOfDay(defaultZoneId);
    }

    public TemporalAccessor parseTemporalAccessor(String source) {
        return fmtDateTime.parseBest(source, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
    }
}
