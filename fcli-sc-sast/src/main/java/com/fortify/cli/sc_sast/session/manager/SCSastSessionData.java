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
package com.fortify.cli.sc_sast.session.manager;

import java.time.OffsetDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.rest.runner.config.IUserCredentialsConfig;
import com.fortify.cli.common.rest.runner.config.UrlConfig;
import com.fortify.cli.common.session.manager.api.SessionSummary;
import com.fortify.cli.common.session.manager.spi.AbstractSessionData;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc.session.manager.ISSCCredentialsConfig;
import com.fortify.cli.ssc.session.manager.ISSCUserCredentialsConfig;
import com.fortify.cli.ssc.token.helper.SSCTokenCreateRequest;
import com.fortify.cli.ssc.token.helper.SSCTokenCreateResponse;
import com.fortify.cli.ssc.token.helper.SSCTokenCreateResponse.SSCTokenData;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Data @EqualsAndHashCode(callSuper = true) @ReflectiveAccess @JsonIgnoreProperties(ignoreUnknown = true)
public class SCSastSessionData extends AbstractSessionData implements ISCSastSessionData {
    @JsonDeserialize(as = UrlConfig.class) private IUrlConfig sscUrlConfig;
    @JsonDeserialize(as = UrlConfig.class) private IUrlConfig scSastUrlConfig;
    @Getter private char[] scSastClientAuthToken;
    private char[] predefinedSscToken;
    private SSCTokenCreateResponse cachedSscTokenResponse;
    
    protected SCSastSessionData() {}
    
    public SCSastSessionData(IUrlConfig sscUrlConfig, ISSCCredentialsConfig credentialsConfig, char[] scSastClientAuthToken, SSCTokenHelper sscTokenHelper) {
        this(sscUrlConfig, null, credentialsConfig, scSastClientAuthToken, sscTokenHelper);
    }
    
    public SCSastSessionData(IUrlConfig sscUrlConfig, IUrlConfig scSastUrlConfig, ISSCCredentialsConfig credentialsConfig, char[] scSastClientAuthToken, SSCTokenHelper sscTokenHelper) {
        this.sscUrlConfig = sscUrlConfig;
        this.predefinedSscToken = credentialsConfig.getPredefinedToken();
        this.scSastClientAuthToken = scSastClientAuthToken;
        this.cachedSscTokenResponse = generateToken(sscUrlConfig, credentialsConfig, sscTokenHelper);
        char[] activeToken = getActiveSSCToken();
        this.scSastUrlConfig = activeToken==null ? null : buildScSastUrlConfig(sscUrlConfig, scSastUrlConfig, activeToken, sscTokenHelper);
    }

    @JsonIgnore
    public void logout(SSCTokenHelper sscTokenHelper, IUserCredentialsConfig userCredentialsConfig) {
        if ( cachedSscTokenResponse!=null && userCredentialsConfig!=null ) {
            sscTokenHelper.deleteTokensById(getSscUrlConfig(), userCredentialsConfig, getTokenId());
        }
    }
    
    @Override
    public String getUrlDescriptor() {
        return String.format("SSC:     %s\nSC-SAST: %s", 
                sscUrlConfig==null || sscUrlConfig.getUrl()==null ? "unknown" : sscUrlConfig.getUrl(),
                scSastUrlConfig==null || scSastUrlConfig.getUrl()==null ? "unknown" : scSastUrlConfig.getUrl());
    }

    @Override
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
    
    @JsonIgnore
    protected SSCTokenCreateResponse generateToken(IUrlConfig urlConfig, ISSCCredentialsConfig credentialsConfig, SSCTokenHelper sscTokenHelper) {
        if ( credentialsConfig.getPredefinedToken()==null ) {
            ISSCUserCredentialsConfig uc = credentialsConfig.getUserCredentialsConfig();
            if ( uc!=null && StringUtils.isNotBlank(uc.getUser()) && uc.getPassword()!=null ) {
                SSCTokenCreateRequest tokenCreateRequest = SSCTokenCreateRequest.builder()
                        .description("Auto-generated by fcli session login command")
                        .terminalDate(getExpiresAt(uc.getExpiresAt())) 
                        .type("CIToken")
                        .build();
                return sscTokenHelper.createToken(urlConfig, uc, tokenCreateRequest, SSCTokenCreateResponse.class);
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
    
    private static final IUrlConfig buildScSastUrlConfig(IUrlConfig sscUrlConfig, IUrlConfig scSastUrlConfig, char[] activeToken, SSCTokenHelper sscTokenHelper) {
        String scSastUrl = scSastUrlConfig!=null && StringUtils.isNotBlank(scSastUrlConfig.getUrl())
                ? scSastUrlConfig.getUrl()
                : getScSastUrl(sscUrlConfig, activeToken, sscTokenHelper);
        UrlConfig.UrlConfigBuilder builder = UrlConfig.builderFrom(sscUrlConfig, scSastUrlConfig);
        builder.url(scSastUrl);
        return builder.build();
    }

    private static String getScSastUrl(IUrlConfig sscUrlConfig, char[] activeToken, SSCTokenHelper sscTokenHelper) {
        return sscTokenHelper.run(sscUrlConfig, activeToken, SCSastSessionData::getScSastUrl);
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
        
        return JsonHelper.evaluateSpELExpression(configData, "data.properties", ArrayNode.class);
    }
    
    private static final void checkScSastIsEnabled(ArrayNode properties) {
        boolean scSastEnabled = JsonHelper.evaluateSpELExpression(properties, "^[name=='cloud.ctrl.poll.enabled']?.value=='true'", Boolean.class);
        if (!scSastEnabled) {
            throw new IllegalStateException("ScanCentral SAST must be enabled in SSC");
        }
    }
    
    private static final String getScSastUrlFromProperties(ArrayNode properties) {
        String scSastUrl = JsonHelper.evaluateSpELExpression(properties, "^[name=='cloud.ctrl.url']?.value", String.class);
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
