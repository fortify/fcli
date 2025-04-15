/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.spring.expression;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.crypto.helper.EncryptionHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DebugHelper;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.common.variable.FcliVariableHelper;

import lombok.NoArgsConstructor;

@Reflectable @NoArgsConstructor
public class SpelFunctionsStandard {
    private static final DateTimePeriodHelper PeriodHelper = DateTimePeriodHelper.all();
    
    public static final boolean isDebugEnabled() {
        return DebugHelper.isDebugEnabled();
    }

    public static final String uuid() {
        return UUID.randomUUID().toString();
    }
    
    public static final String fmt(String fmt, Object... input) {
        return String.format(fmt, input);
    }
    
    public static final OffsetDateTime date(String s) {
        if(s == null) { return null; }
        
        OffsetDateTime dt = null;
        try {
            dt= OffsetDateTime.parse(s);
        } catch(DateTimeParseException e) {
            LocalDate d = LocalDate.parse(s);
            dt = OffsetDateTime.of(d.atStartOfDay(), ZoneOffset.UTC);
        }

        return dt;
    }
    
    public static final OffsetDateTime now(String... s) {
        if ( s!=null && s.length>1 ) {
            throw new FcliSimpleException("#now(period) only takes a single argument");
        } else if ( s==null || s.length==0 || StringUtils.isBlank(s[0]) ) {
            return OffsetDateTime.now();
        } else if ( s[0].startsWith("+") && s[0].length()>1 ) {
            return PeriodHelper.getCurrentOffsetDateTimePlusPeriod(s[0].substring(1));
        } else if ( s[0].startsWith("-") && s[0].length()>1 ) {
            return PeriodHelper.getCurrentOffsetDateTimeMinusPeriod(s[0].substring(1));
        } else {
            throw new FcliSimpleException("Period passed to #now function is not valid: "+s[0]);
        }
    }
    
    public static final JsonNode var(String name) {
        return FcliVariableHelper.getVariableContents(name, true);
    }
    
    public static final String env(String name) {
        if ( StringUtils.isBlank(name)) { throw new FcliSimpleException("Environment variable name passed to #env may not be null"); }
        var result = EnvHelper.env(name);
        // Return null in case of blank string
        return StringUtils.isBlank(result) ? null : result;
    }
    
    public static final String encrypt(String s) {
        return EncryptionHelper.encrypt(s);
    }
    
    public static final String decrypt(String s) {
        return EncryptionHelper.decrypt(s);
    }
    
}
