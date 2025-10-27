/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.aviator._common.session.user.helper;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.session.helper.AbstractSessionDescriptor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data @EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
@Reflectable @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AviatorUserSessionDescriptor extends AbstractSessionDescriptor {
    @JsonProperty
    @MaskValue(sensitivity = LogSensitivityLevel.low, description = "AVIATOR HOST NAME", pattern = MaskValue.URL_HOSTNAME_PATTERN)
    private String aviatorUrl;
    @JsonProperty
    @MaskValue(sensitivity = LogSensitivityLevel.high, description = "AVIATOR TOKEN")
    private String aviatorToken;
    @JsonProperty
    private Date expiryDate;

    @Override @JsonIgnore
    public String getUrlDescriptor() {
        return aviatorUrl;
    }

    @Override @JsonIgnore
    public Date getExpiryDate() {
        return expiryDate;
    }

    @Override @JsonIgnore
    public String getType() {
        return AviatorUserSessionHelper.instance().getType();
    }
}