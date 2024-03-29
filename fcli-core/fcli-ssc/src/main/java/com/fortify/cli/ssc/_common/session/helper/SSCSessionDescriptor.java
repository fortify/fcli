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
package com.fortify.cli.ssc._common.session.helper;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.IUserCredentialsConfig;
import com.fortify.cli.common.session.helper.AbstractSessionDescriptorWithSingleUrlConfig;
import com.fortify.cli.common.session.helper.SessionSummary;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.access_control.helper.SSCTokenCreateRequest;
import com.fortify.cli.ssc.access_control.helper.SSCTokenCreateResponse;
import com.fortify.cli.ssc.access_control.helper.SSCTokenCreateResponse.SSCTokenData;
import com.fortify.cli.ssc.access_control.helper.SSCTokenHelper;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data @EqualsAndHashCode(callSuper = true) @JsonIgnoreProperties(ignoreUnknown = true)
@Reflectable @NoArgsConstructor
public class SSCSessionDescriptor extends AbstractSessionDescriptorWithSingleUrlConfig {
    private char[] predefinedToken;
    private SSCTokenCreateResponse cachedTokenResponse;
    
    public SSCSessionDescriptor(IUrlConfig urlConfig, ISSCCredentialsConfig credentialsConfig) {
        super(urlConfig);
        this.predefinedToken = credentialsConfig.getPredefinedToken();
        this.cachedTokenResponse = generateToken(urlConfig, credentialsConfig);
    }
    
    @JsonIgnore
    public void logout(IUserCredentialsConfig userCredentialsConfig) {
        if ( cachedTokenResponse!=null && userCredentialsConfig!=null ) {
            SSCTokenHelper.deleteTokensById(getUrlConfig(), userCredentialsConfig, getTokenId());
        }
    }

    @JsonIgnore 
    public final char[] getActiveToken() {
        if ( hasActiveCachedTokenResponse() ) {
            return getCachedTokenResponseData().getToken();
        } else {
            return predefinedToken;
        }
    }
    
    @JsonIgnore
    public final boolean hasActiveCachedTokenResponse() {
        return getCachedTokenResponseData()!=null && getCachedTokenResponseData().getTerminalDate().after(new Date()); 
    }
    
    @JsonIgnore
    public Date getExpiryDate() {
        Date sessionExpiryDate = SessionSummary.EXPIRES_UNKNOWN;
        if ( getCachedTokenTerminalDate()!=null ) {
            sessionExpiryDate = getCachedTokenTerminalDate();
        }
        return sessionExpiryDate;
    }
    
    @JsonIgnore
    protected SSCTokenCreateResponse generateToken(IUrlConfig urlConfig, ISSCCredentialsConfig credentialsConfig) {
        if ( credentialsConfig.getPredefinedToken()==null ) {
            ISSCUserCredentialsConfig uc = credentialsConfig.getUserCredentialsConfig();
            if ( uc!=null && StringUtils.isNotBlank(uc.getUser()) && uc.getPassword()!=null ) { 
                SSCTokenCreateRequest tokenCreateRequest = SSCTokenCreateRequest.builder()
                    .description("Auto-generated by fcli session login command")
                    .terminalDate(uc.getExpiresAt())
                    .type("UnifiedLoginToken")
                    .build();
                return SSCTokenHelper.createToken(urlConfig, uc, tokenCreateRequest, SSCTokenCreateResponse.class);
            }
        }
        return null;
    }
    
    @JsonIgnore 
    private final String getTokenId() {
        if ( hasActiveCachedTokenResponse() ) {
            return getCachedTokenResponseData().getId();
        } else {
            return null;
        }
    }
    
    @JsonIgnore
    private Date getCachedTokenTerminalDate() {
        return getCachedTokenResponseData()==null ? null : getCachedTokenResponseData().getTerminalDate();
    }
    
    @JsonIgnore
    private SSCTokenData getCachedTokenResponseData() {
        return cachedTokenResponse==null || cachedTokenResponse.getData()==null 
                ? null
                : cachedTokenResponse.getData();
    }
    
    @JsonIgnore @Override
    public String getType() {
        return SSCSessionHelper.instance().getType();
    }
}
