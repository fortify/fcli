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
package com.fortify.cli.sc_sast.session.cli.mixin;

import java.time.OffsetDateTime;

import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.ssc.session.helper.ISSCUserCredentialsConfig;

import lombok.Getter;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;

public class SCSastUserCredentialAndExpiryOptions extends SCSastUserCredentialOptions implements ISSCUserCredentialsConfig {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MINUTES, Period.DAYS);
    
    @Option(names = {"--expire-in"}, descriptionKey = "fcli.ssc.session.expire-in", required = false, defaultValue = "1d", showDefaultValue = Visibility.ALWAYS)
    @Getter private String expireIn;
    
    @Override
    public OffsetDateTime getExpiresAt() {
        return PERIOD_HELPER.getCurrentOffsetDateTimePlusPeriod(expireIn);
    }
}
