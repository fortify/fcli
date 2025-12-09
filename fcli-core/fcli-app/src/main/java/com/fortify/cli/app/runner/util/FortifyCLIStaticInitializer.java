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
package com.fortify.cli.app.runner.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigDescriptor;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigHelper;
import com.fortify.cli.common.i18n.helper.LanguageHelper;
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
        System.getProperties().putAll(FortifyCLIResourceBundlePropertiesHelper.getResourceBundleProperties());
    }
    
    private void initializeTrustStore() {
        String trustStorePropertyKey = "javax.net.ssl.trustStore";
        String trustStoreTypePropertyKey = "javax.net.ssl.trustStoreType";
        String trustStorePasswordPropertyKey = "javax.net.ssl.trustStorePassword";

        // First clear existing configuration
        System.clearProperty(trustStorePropertyKey);
        System.clearProperty(trustStoreTypePropertyKey);
        System.clearProperty(trustStorePasswordPropertyKey);
        
        TrustStoreConfigDescriptor descriptor = TrustStoreConfigHelper.getTrustStoreConfig();
        if (descriptor != null && StringUtils.isNotBlank(descriptor.getPath())) {
            initializeTrustStoreFromConfig(descriptor, trustStorePropertyKey, trustStoreTypePropertyKey,
                    trustStorePasswordPropertyKey);
        } else {
            initializeTrustStoreFromEnv(trustStorePropertyKey, trustStoreTypePropertyKey,
                    trustStorePasswordPropertyKey);
        }
        log.debug("INFO: Trust store file: " + System.getProperty(trustStorePropertyKey, "NONE"));
    }

    private void initializeTrustStoreFromEnv(String trustStorePropertyKey, String trustStoreTypePropertyKey,
            String trustStorePasswordPropertyKey) {
        String trustStorePath = System.getenv("FCLI_TRUSTSTORE");
        if (null != trustStorePath && Files.exists(Path.of(trustStorePath))) {
            System.setProperty(trustStorePropertyKey, trustStorePath);
            
            String trustStoreType = "jks";
            if (null != System.getenv("FCLI_TRUSTSTORE_TYPE")) {
                trustStoreType = System.getenv("FCLI_TRUSTSTORE_TYPE");
            } else {
                String fileName = Paths.get(trustStorePath).getFileName().toString();
                String fileExtension = StringUtils.substringAfterLast(fileName, ".");
                if (fileExtension.equals("jks") || fileExtension.equals("p12") || fileExtension.equals("pfx")) {
                    trustStoreType = fileExtension;
                }
            }
            System.setProperty(trustStoreTypePropertyKey, trustStoreType);

            String trustStorePwd = "changeit";
            if (null != System.getenv("FCLI_TRUSTSTORE_PWD")) {
                trustStorePwd = System.getenv("FCLI_TRUSTSTORE_PWD");
            }
            System.setProperty(trustStorePasswordPropertyKey, trustStorePwd);
        }
    }

    private void initializeTrustStoreFromConfig(TrustStoreConfigDescriptor descriptor, String trustStorePropertyKey,
            String trustStoreTypePropertyKey, String trustStorePasswordPropertyKey) {
        Path absolutePath = Path.of(descriptor.getPath()).toAbsolutePath();
        if (!Files.exists(absolutePath)) {
            log.warn("WARN: Trust store cannot be found: " + absolutePath);
        }
        System.setProperty(trustStorePropertyKey, descriptor.getPath());
        if (StringUtils.isNotBlank(descriptor.getType())) {
            System.setProperty(trustStoreTypePropertyKey, descriptor.getType());
        }
        if (StringUtils.isNotBlank(descriptor.getPassword())) {
            System.setProperty(trustStorePasswordPropertyKey, descriptor.getPassword());
        }
    }
    
    private void initializeLocale() {
        Locale.setDefault(LanguageHelper.getConfiguredLanguageDescriptor().getLocale());
    }
}
