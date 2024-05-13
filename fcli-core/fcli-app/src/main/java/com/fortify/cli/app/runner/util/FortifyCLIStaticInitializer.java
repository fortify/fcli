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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigDescriptor;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigHelper;
import com.fortify.cli.common.i18n.helper.LanguageHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.scan.helper.FoDScanStatus;
import com.fortify.cli.sc_dast.scan.helper.SCDastScanStatus;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobArtifactState;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobState;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactStatus;
import com.fortify.cli.tool._common.helper.ToolUninstaller;

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
        ToolUninstaller.deleteAllPending();
        initializeTrustStore();
        initializeLocale();
        initializeFoDProperties();
        initializeSCDastProperties();
        initializeSCSastProperties();
        initializeSSCProperties();
        initializeActionProperties();
    }
    
    private void initializeFoDProperties() {
        System.setProperty("fcli.fod.scan.states", getValueNamesString(FoDScanStatus.values()));
        System.setProperty("fcli.fod.scan.states.complete", getValueNamesString(FoDScanStatus.getDefaultCompleteStates()));
    }
    
    private void initializeSCDastProperties() {
        System.setProperty("fcli.sc-dast.scan.states", getValueNamesString(SCDastScanStatus.values()));
        System.setProperty("fcli.sc-dast.scan.states.complete", getValueNamesString(SCDastScanStatus.getDefaultCompleteStates()));
    }
    
    private void initializeSCSastProperties() {
        System.setProperty("fcli.sc-sast.scan.jobStates", getValueNamesString(SCSastControllerScanJobState.values()));
        System.setProperty("fcli.sc-sast.scan.jobStates.complete", getValueNamesString(SCSastControllerScanJobState.getDefaultCompleteStates()));
        System.setProperty("fcli.sc-sast.scan.jobArtifactStates", getValueNamesString(SCSastControllerScanJobArtifactState.values()));
        System.setProperty("fcli.sc-sast.scan.jobArtifactStates.complete", getValueNamesString(SCSastControllerScanJobArtifactState.getDefaultCompleteStates()));
    }
    
    private void initializeSSCProperties() {
        System.setProperty("fcli.ssc.artifact.states", getValueNamesString(SSCArtifactStatus.values()));
        System.setProperty("fcli.ssc.artifact.states.complete", getValueNamesString(SSCArtifactStatus.getDefaultCompleteStates()));
    }
    
    private void initializeActionProperties() {
        System.setProperty("fcli.action.supportedSchemaVersions", "TODO");
    }
    
    private void initializeTrustStore() {
        // First clear existing configuration
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("avax.net.ssl.trustStorePassword");
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
    
    private String getValueNamesString(Enum<?>[] values) {
        return getValuesString(values, Enum::name);
    }
    
    private String getValuesString(Enum<?>[] values, Function<Enum<?>, String> f) {
        return Stream.of(values).map(f).collect(Collectors.joining(", "));
    }
}
