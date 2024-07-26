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
package com.fortify.cli.sc_sast._common.session.helper;

import java.time.OffsetDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.IUserCredentialsConfig;
import com.fortify.cli.common.rest.unirest.config.UrlConfig;
import com.fortify.cli.common.session.helper.AbstractSessionDescriptor;
import com.fortify.cli.common.session.helper.SessionSummary;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc._common.session.helper.ISSCCredentialsConfig;
import com.fortify.cli.ssc._common.session.helper.ISSCUserCredentialsConfig;
import com.fortify.cli.ssc.access_control.helper.SSCTokenCreateRequest;
import com.fortify.cli.ssc.access_control.helper.SSCTokenGetOrCreateResponse;
import com.fortify.cli.ssc.access_control.helper.SSCTokenHelper;
import com.fortify.cli.ssc.access_control.helper.SSCTokenGetOrCreateResponse.SSCTokenData;

import kong.unirest.UnirestInstance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data @EqualsAndHashCode(callSuper = true) 
@Reflectable @NoArgsConstructor 
@JsonIgnoreProperties(ignoreUnknown = true)
public class SCSastSessionDescriptor extends AbstractSessionDescriptor {
    @JsonDeserialize(as = UrlConfig.class) private IUrlConfig sscUrlConfig;
    @JsonDeserialize(as = UrlConfig.class) private IUrlConfig scSastUrlConfig;
    @Getter private char[] scSastClientAuthToken;
    private char[] predefinedSscToken;
    private SSCTokenGetOrCreateResponse cachedSscTokenResponse;
    
    public SCSastSessionDescriptor(IUrlConfig sscUrlConfig, ISSCCredentialsConfig credentialsConfig, char[] scSastClientAuthToken) {
        this(sscUrlConfig, null, credentialsConfig, scSastClientAuthToken);
    }
    
    public SCSastSessionDescriptor(IUrlConfig sscUrlConfig, IUrlConfig scSastUrlConfig, ISSCCredentialsConfig credentialsConfig, char[] scSastClientAuthToken) {
        this.sscUrlConfig = sscUrlConfig;
        this.predefinedSscToken = credentialsConfig.getPredefinedToken();
        this.scSastClientAuthToken = scSastClientAuthToken;
        this.cachedSscTokenResponse = getOrGenerateToken(sscUrlConfig, credentialsConfig);
        char[] activeToken = getActiveSSCToken();
        this.scSastUrlConfig = activeToken==null ? null : buildScSastUrlConfig(sscUrlConfig, scSastUrlConfig, activeToken);
    }

    @JsonIgnore
    public void logout(IUserCredentialsConfig userCredentialsConfig) {
        if ( cachedSscTokenResponse!=null && userCredentialsConfig!=null ) {
            SSCTokenHelper.deleteTokensById(getSscUrlConfig(), userCredentialsConfig, getTokenId());
        }
    }
    
    @JsonIgnore
    @Override
    public String getUrlDescriptor() {
        return String.format("SSC:     %s\nSC-SAST: %s", 
                sscUrlConfig==null || sscUrlConfig.getUrl()==null ? "unknown" : sscUrlConfig.getUrl(),
                scSastUrlConfig==null || scSastUrlConfig.getUrl()==null ? "unknown" : scSastUrlConfig.getUrl());
    }

    @JsonIgnore 
    public final char[] getActiveSSCToken() {
        if ( hasActiveCachedTokenResponse() ) {
            return getCachedSscTokenResponseData().getToken();
        } else {
            return predefinedSscToken;
        }
    }
    
    @JsonIgnore
    public final boolean hasActiveCachedTokenResponse() {
        return getCachedSscTokenResponseData()!=null && getCachedSscTokenResponseData().getTerminalDate().after(new Date()); 
    }
    
    @JsonIgnore
    public Date getExpiryDate() {
        Date sessionExpiryDate = SessionSummary.EXPIRES_UNKNOWN;
        if ( getCachedTokenTerminalDate()!=null ) {
            sessionExpiryDate = getCachedTokenTerminalDate();
        }
        return sessionExpiryDate;
    }
    
    @JsonIgnore @Override
    public String getType() {
        return SCSastSessionHelper.instance().getType();
    }
    
    @JsonIgnore
    protected SSCTokenGetOrCreateResponse getOrGenerateToken(IUrlConfig urlConfig, ISSCCredentialsConfig credentialsConfig) {
        return credentialsConfig.getPredefinedToken()==null 
                ? generateToken(urlConfig, credentialsConfig) 
                : getToken(urlConfig, credentialsConfig);
    }

    @JsonIgnore
    protected SSCTokenGetOrCreateResponse getToken(IUrlConfig urlConfig, ISSCCredentialsConfig credentialsConfig) {
        return SSCTokenHelper.getTokenData(urlConfig, credentialsConfig.getPredefinedToken());
    }
    
    @JsonIgnore
    protected SSCTokenGetOrCreateResponse generateToken(IUrlConfig urlConfig, ISSCCredentialsConfig credentialsConfig) {
        ISSCUserCredentialsConfig uc = credentialsConfig.getUserCredentialsConfig();
        if ( uc!=null && StringUtils.isNotBlank(uc.getUser()) && uc.getPassword()!=null ) {
            SSCTokenCreateRequest tokenCreateRequest = SSCTokenCreateRequest.builder()
                    .description("Auto-generated by fcli session login command")
                    .terminalDate(getExpiresAt(uc.getExpiresAt())) 
                    .type("UnifiedLoginToken")
                    .build();
            return SSCTokenHelper.createToken(urlConfig, uc, tokenCreateRequest, SSCTokenGetOrCreateResponse.class);
        }
        return null;
    }
    
    private OffsetDateTime getExpiresAt(OffsetDateTime expiresAt) {
        return expiresAt!=null 
            ? expiresAt 
            : DateTimePeriodHelper.byRange(Period.MINUTES, Period.DAYS).getCurrentOffsetDateTimePlusPeriod("1d");
    }

    @JsonIgnore 
    private final String getTokenId() {
        if ( hasActiveCachedTokenResponse() ) {
            return getCachedSscTokenResponseData().getId();
        } else {
            return null;
        }
    }
    
    @JsonIgnore
    private Date getCachedTokenTerminalDate() {
        return getCachedSscTokenResponseData()==null ? null : getCachedSscTokenResponseData().getTerminalDate();
    }
    
    @JsonIgnore
    private SSCTokenData getCachedSscTokenResponseData() {
        return cachedSscTokenResponse==null || cachedSscTokenResponse.getData()==null 
                ? null
                : cachedSscTokenResponse.getData();
    }
    
    private static final IUrlConfig buildScSastUrlConfig(IUrlConfig sscUrlConfig, IUrlConfig scSastUrlConfig, char[] activeToken) {
        String scSastUrl = scSastUrlConfig!=null && StringUtils.isNotBlank(scSastUrlConfig.getUrl())
                ? scSastUrlConfig.getUrl()
                : getScSastUrl(sscUrlConfig, activeToken);
        UrlConfig.UrlConfigBuilder builder = UrlConfig.builderFrom(sscUrlConfig, scSastUrlConfig);
        builder.url(scSastUrl);
        return builder.build();
    }

    private static String getScSastUrl(IUrlConfig sscUrlConfig, char[] activeToken) {
        return SSCTokenHelper.run(sscUrlConfig, activeToken, SCSastSessionDescriptor::getScSastUrl);
    }

    private static final String getScSastUrl(UnirestInstance unirest) {
        ArrayNode properties = getScSastConfigurationProperties(unirest);
        checkScSastIsEnabled(properties);
        String scSastUrl = getScSastUrlFromProperties(properties);
        return normalizeScSastUrl(scSastUrl);
    }
    
    private static final ArrayNode getScSastConfigurationProperties(UnirestInstance sscUnirest) {
        ObjectNode configData = sscUnirest.get("/api/v1/configuration?group=cloudscan")
                .asObject(ObjectNode.class)
                .getBody(); 
        
        return JsonHelper.evaluateSpelExpression(configData, "data.properties", ArrayNode.class);
    }
    
    private static final void checkScSastIsEnabled(ArrayNode properties) {
        boolean scSastEnabled = JsonHelper.evaluateSpelExpression(properties, "^[name=='cloud.ctrl.poll.enabled']?.value=='true'", Boolean.class);
        if (!scSastEnabled) {
            throw new IllegalStateException("ScanCentral SAST must be enabled in SSC");
        }
    }
    
    private static final String getScSastUrlFromProperties(ArrayNode properties) {
        String scSastUrl = JsonHelper.evaluateSpelExpression(properties, "^[name=='cloud.ctrl.url']?.value", String.class);
        if ( scSastUrl.isEmpty() ) {
            throw new IllegalStateException("SSC returns an empty ScanCentral SAST URL");
        }
        return scSastUrl;
    }
    
    private static final String normalizeScSastUrl(String scSastUrl) {
        // We remove any trailing slashes from the URL as most users will specify relative URL's starting with /api/v2/...
        return scSastUrl.replaceAll("/+$", "");
    }
}
