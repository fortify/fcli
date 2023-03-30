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
package com.fortify.cli.sc_sast.session.helper;

import java.time.OffsetDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.IUserCredentialsConfig;
import com.fortify.cli.common.rest.unirest.config.UrlConfig;
import com.fortify.cli.common.session.helper.AbstractSessionDescriptor;
import com.fortify.cli.common.session.helper.SessionSummary;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.entity.token.helper.SSCTokenCreateRequest;
import com.fortify.cli.ssc.entity.token.helper.SSCTokenCreateResponse;
import com.fortify.cli.ssc.entity.token.helper.SSCTokenCreateResponse.SSCTokenData;
import com.fortify.cli.ssc.entity.token.helper.SSCTokenHelper;
import com.fortify.cli.ssc.session.helper.ISSCCredentialsConfig;
import com.fortify.cli.ssc.session.helper.ISSCUserCredentialsConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Data @EqualsAndHashCode(callSuper = true) @ReflectiveAccess @JsonIgnoreProperties(ignoreUnknown = true)
public class SCSastSessionDescriptor extends AbstractSessionDescriptor {
    @JsonDeserialize(as = UrlConfig.class) private IUrlConfig sscUrlConfig;
    @JsonDeserialize(as = UrlConfig.class) private IUrlConfig scSastUrlConfig;
    @Getter private char[] scSastClientAuthToken;
    private char[] predefinedSscToken;
    private SSCTokenCreateResponse cachedSscTokenResponse;
    
    protected SCSastSessionDescriptor() {}
    
    public SCSastSessionDescriptor(IUrlConfig sscUrlConfig, ISSCCredentialsConfig credentialsConfig, char[] scSastClientAuthToken) {
        this(sscUrlConfig, null, credentialsConfig, scSastClientAuthToken);
    }
    
    public SCSastSessionDescriptor(IUrlConfig sscUrlConfig, IUrlConfig scSastUrlConfig, ISSCCredentialsConfig credentialsConfig, char[] scSastClientAuthToken) {
        this.sscUrlConfig = sscUrlConfig;
        this.predefinedSscToken = credentialsConfig.getPredefinedToken();
        this.scSastClientAuthToken = scSastClientAuthToken;
        this.cachedSscTokenResponse = generateToken(sscUrlConfig, credentialsConfig);
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
    protected SSCTokenCreateResponse generateToken(IUrlConfig urlConfig, ISSCCredentialsConfig credentialsConfig) {
        if ( credentialsConfig.getPredefinedToken()==null ) {
            ISSCUserCredentialsConfig uc = credentialsConfig.getUserCredentialsConfig();
            if ( uc!=null && StringUtils.isNotBlank(uc.getUser()) && uc.getPassword()!=null ) {
                SSCTokenCreateRequest tokenCreateRequest = SSCTokenCreateRequest.builder()
                        .description("Auto-generated by fcli session login command")
                        .terminalDate(getExpiresAt(uc.getExpiresAt())) 
                        .type("CIToken")
                        .build();
                return SSCTokenHelper.createToken(urlConfig, uc, tokenCreateRequest, SSCTokenCreateResponse.class);
            }
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
