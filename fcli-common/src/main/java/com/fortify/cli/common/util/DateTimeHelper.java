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
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeHelper {
	private static final Pattern periodPattern = Pattern.compile("([0-9]+)([hdwmy])");

	public static long parsePeriod(String period){
	    if(period == null) return 0;
	    period = period.toLowerCase(Locale.ENGLISH);
	    Matcher matcher = periodPattern.matcher(period);
	    Instant instant=Instant.EPOCH;
	    while(matcher.find()){
	        int num = Integer.parseInt(matcher.group(1));
	        String typ = matcher.group(2);
	        switch (typ) {
	        	case "m":
	        		instant=instant.plus(Duration.ofMinutes(num));
	        		break;
	            case "h":
	                instant=instant.plus(Duration.ofHours(num));
	                break;
	            case "d":
	                instant=instant.plus(Duration.ofDays(num));
	                break;
	        }
	    }
	    return instant.toEpochMilli();
	}
	
	public static final Date getCurrentDatePlusPeriod(String period) {
		return new Date(System.currentTimeMillis() + parsePeriod(period));
	}
	
	public static final OffsetDateTime getCurrentOffsetDateTimePlusPeriod(String period) {
		return OffsetDateTime.now(ZoneOffset.UTC).plusNanos(parsePeriod(period)*1000000);
	}
}
