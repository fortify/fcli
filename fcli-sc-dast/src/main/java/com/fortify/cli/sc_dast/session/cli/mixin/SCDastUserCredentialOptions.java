/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
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
package com.fortify.cli.sc_dast.session.cli.mixin;

import java.time.OffsetDateTime;

import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.sc_dast.util.SCDastConstants;
import com.fortify.cli.ssc.session.manager.ISSCUserCredentialsConfig;

import lombok.Getter;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;

public class SCDastUserCredentialOptions implements ISSCUserCredentialsConfig {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MINUTES, Period.DAYS);
    
    @Option(names = {"--ssc-user", "-u"}, required = true)
    @Getter private String user;
    
    @Option(names = {"--ssc-password", "-p"}, interactive = true, echo = false, arity = "0..1", required = true)
    @Getter private char[] password;
    
    @Option(names = {"--expire-in"}, descriptionKey = "fcli.ssc.session.expire-in", required = false, defaultValue = SCDastConstants.DEFAULT_TOKEN_EXPIRY, showDefaultValue = Visibility.ALWAYS)
    @Getter private String expireIn;
    
    @Override
    public OffsetDateTime getExpiresAt() {
        return PERIOD_HELPER.getCurrentOffsetDateTimePlusPeriod(expireIn);
    }
}
