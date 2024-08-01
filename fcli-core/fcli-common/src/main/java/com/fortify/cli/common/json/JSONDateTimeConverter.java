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
package com.fortify.cli.common.json;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;

public final class JSONDateTimeConverter implements Converter<String,Date> {
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
		this.fmtDateTime = fmtDateTime!=null ? fmtDateTime : createDefaultDateTimeFormatter();
		this.defaultZoneId = defaultZoneId!=null ? defaultZoneId : ZoneId.systemDefault();
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