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
package com.fortify.cli.common.http.ssl.trust;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigDescriptor;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustStoreConfigHelper;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustedUrlCertificateDescriptor;
import com.fortify.cli.common.http.ssl.truststore.helper.TrustedUrlTrustStoreHelper;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.PlatformHelper;

/**
 * Process-wide SSL trust manager for fcli.
 *
 * <p>{@link #installAsDefault()} is called once during startup so every new TLS
 * client uses the same trust manager instance. Long-running processes such as
 * RPC and MCP servers can then call {@link #refresh()} or
 * {@link #refreshIfChanged()} at runtime to pick up trust-store changes without
 * restarting the JVM.</p>
 *
 * <p>Trust checks always run against an immutable snapshot referenced through an
 * {@link AtomicReference}, allowing refresh operations to atomically swap in a
 * newly loaded trust configuration without blocking in-flight TLS handshakes.</p>
 */
public final class FcliTrustManager extends X509ExtendedTrustManager {
    private static final Logger LOG = LoggerFactory.getLogger(FcliTrustManager.class);
    private static final String TRUST_STORE_PROPERTY_KEY = "javax.net.ssl.trustStore";
    private static final String TRUST_STORE_TYPE_PROPERTY_KEY = "javax.net.ssl.trustStoreType";
    private static final String TRUST_STORE_PASSWORD_PROPERTY_KEY = "javax.net.ssl.trustStorePassword";

    private static final FcliTrustManager INSTANCE = new FcliTrustManager();
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final int MAX_CERT_CHANGE_LOG_ENTRIES = 20;

    private final Object refreshLock = new Object();
    private final AtomicReference<TrustSnapshot> snapshotRef = new AtomicReference<>(TrustSnapshot.empty());
    private final AtomicLong refreshCounter = new AtomicLong(0);
    private final AtomicLong skippedRefreshCounter = new AtomicLong(0);
    private final AtomicLong failedRefreshCounter = new AtomicLong(0);
    private volatile String trustStateFingerprint = "";

    private FcliTrustManager() {}

    public static FcliTrustManager getInstance() {
        return INSTANCE;
    }

    /**
     * Installs this trust manager as the JVM default SSL trust manager.
     */
    public static void installAsDefault() {
        if (INSTALLED.compareAndSet(false, true)) {
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, new TrustManager[] {INSTANCE}, null);
                SSLContext.setDefault(ctx);
                LOG.debug("Installed refreshable fcli SSL trust manager");
            } catch (GeneralSecurityException e) {
                INSTALLED.set(false);
                LOG.warn("Could not install refreshable fcli SSL trust manager: {}", e.getMessage());
                return;
            }
        }
        refresh();
    }

    /**
     * Reloads the current trust configuration and replaces the active snapshot.
     */
    public static boolean refresh() {
        return INSTANCE.refreshInternal();
    }

    public static boolean refreshIfChanged() {
        return INSTANCE.refreshIfChangedInternal();
    }

    private boolean refreshIfChangedInternal() {
        var current = trustStateFingerprint;
        var latest = computeTrustStateFingerprint();
        if (Objects.equals(current, latest)) {
            skippedRefreshCounter.incrementAndGet();
            return true;
        }
        synchronized (refreshLock) {
            if (Objects.equals(trustStateFingerprint, latest)) {
                skippedRefreshCounter.incrementAndGet();
                return true;
            }
            return refreshInternal(latest, "changed");
        }
    }

    private boolean refreshInternal() {
        synchronized (refreshLock) {
            return refreshInternal(computeTrustStateFingerprint(), "forced");
        }
    }

    private boolean refreshInternal(String latestFingerprint, String reason) {
        var startNanos = System.nanoTime();
        var previousSnapshot = snapshotRef.get();
        var descriptor = TrustStoreConfigHelper.getTrustStoreConfig();
        applyTrustStoreSystemProperties(descriptor);

        List<X509TrustManager> managers = new ArrayList<>();
        addTrustManagerFromKeyStore(managers, null);

        if (!isOsTrustStoreDisabled(descriptor)) {
            var platformKeyStore = loadPlatformKeyStore();
            if (platformKeyStore != null) {
                addTrustManagerFromKeyStore(managers, platformKeyStore);
            }
        } else {
            LOG.debug("OS trust store merge disabled");
        }

        var trustedUrlsKeyStore = TrustedUrlTrustStoreHelper.getTrustedUrlsKeyStore();
        if (trustedUrlsKeyStore != null) {
            addTrustManagerFromKeyStore(managers, trustedUrlsKeyStore);
        }

        if (managers.isEmpty()) {
            failedRefreshCounter.incrementAndGet();
            LOG.warn("No trust managers available after refresh; keeping previous trust snapshot");
            return false;
        }

        var newSnapshot = TrustSnapshot.of(managers);
        snapshotRef.set(newSnapshot);
        trustStateFingerprint = latestFingerprint;
        var refreshId = refreshCounter.incrementAndGet();
        logRefreshSummary(refreshId, reason, startNanos, previousSnapshot, newSnapshot, descriptor);
        logChangedCertificates(refreshId, previousSnapshot, newSnapshot);
        logAcceptedIssuers(refreshId, newSnapshot.delegates());
        return true;
    }

    private void logRefreshSummary(long refreshId, String reason, long startNanos,
            TrustSnapshot previousSnapshot, TrustSnapshot newSnapshot, TrustStoreConfigDescriptor descriptor) {
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        int previousDelegates = previousSnapshot.delegates().size();
        int previousCerts = previousSnapshot.acceptedIssuers().length;
        int newDelegates = newSnapshot.delegates().size();
        int newCerts = newSnapshot.acceptedIssuers().length;
        LOG.debug("Trust refresh#{} reason={} took {}ms delegates {}->{} certs {}->{} skipped={} failed={} path={} useOs={} disableOs={}",
                refreshId,
                reason,
                elapsedMs,
                previousDelegates,
                newDelegates,
                previousCerts,
                newCerts,
                skippedRefreshCounter.get(),
                failedRefreshCounter.get(),
                descriptor == null ? "" : Objects.toString(descriptor.getPath(), ""),
                descriptor != null && !Boolean.FALSE.equals(descriptor.getUseOsTrustStore()),
                EnvHelper.asBoolean(EnvHelper.env("FCLI_DISABLE_OS_TRUSTSTORE")));
    }

    private void logChangedCertificates(long refreshId, TrustSnapshot previousSnapshot, TrustSnapshot newSnapshot) {
        if (!LOG.isDebugEnabled()) {
            return;
        }
        var previousCertIds = toCertIds(previousSnapshot.acceptedIssuers());
        var newCertIds = toCertIds(newSnapshot.acceptedIssuers());
        var added = new LinkedHashSet<String>(newCertIds);
        added.removeAll(previousCertIds);
        var removed = new LinkedHashSet<String>(previousCertIds);
        removed.removeAll(newCertIds);
        LOG.debug("Trust refresh#{} certificate delta: +{} -{}", refreshId, added.size(), removed.size());
        logCertificateIdSample(refreshId, "added", added);
        logCertificateIdSample(refreshId, "removed", removed);
    }

    private void logCertificateIdSample(long refreshId, String changeType, Set<String> certIds) {
        if (certIds.isEmpty()) {
            return;
        }
        int maxEntries = Math.min(MAX_CERT_CHANGE_LOG_ENTRIES, certIds.size());
        certIds.stream().limit(maxEntries)
                .forEach(id -> LOG.debug("Trust refresh#{} {} cert: {}", refreshId, changeType, id));
        if (certIds.size() > maxEntries) {
            LOG.debug("Trust refresh#{} {} certs truncated: showing {} of {}", refreshId, changeType, maxEntries, certIds.size());
        }
    }

    private Set<String> toCertIds(X509Certificate[] certificates) {
        var result = new LinkedHashSet<String>();
        if (certificates == null) {
            return result;
        }
        for (var cert : certificates) {
            result.add(certificateId(cert));
        }
        return result;
    }

    private String certificateId(X509Certificate cert) {
        if (cert == null) {
            return "null-cert";
        }
        return String.format("subject=%s issuer=%s serial=%s",
                cert.getSubjectX500Principal().getName(),
                cert.getIssuerX500Principal().getName(),
                cert.getSerialNumber().toString(16));
    }

    private String computeTrustStateFingerprint() {
        var descriptor = TrustStoreConfigHelper.getTrustStoreConfig();
        var descriptorFingerprint = String.join("|",
                Objects.toString(descriptor.getPath(), ""),
                Objects.toString(descriptor.getType(), ""),
                hashSecretForFingerprint(descriptor.getPassword()),
                Objects.toString(descriptor.getUseOsTrustStore(), ""));

        var trustedUrlsFingerprint = TrustedUrlTrustStoreHelper.listTrustedUrls()
                .sorted(Comparator.comparing(TrustedUrlCertificateDescriptor::getKey))
                .map(d -> String.join("|",
                        Objects.toString(d.getKey(), ""),
                        Objects.toString(d.getSha256(), ""),
                        Objects.toString(d.getNotAfter(), "")))
                .collect(Collectors.joining("::"));

        return String.join("#",
                descriptorFingerprint,
                trustedUrlsFingerprint,
                Objects.toString(EnvHelper.env("FCLI_TRUSTSTORE"), ""),
                Objects.toString(EnvHelper.env("FCLI_TRUSTSTORE_TYPE"), ""),
                hashSecretForFingerprint(EnvHelper.env("FCLI_TRUSTSTORE_PWD")),
                Objects.toString(EnvHelper.env("FCLI_DISABLE_OS_TRUSTSTORE"), ""),
                PlatformHelper.getOSString());
    }

    private String hashSecretForFingerprint(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        try {
            var bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (GeneralSecurityException e) {
            LOG.debug("Unable to hash trust-store secret for fingerprinting: {}", e.getMessage());
            return Integer.toHexString(value.hashCode());
        }
    }

    private void applyTrustStoreSystemProperties(TrustStoreConfigDescriptor descriptor) {
        System.clearProperty(TRUST_STORE_PROPERTY_KEY);
        System.clearProperty(TRUST_STORE_TYPE_PROPERTY_KEY);
        System.clearProperty(TRUST_STORE_PASSWORD_PROPERTY_KEY);

        if (descriptor != null && StringUtils.isNotBlank(descriptor.getPath())) {
            initializeTrustStoreFromConfig(descriptor);
        } else {
            initializeTrustStoreFromEnv();
        }
        LOG.debug("Trust store file: {}", System.getProperty(TRUST_STORE_PROPERTY_KEY, "NONE"));
    }

    private boolean isOsTrustStoreDisabled(TrustStoreConfigDescriptor descriptor) {
        if (EnvHelper.asBoolean(EnvHelper.env("FCLI_DISABLE_OS_TRUSTSTORE"))) {
            return true;
        }
        return descriptor != null && Boolean.FALSE.equals(descriptor.getUseOsTrustStore());
    }

    private void addTrustManagerFromKeyStore(List<X509TrustManager> managers, KeyStore keyStore) {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager x509tm) {
                    managers.add(x509tm);
                }
            }
        } catch (GeneralSecurityException e) {
            LOG.debug("Could not load trust manager from key store: {}", e.getMessage());
        }
    }

    private void logAcceptedIssuers(long refreshId, List<X509TrustManager> trustManagers) {
        if (!LOG.isTraceEnabled()) {
            return;
        }
        trustManagers.stream()
                .map(X509TrustManager::getAcceptedIssuers)
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .forEach(cert -> LOG.trace("Trust refresh#{} cert - Subject: {}, Issuer: {}",
                        refreshId,
                        cert.getSubjectX500Principal().getName(),
                        cert.getIssuerX500Principal().getName()));
    }

    private KeyStore loadPlatformKeyStore() {
        if (PlatformHelper.isWindows()) {
            return loadSingleKeyStore("Windows-ROOT");
        } else if (PlatformHelper.isMac()) {
            return loadSingleKeyStore("KeychainStore");
        } else {
            LOG.debug("No OS trust store loaded for platform: {}", PlatformHelper.getOSString());
            return null;
        }
    }

    private KeyStore loadSingleKeyStore(String type) {
        try {
            KeyStore ks = KeyStore.getInstance(type);
            ks.load(null, null);
            LOG.debug("Loaded OS trust store: {}", type);
            return ks;
        } catch (Exception | LinkageError e) {
            LOG.warn("OS trust store unavailable ({}): {}", type, e.getMessage());
            LOG.debug("OS trust store load failure details", e);
            return null;
        }
    }

    private void initializeTrustStoreFromEnv() {
        String trustStorePath = System.getenv("FCLI_TRUSTSTORE");
        if (trustStorePath != null && Files.exists(Path.of(trustStorePath))) {
            System.setProperty(TRUST_STORE_PROPERTY_KEY, trustStorePath);

            String trustStoreType = "jks";
            if (System.getenv("FCLI_TRUSTSTORE_TYPE") != null) {
                trustStoreType = System.getenv("FCLI_TRUSTSTORE_TYPE");
            } else {
                String fileName = Paths.get(trustStorePath).getFileName().toString();
                String fileExtension = StringUtils.substringAfterLast(fileName, ".");
                if (fileExtension.equals("jks") || fileExtension.equals("p12") || fileExtension.equals("pfx")) {
                    trustStoreType = fileExtension;
                }
            }
            System.setProperty(TRUST_STORE_TYPE_PROPERTY_KEY, trustStoreType);

            String trustStorePwd = "changeit";
            if (System.getenv("FCLI_TRUSTSTORE_PWD") != null) {
                trustStorePwd = System.getenv("FCLI_TRUSTSTORE_PWD");
            }
            System.setProperty(TRUST_STORE_PASSWORD_PROPERTY_KEY, trustStorePwd);
        }
    }

    private void initializeTrustStoreFromConfig(TrustStoreConfigDescriptor descriptor) {
        Path absolutePath = Path.of(descriptor.getPath()).toAbsolutePath();
        if (!Files.exists(absolutePath)) {
            LOG.warn("WARN: Trust store cannot be found: {}", absolutePath);
        }
        System.setProperty(TRUST_STORE_PROPERTY_KEY, descriptor.getPath());
        if (StringUtils.isNotBlank(descriptor.getType())) {
            System.setProperty(TRUST_STORE_TYPE_PROPERTY_KEY, descriptor.getType());
        }
        if (StringUtils.isNotBlank(descriptor.getPassword())) {
            System.setProperty(TRUST_STORE_PASSWORD_PROPERTY_KEY, descriptor.getPassword());
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        tryEach("checkClientTrusted", chain, tm -> tm.checkClientTrusted(chain, authType));
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        LOG.debug("checkServerTrusted(chain[{}], authType={})", chainLength(chain), authType);
        logChain(chain);
        tryEach("checkServerTrusted", chain, tm -> tm.checkServerTrusted(chain, authType));
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        tryEach("checkClientTrusted(socket)", chain, tm -> {
            if (tm instanceof X509ExtendedTrustManager ext) {
                ext.checkClientTrusted(chain, authType, socket);
            } else {
                tm.checkClientTrusted(chain, authType);
            }
        });
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        tryEach("checkClientTrusted(engine)", chain, tm -> {
            if (tm instanceof X509ExtendedTrustManager ext) {
                ext.checkClientTrusted(chain, authType, engine);
            } else {
                tm.checkClientTrusted(chain, authType);
            }
        });
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        LOG.debug("checkServerTrusted(chain[{}], authType={}, socket)", chainLength(chain), authType);
        logChain(chain);
        tryEach("checkServerTrusted(socket)", chain, tm -> {
            if (tm instanceof X509ExtendedTrustManager ext) {
                ext.checkServerTrusted(chain, authType, socket);
            } else {
                tm.checkServerTrusted(chain, authType);
            }
        });
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        LOG.debug("checkServerTrusted(chain[{}], authType={}, engine)", chainLength(chain), authType);
        logChain(chain);
        tryEach("checkServerTrusted(engine)", chain, tm -> {
            if (tm instanceof X509ExtendedTrustManager ext) {
                ext.checkServerTrusted(chain, authType, engine);
            } else {
                tm.checkServerTrusted(chain, authType);
            }
        });
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return snapshotRef.get().acceptedIssuers();
    }

    private int chainLength(X509Certificate[] chain) {
        return chain == null ? 0 : chain.length;
    }

    private void logChain(X509Certificate[] chain) {
        if (!LOG.isDebugEnabled() || chain == null) {
            return;
        }
        for (int i = 0; i < chain.length; i++) {
            LOG.debug("  cert[{}]: subject={}, issuer={}", i,
                    chain[i].getSubjectX500Principal().getName(),
                    chain[i].getIssuerX500Principal().getName());
        }
    }

    private void tryEach(String method, X509Certificate[] chain, TrustCheckFactory check) throws CertificateException {
        var snapshot = snapshotRef.get();
        CertificateException last = null;
        var delegates = snapshot.delegates();
        for (int i = 0; i < delegates.size(); i++) {
            var tm = delegates.get(i);
            try {
                check.check(tm);
                LOG.debug("{}: accepted by delegate[{}] {}", method, i, tm.getClass().getName());
                return;
            } catch (CertificateException e) {
                LOG.debug("{}: rejected by delegate[{}] {}: {}", method, i, tm.getClass().getName(), e.getMessage());
                last = e;
            }
        }
        LOG.debug("{}: all {} delegate(s) rejected the certificate chain", method, delegates.size());
        if (last != null) {
            throw last;
        }
        throw new CertificateException("No trust manager accepted the certificate chain");
    }

    private record TrustSnapshot(List<X509TrustManager> delegates, X509Certificate[] acceptedIssuers) {
        private static TrustSnapshot empty() {
            return new TrustSnapshot(List.of(), new X509Certificate[0]);
        }

        private static TrustSnapshot of(List<X509TrustManager> delegates) {
            var immutableDelegates = List.copyOf(delegates);
            var acceptedIssuers = immutableDelegates.stream()
                    .map(X509TrustManager::getAcceptedIssuers)
                    .filter(Objects::nonNull)
                    .flatMap(Arrays::stream)
                    .toArray(X509Certificate[]::new);
            return new TrustSnapshot(immutableDelegates, acceptedIssuers);
        }
    }

    @FunctionalInterface
    private interface TrustCheckFactory {
        void check(X509TrustManager tm) throws CertificateException;
    }
}
