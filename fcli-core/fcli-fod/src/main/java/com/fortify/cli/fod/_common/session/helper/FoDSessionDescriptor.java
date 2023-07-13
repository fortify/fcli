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
package com.fortify.cli.fod._common.session.helper;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.session.helper.AbstractSessionDescriptorWithSingleUrlConfig;
import com.fortify.cli.common.session.helper.SessionSummary;
import com.fortify.cli.fod._common.session.helper.oauth.FoDTokenCreateResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data @EqualsAndHashCode(callSuper = true) @JsonIgnoreProperties(ignoreUnknown = true)
@Reflectable @NoArgsConstructor
public class FoDSessionDescriptor extends AbstractSessionDescriptorWithSingleUrlConfig {
    private FoDTokenCreateResponse cachedTokenResponse;
    
    public FoDSessionDescriptor(IUrlConfig urlConfig, FoDTokenCreateResponse tokenResponse) {
        super(urlConfig);
        this.cachedTokenResponse = tokenResponse;
    }
    
    @JsonIgnore
    public final boolean hasActiveCachedTokenResponse() {
        return getCachedTokenResponse()!=null && cachedTokenResponse.isActive(); 
    }
    
    @JsonIgnore 
    public String getActiveBearerToken() {
        return hasActiveCachedTokenResponse() ? cachedTokenResponse.getAccessToken() : null; 
    }
    
    @JsonIgnore @Override
    public Date getExpiryDate() {
        Date sessionExpiryDate = SessionSummary.EXPIRES_UNKNOWN;
        if ( getCachedTokenResponse()!=null ) {
            sessionExpiryDate = new Date(getCachedTokenResponse().getExpiresAt());
        }
        return sessionExpiryDate;
    }
    
    @JsonIgnore @Override
    public String getType() {
        return FoDSessionHelper.instance().getType();
    }
}
