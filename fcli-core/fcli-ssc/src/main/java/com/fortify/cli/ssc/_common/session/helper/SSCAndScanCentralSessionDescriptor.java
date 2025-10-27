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
package com.fortify.cli.ssc._common.session.helper;

import java.util.Date;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.IUserCredentialsConfig;
import com.fortify.cli.common.rest.unirest.config.UrlConfig;
import com.fortify.cli.common.session.helper.AbstractSessionDescriptor;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCAndScanCentralSessionLoginOptions.SSCAndScanCentralUrlConfigOptions.SSCComponentDisable;
import com.fortify.cli.ssc.access_control.helper.SSCTokenCreateRequest;
import com.fortify.cli.ssc.access_control.helper.SSCTokenGetOrCreateResponse;
import com.fortify.cli.ssc.access_control.helper.SSCTokenGetOrCreateResponse.SSCTokenData;
import com.fortify.cli.ssc.access_control.helper.SSCTokenHelper;

import kong.unirest.UnirestInstance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data @EqualsAndHashCode(callSuper = true) 
@Reflectable @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SSCAndScanCentralSessionDescriptor extends AbstractSessionDescriptor {
    @JsonDeserialize(as = UrlConfig.class) private IUrlConfig sscUrlConfig;
    @JsonDeserialize(as = UrlConfig.class) private IUrlConfig scSastUrlConfig;
    @JsonDeserialize(as = UrlConfig.class) private IUrlConfig scDastUrlConfig;
    private SSCTokenData sscTokenData;
    private boolean revokeTokenOnLogout;
    @MaskValue(sensitivity = LogSensitivityLevel.high, description = "SC SAST TOKEN")
    private char[] scSastClientAuthToken;
    private String scSastDisabledReason;
    private String scDastDisabledReason;
    
    @JsonIgnore
    public void logout(IUserCredentialsConfig userCredentialsConfig) {
        // We only revoke the token if we generated a token upon login, 
        // and that token hasn't expired yet.
        if ( revokeTokenOnLogout && hasActiveCachedTokenResponse() ) {
            SSCTokenHelper.revokeToken(getSscUrlConfig(), userCredentialsConfig, sscTokenData.getToken());
        }
    }
    
    @JsonIgnore @Override
    public String getUrlDescriptor() {
        return String.format("SSC:     %s\nSC-SAST: %s\nSC-DAST: %s", 
                getUrlDescriptor(sscUrlConfig, null),
                getUrlDescriptor(scSastUrlConfig, scSastDisabledReason),
                getUrlDescriptor(scDastUrlConfig, scDastDisabledReason));
    }
    
    @JsonIgnore
    private String getUrlDescriptor(IUrlConfig urlConfig, String disabledReason) {
        return urlConfig!=null && StringUtils.isNotBlank(urlConfig.getUrl()) && StringUtils.isBlank(disabledReason)
            ? urlConfig.getUrl()
            : "N/A" + (StringUtils.isBlank(disabledReason) ? "" : " ("+disabledReason+")");
    }

    @JsonIgnore 
    public final char[] getActiveSSCToken() {
        return hasActiveCachedTokenResponse() ? sscTokenData.getToken() : null;
    }
    
    @JsonIgnore
    public final boolean hasActiveCachedTokenResponse() {
        var terminalDate = sscTokenData.getTerminalDate(); // May be null for older SSC instances
        return terminalDate==null || terminalDate.after(new Date()); 
    }
    
    @JsonIgnore
    public Date getExpiryDate() {
        return sscTokenData.getTerminalDate(); // May be null for older SSC instances
    }
    
    @JsonIgnore @Override
    public String getType() {
        return SSCAndScanCentralSessionHelper.instance().getType();
    }
    
    public static final SSCAndScanCentralSessionDescriptor create(ISSCAndScanCentralUrlConfig sscAndScanCentralUrlConfig, ISSCAndScanCentralCredentialsConfig sscAndScanCentralCredentialsConfig) {
        var sscUrlConfig = UrlConfig.builderFromConnectionConfig(sscAndScanCentralUrlConfig).url(normalizeUrl(sscAndScanCentralUrlConfig.getSscUrl())).build();
        var providedSscToken = sscAndScanCentralCredentialsConfig.getSscToken();
        var sscTokenData = providedSscToken==null 
                ? generateSscToken(sscUrlConfig, sscAndScanCentralCredentialsConfig.getSscUserCredentialsConfig()) 
                : SSCTokenHelper.getTokenData(sscUrlConfig, providedSscToken);
        var sessionDescriptorBuilder = SSCAndScanCentralSessionDescriptor.builder()
                .sscUrlConfig(sscUrlConfig)
                .sscTokenData(sscTokenData)
                .revokeTokenOnLogout(providedSscToken==null);
        var sscConfigProperties = getSscConfigProperties(sscUrlConfig, sscTokenData.getToken());
        Set<SSCComponentDisable> disabledComponents = sscAndScanCentralUrlConfig.getDisabledComponents();
        if (disabledComponents.contains(SSCComponentDisable.sc_sast)) {
            sessionDescriptorBuilder.scSastDisabledReason("Disabled by user");
        } else {
            addScSastSessionConfig(sessionDescriptorBuilder, sscAndScanCentralUrlConfig,
                    sscAndScanCentralCredentialsConfig.getScSastClientAuthToken(), sscConfigProperties);
        }
        if (disabledComponents.contains(SSCComponentDisable.sc_dast))
            sessionDescriptorBuilder.scDastDisabledReason("Disabled by user");
        else
            addScDastSessionConfig(sessionDescriptorBuilder, sscAndScanCentralUrlConfig, sscConfigProperties);
        return sessionDescriptorBuilder.build();
    }
    
    private static final SSCTokenData generateSscToken(IUrlConfig sscUrlConfig, ISSCUserCredentialsConfig sscUserCredentialsConfig) {
        checkUserCredentials(sscUserCredentialsConfig);
        try {
            return generateSscToken(sscUrlConfig, sscUserCredentialsConfig, "AutomationToken");
        } catch ( UnexpectedHttpResponseException e ) {
            if ( e.getStatus()==400 ) { // AutomationToken isn't available on older SSC versions, so request UnifiedLoginToken
                return generateSscToken(sscUrlConfig, sscUserCredentialsConfig, "UnifiedLoginToken");
            } else {
                throw e;
            }
        }
    }

    private static void checkUserCredentials(ISSCUserCredentialsConfig sscUserCredentialsConfig) {
        if ( sscUserCredentialsConfig==null || StringUtils.isBlank(sscUserCredentialsConfig.getUser()) || sscUserCredentialsConfig.getPassword()==null ) {
            throw new FcliSimpleException("Unable to log in to SSC due to missing user credentials");
        }
    }
    
    private static final SSCTokenData generateSscToken(IUrlConfig urlConfig, ISSCUserCredentialsConfig sscUserCredentialsConfig, String tokenType) {
        SSCTokenCreateRequest tokenCreateRequest = SSCTokenCreateRequest.builder()
                .description("fcli session token")
                .terminalDate(sscUserCredentialsConfig.getExpiresAt()) 
                .type(tokenType)
                .build();
        return SSCTokenHelper.createToken(urlConfig, sscUserCredentialsConfig, tokenCreateRequest, SSCTokenGetOrCreateResponse.class).getData();
    }
    
    private static ArrayNode getSscConfigProperties(UrlConfig sscUrlConfig, char[] token) {
        try {
            return SSCTokenHelper.run(sscUrlConfig, token, SSCAndScanCentralSessionDescriptor::getSscConfigProperties);
        } catch ( Exception e ) {
            // Provided token likely doesn't allow for getting configuration properties
            return null;
        }
    }
    
    private static final ArrayNode getSscConfigProperties(UnirestInstance sscUnirest) {
        ObjectNode configData = sscUnirest.get("/api/v1/configuration")
                .asObject(ObjectNode.class)
                .getBody(); 
        return JsonHelper.evaluateSpelExpression(configData, "data.properties", ArrayNode.class);
    }
    
    private static final void addScSastSessionConfig(SSCAndScanCentralSessionDescriptorBuilder sessionDescriptorBuilder, ISSCAndScanCentralUrlConfig sscAndScanCentralUrlConfig, char[] scSastClientAuthToken, ArrayNode sscConfigProperties) {
        String disabledReason = null;
        String controllerUrl = sscAndScanCentralUrlConfig.getScSastControllerUrl();
        if ( StringUtils.isBlank(controllerUrl) ) {
            if ( sscConfigProperties==null ) {
                disabledReason = "Error retrieving SSC configuration";
            } else if ( JsonHelper.evaluateSpelExpression(sscConfigProperties, "^[name=='cloud.ctrl.poll.enabled']?.value!='true'", Boolean.class) ) {
                disabledReason = "Disabled in SSC";
            } else {
                controllerUrl = JsonHelper.evaluateSpelExpression(sscConfigProperties, "^[name=='cloud.ctrl.url']?.value", String.class);
            }
        }
        if ( StringUtils.isBlank(disabledReason) ) {
            if ( StringUtils.isBlank(controllerUrl) ) {
                disabledReason = "URL not configured";
            } else if ( scSastClientAuthToken==null ) {
                disabledReason = "No client-auth-token";
            }
        }
        if ( StringUtils.isNotBlank(disabledReason) ) {
            sessionDescriptorBuilder.scSastDisabledReason(disabledReason);
        } else {
            sessionDescriptorBuilder
                .scSastClientAuthToken(scSastClientAuthToken)
                .scSastUrlConfig(UrlConfig.builderFromConnectionConfig(sscAndScanCentralUrlConfig).url(normalizeUrl(controllerUrl)).build());
        }
    }

    private static final void addScDastSessionConfig(SSCAndScanCentralSessionDescriptorBuilder sessionDescriptorBuilder, ISSCAndScanCentralUrlConfig sscAndScanCentralUrlConfig, ArrayNode sscConfigProperties) {
        String apiUrl = null;
        String disabledReason = null;
        if ( sscConfigProperties==null ) {
            disabledReason = "Error retrieving SSC configuration";
        } else if ( JsonHelper.evaluateSpelExpression(sscConfigProperties, "^[name=='edast.enabled']?.value!='true'", Boolean.class) ) {
            disabledReason = "Disabled in SSC";
        } else {
            apiUrl = JsonHelper.evaluateSpelExpression(sscConfigProperties, "^[name=='edast.server.url']?.value", String.class);
            if ( StringUtils.isBlank(apiUrl) ) {
                disabledReason = "URL not configured";
            } else {
                apiUrl = apiUrl.replaceAll("/api/?$", ""); // Normalize URL, removing /api(/) suffix
            }
        }
        if ( StringUtils.isNotBlank(disabledReason) ) {
            sessionDescriptorBuilder.scDastDisabledReason(disabledReason);
        } else {
            sessionDescriptorBuilder
                .scDastUrlConfig(UrlConfig.builderFromConnectionConfig(sscAndScanCentralUrlConfig).url(normalizeUrl(apiUrl)).build());
        }
    }
    
    private static final String normalizeUrl(String url) {
        // We remove any trailing slashes from the URL as most users will specify relative URL's starting with a slash
        return url.replaceAll("/+$", "");
    }
}