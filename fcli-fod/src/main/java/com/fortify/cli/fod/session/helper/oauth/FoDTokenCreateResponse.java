/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod.session.helper.oauth;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;

@Data @ReflectiveAccess @JsonIgnoreProperties(ignoreUnknown = true)
public final class FoDTokenCreateResponse {
    @JsonProperty("access_token") private String accessToken;
    @JsonProperty("expires_at") private long expiresAt;

    @JsonProperty("expires_in")
    public void setExpiresIn(long expiresIn) {
        this.expiresAt = new Date().getTime()+((expiresIn-5)*1000);
    }

    @JsonIgnore public boolean isActive() {
        return new Date().getTime() < expiresAt;
    }
}