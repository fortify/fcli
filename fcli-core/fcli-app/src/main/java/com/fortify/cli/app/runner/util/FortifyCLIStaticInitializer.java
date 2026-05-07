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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.action.runner.ActionProductContextProviders;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigDescriptor;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigHelper;
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

        // Merge OS platform trust store (e.g. Windows Certificate Store / macOS Keychain)
        // with the configured trust store so enterprise CAs are trusted automatically.
        initializePlatformTrustStore();
    }

    private void initializePlatformTrustStore() {
        KeyStore platformKeyStore = loadPlatformKeyStore();
        if (platformKeyStore == null) {
            return; // No OS trust store available on this platform or in this runtime
        }

        List<X509TrustManager> managers = new ArrayList<>();
        addTrustManagerFromKeyStore(managers, null); // null = read from javax.net.ssl.trustStore system props
        addTrustManagerFromKeyStore(managers, platformKeyStore);

        if (managers.size() < 2) {
            return; // Nothing new to composite
        }

        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{new CompositeX509TrustManager(managers)}, null);
            SSLContext.setDefault(ctx);
            log.debug("Composite SSL context installed: configured trust store + OS trust store");
        } catch (GeneralSecurityException e) {
            log.warn("Could not install composite SSL context with OS trust store: " + e.getMessage());
        }
    }

    private void addTrustManagerFromKeyStore(List<X509TrustManager> managers, KeyStore keyStore) {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore); // null = uses javax.net.ssl.trustStore system props
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager x509tm) {
                    managers.add(x509tm);
                }
            }
        } catch (GeneralSecurityException e) {
            log.debug("Could not load trust manager from key store: " + e.getMessage());
        }
    }

    private KeyStore loadPlatformKeyStore() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String type;
        if (os.contains("win")) {
            type = "Windows-ROOT";
        } else if (os.contains("mac")) {
            type = "KeychainStore";
        } else {
            return null; // Linux has no standard Java-accessible OS trust store
        }
        try {
            KeyStore ks = KeyStore.getInstance(type);
            ks.load(null, null);
            log.debug("Loaded OS trust store: " + type);
            return ks;
        } catch (Exception e) {
            // Provider may be unavailable in GraalVM native images built on a different OS
            log.debug("OS trust store unavailable (" + type + "): " + e.getMessage());
            return null;
        }
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

    private static final class CompositeX509TrustManager implements X509TrustManager {
        private final List<X509TrustManager> delegates;

        CompositeX509TrustManager(List<X509TrustManager> delegates) {
            this.delegates = List.copyOf(delegates);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            CertificateException last = null;
            for (X509TrustManager tm : delegates) {
                try { tm.checkClientTrusted(chain, authType); return; }
                catch (CertificateException e) { last = e; }
            }
            if (last != null) throw last;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            CertificateException last = null;
            for (X509TrustManager tm : delegates) {
                try { tm.checkServerTrusted(chain, authType); return; }
                catch (CertificateException e) { last = e; }
            }
            if (last != null) throw last;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegates.stream()
                .flatMap(tm -> Arrays.stream(tm.getAcceptedIssuers()))
                .toArray(X509Certificate[]::new);
        }
    }
}
