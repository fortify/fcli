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
package com.fortify.cli.debricked._common.session.helper;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.session.helper.AbstractSessionDescriptorWithSingleUrlConfig;
import com.fortify.cli.common.session.helper.SessionSummary;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data @EqualsAndHashCode(callSuper = true) @JsonIgnoreProperties(ignoreUnknown = true)
@Reflectable @NoArgsConstructor
public class DebrickedSessionDescriptor extends AbstractSessionDescriptorWithSingleUrlConfig {
    private String jwtToken;
    private String refreshToken;
    private Date tokenExpiryDate;
    
    public DebrickedSessionDescriptor(IUrlConfig urlConfig, String jwtToken, String refreshToken) {
        super(urlConfig);
        this.jwtToken = jwtToken;
        this.refreshToken = refreshToken;
        this.tokenExpiryDate = getExpiry(jwtToken);
    }
    
    @JsonIgnore
    public final boolean hasActiveJwtToken() {
        return jwtToken != null && (tokenExpiryDate == null || tokenExpiryDate.after(new Date()));
    }
    
    @JsonIgnore 
    public String getActiveJwtToken() {
        return hasActiveJwtToken() ? jwtToken : null; 
    }
    
    @JsonIgnore @Override
    public Date getExpiryDate() {
        return tokenExpiryDate != null ? tokenExpiryDate : SessionSummary.EXPIRES_UNKNOWN;
    }
    
    @JsonIgnore @Override
    public String getType() {
        return "Debricked";
    }
    
    @JsonIgnore
    private static final Date getExpiry(String jwtToken) {
        var payload = JsonHelper.parseJwtToken(jwtToken);
        var exp = payload.get("exp");
        if ( exp!=null && exp.isNumber() ) {
            long expSeconds = exp.asLong();
            if ( expSeconds>0 ) {
                return new Date(expSeconds*1000L);
            }
        }
        return null;
    }
}