/*
 * Copyright 2021-2026 Open Text.
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

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.action.runner.ActionProductContextProviders;
import com.fortify.cli.common.http.ssl.trust.FcliTrustManager;
import com.fortify.cli.common.i18n.helper.LanguageHelper;
import com.fortify.cli.fod.action.helper.FoDActionProductContextProvider;
import com.fortify.cli.ssc.action.helper.SSCActionProductContextProvider;
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
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Getter(lazy = true)
    private static final FortifyCLIStaticInitializer instance = new FortifyCLIStaticInitializer();
    
    public void initialize() {
        ToolUninstaller.deleteAllPending();
        initializeTrustStore();
        initializeLocale();
        initializeProductContextProviders();
        System.getProperties().putAll(FortifyCLIResourceBundlePropertiesHelper.getResourceBundleProperties());
    }
    
    private void initializeProductContextProviders() {
        ActionProductContextProviders.register(new SSCActionProductContextProvider());
        ActionProductContextProviders.register(new FoDActionProductContextProvider());
    }
    
    private void initializeTrustStore() {
        FcliTrustManager.installAsDefault();
        log.debug("Initialized refreshable fcli trust manager");
    }
    
    private void initializeLocale() {
        Locale.setDefault(LanguageHelper.getConfiguredLanguageDescriptor().getLocale());
    }
}
