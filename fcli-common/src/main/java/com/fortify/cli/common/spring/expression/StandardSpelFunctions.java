package com.fortify.cli.common.spring.expression;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.common.variable.FcliVariableHelper;

import io.micronaut.core.annotation.ReflectiveAccess;

@ReflectiveAccess
public class StandardSpELFunctions {
    private static final DateTimePeriodHelper PeriodHelper = DateTimePeriodHelper.all();

    public static final OffsetDateTime date(String s) {
        return OffsetDateTime.parse(s);
    }
    
    public static final OffsetDateTime now(String... s) {
        if ( s!=null && s.length>1 ) {
            throw new IllegalArgumentException("#now(period) only takes a single argument");
        } else if ( s==null || s.length==0 || StringUtils.isBlank(s[0]) ) {
            return OffsetDateTime.now();
        } else if ( s[0].startsWith("+") && s[0].length()>1 ) {
            return PeriodHelper.getCurrentOffsetDateTimePlusPeriod(s[0].substring(1));
        } else if ( s[0].startsWith("-") && s[0].length()>1 ) {
            return PeriodHelper.getCurrentOffsetDateTimeMinusPeriod(s[0].substring(1));
        } else {
            throw new IllegalArgumentException("Period passed to #now function is not valid: "+s[0]);
        }
    }
    
    public static final JsonNode var(String name) {
        return FcliVariableHelper.getVariableContents(name, true);
    }
    
    public static final String env(String name) {
        return System.getenv(name);
    }
}
