/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.app.runner.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigDescriptor;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigHelper;
import com.fortify.cli.common.i18n.helper.LanguageHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod.scan.helper.FoDScanStatus;
import com.fortify.cli.sc_dast.scan.helper.SCDastScanStatus;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobArtifactState;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobState;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactStatus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * This class is responsible for performing static initialization of fcli, i.e.,
 * initialization that is not dependent on command-line options.
 * 
 * @author Ruud Senden
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FortifyCLIStaticInitializer {
    private final Log log = LogFactory.getLog(getClass());
    @Getter(lazy = true)
    private static final FortifyCLIStaticInitializer instance = new FortifyCLIStaticInitializer(); 
    
    public void initialize() {
        initializeTrustStore();
        initializeLocale();
        initializeFoDProperties();
        initializeSCDastProperties();
        initializeSCSastProperties();
        initializeSSCProperties();
    }
    
    private void initializeFoDProperties() {
        System.setProperty("fcli.fod.scan.states", getValuesString(FoDScanStatus.values()));
        System.setProperty("fcli.fod.scan.states.complete", getValuesString(FoDScanStatus.getDefaultCompleteStates()));
    }
    
    private void initializeSCDastProperties() {
        System.setProperty("fcli.sc-dast.scan.states", getValuesString(SCDastScanStatus.values()));
        System.setProperty("fcli.sc-dast.scan.states.complete", getValuesString(SCDastScanStatus.getDefaultCompleteStates()));
    }
    
    private void initializeSCSastProperties() {
        System.setProperty("fcli.sc-sast.scan.jobStates", getValuesString(SCSastControllerScanJobState.values()));
        System.setProperty("fcli.sc-sast.scan.jobStates.complete", getValuesString(SCSastControllerScanJobState.getDefaultCompleteStates()));
        System.setProperty("fcli.sc-sast.scan.jobArtifactStates", getValuesString(SCSastControllerScanJobArtifactState.values()));
        System.setProperty("fcli.sc-sast.scan.jobArtifactStates.complete", getValuesString(SCSastControllerScanJobArtifactState.getDefaultCompleteStates()));
    }
    
    private void initializeSSCProperties() {
        System.setProperty("fcli.ssc.artifact.states", getValuesString(SSCArtifactStatus.values()));
        System.setProperty("fcli.ssc.artifact.states.complete", getValuesString(SSCArtifactStatus.getDefaultCompleteStates()));
    }
    
    private void initializeTrustStore() {
        TrustStoreConfigDescriptor descriptor = TrustStoreConfigHelper.getTrustStoreConfig();
        if ( descriptor!=null && StringUtils.isNotBlank(descriptor.getPath()) ) {
            Path absolutePath = Path.of(descriptor.getPath()).toAbsolutePath();
            if ( !Files.exists(absolutePath) ) {
                log.warn("WARN: Trust store cannot be found: "+absolutePath);
            }
            System.setProperty("javax.net.ssl.trustStore", descriptor.getPath());
            if ( StringUtils.isNotBlank(descriptor.getType()) ) {
                System.setProperty("javax.net.ssl.trustStoreType", descriptor.getType());
            }
            if ( StringUtils.isNotBlank(descriptor.getPassword()) ) {
                System.setProperty("javax.net.ssl.trustStorePassword", descriptor.getPassword());
            }
        }
    }
    
    private void initializeLocale() {
        Locale.setDefault(LanguageHelper.getConfiguredLanguageDescriptor().getLocale());
    }

    private String getValuesString(Enum<?>[] values) {
        return Stream.of(values).map(Enum::name).collect(Collectors.joining(", "));
    }
}
