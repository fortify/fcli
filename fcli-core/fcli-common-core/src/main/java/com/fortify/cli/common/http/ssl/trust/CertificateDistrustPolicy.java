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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

/**
 * Mandatory, offline CA-key distrust policy. This class uses only JDK APIs so CI
 * can also run its source file to sanitize the native-image builder's trust store.
 */
public final class CertificateDistrustPolicy {
    public static final String RESOURCE = "com/fortify/cli/common/http/ssl/trust/distrusted-keys.txt";
    public static final String REJECTION_MESSAGE = "Certificate rejected by fcli distrust policy";

    private final Map<String, Entry> keys;
    private final Map<X500Principal, List<Entry>> issuers;

    private CertificateDistrustPolicy(Map<String, Entry> keys) {
        this.keys = Map.copyOf(keys);
        var byIssuer = new LinkedHashMap<X500Principal, List<Entry>>();
        keys.values().forEach(entry -> byIssuer.computeIfAbsent(entry.subject(), k -> new ArrayList<>()).add(entry));
        this.issuers = Map.copyOf(byIssuer);
    }

    public static CertificateDistrustPolicy load(InputStream input) throws IOException, GeneralSecurityException {
        if (input == null) {
            throw new CertificateException("Missing mandatory fcli certificate distrust policy");
        }
        var entries = new LinkedHashMap<String, Entry>();
        try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.stripLeading().startsWith("#")) {
                    continue;
                }
                var fields = line.split("\\|", -1);
                if (fields.length != 4 || fields[3].isBlank()) {
                    throw new CertificateException("Invalid distrust policy entry at line " + lineNumber);
                }
                try {
                    var encoded = Base64.getDecoder().decode(fields[1]);
                    var key = KeyFactory.getInstance(fields[0]).generatePublic(new X509EncodedKeySpec(encoded));
                    var entry = new Entry(key, new X500Principal(fields[2]), fields[3]);
                    if (entries.putIfAbsent(fingerprint(key), entry) != null) {
                        throw new CertificateException("Duplicate distrusted key at line " + lineNumber);
                    }
                } catch (IllegalArgumentException e) {
                    throw new CertificateException("Invalid distrust policy entry at line " + lineNumber, e);
                }
            }
        }
        if (entries.isEmpty()) {
            throw new CertificateException("Empty mandatory fcli certificate distrust policy");
        }
        return new CertificateDistrustPolicy(entries);
    }

    public void check(X509Certificate certificate) throws CertificateException {
        var reason = rejectionReason(certificate);
        if (reason != null) {
            throw new CertificateException(REJECTION_MESSAGE + ": " + reason);
        }
    }

    public String rejectionReason(X509Certificate certificate) throws CertificateException {
        try {
            var entry = keys.get(fingerprint(certificate.getPublicKey()));
            if (entry != null) {
                return entry.reason();
            }
            // Also cover directly trusted leaves/sub-CAs whose compromised issuer was omitted.
            for (var issuer : issuers.getOrDefault(certificate.getIssuerX500Principal(), List.of())) {
                try {
                    certificate.verify(issuer.key());
                    return issuer.reason();
                } catch (InvalidKeyException | SignatureException e) {
                    // Same issuer name alone is not evidence that this key signed the certificate.
                }
            }
            return null;
        } catch (GeneralSecurityException e) {
            throw new CertificateException("Unable to apply mandatory fcli certificate distrust policy", e);
        }
    }

    public PKIXCertPathChecker newPathChecker() {
        return new PKIXCertPathChecker() {
            @Override
            public void init(boolean forward) {}

            @Override
            public boolean isForwardCheckingSupported() {
                return true;
            }

            @Override
            public Set<String> getSupportedExtensions() {
                return null;
            }

            @Override
            public void check(Certificate certificate, Collection<String> unresolved) throws CertPathValidatorException {
                if (certificate instanceof X509Certificate x509) {
                    try {
                        CertificateDistrustPolicy.this.check(x509);
                    } catch (CertificateException e) {
                        throw new CertPathValidatorException(e.getMessage(), e);
                    }
                }
            }
        };
    }

    public KeyStore filterTrustAnchors(X509Certificate[] certificates) throws GeneralSecurityException, IOException {
        var filtered = KeyStore.getInstance(KeyStore.getDefaultType());
        filtered.load(null, null);
        for (int i = 0; i < certificates.length; i++) {
            if (rejectionReason(certificates[i]) == null) {
                filtered.setCertificateEntry(Integer.toString(i), certificates[i]);
            }
        }
        return filtered;
    }

    private static String fingerprint(PublicKey key) throws GeneralSecurityException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getEncoded()));
    }

    private record Entry(PublicKey key, X500Principal subject, String reason) {}

    /**
     * Build-time helper: java CertificateDistrustPolicy.java policy-file cacerts-file.
     * Updates only the explicitly supplied builder store; never runs during fcli startup.
     */
    public static void main(String[] args) throws IOException, GeneralSecurityException {
        if (args.length != 2) {
            throw new CertificateException("Expected distrust policy file and builder cacerts file");
        }
        var policy = load(Files.newInputStream(Path.of(args[0])));
        var storePath = Path.of(args[1]);
        var password = "changeit".toCharArray();
        var store = KeyStore.getInstance(storePath.toFile(), password);
        int removed = 0;
        for (var alias : Collections.list(store.aliases())) {
            if (store.getCertificate(alias) instanceof X509Certificate certificate) {
                var reason = policy.rejectionReason(certificate);
                if (reason != null) {
                    store.deleteEntry(alias);
                    System.out.println("Removed distrusted builder certificate: " + alias + " (" + reason + ")");
                    removed++;
                }
            }
        }
        if (store.size() == 0) {
            throw new CertificateException("Refusing to write an empty builder trust store");
        }
        try (var output = Files.newOutputStream(storePath)) {
            store.store(output, password);
        }
        var saved = KeyStore.getInstance(storePath.toFile(), password);
        for (var alias : Collections.list(saved.aliases())) {
            if (saved.getCertificate(alias) instanceof X509Certificate certificate) {
                policy.check(certificate);
            }
        }
        System.out.println("Verified builder trust store; removed " + removed + " distrusted certificates");
    }
}
