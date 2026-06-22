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
package com.fortify.cli.common.http.ssl.truststore.helper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.util.FcliDataHelper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TrustedUrlTrustStoreHelper {
    private static final int DEFAULT_HTTPS_PORT = 443;
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final String TLS_CONTEXT_PROTOCOL = "TLS";
    private static final String TLS_V1_2 = "TLSv1.2";
    private static final String TLS_V1_3 = "TLSv1.3";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
    private static final Path TRUSTED_URLS_PATH = FcliDataHelper.getFcliStatePath().resolve("ssl/trusted-urls");

    public static TrustedUrlCertificateDescriptor addTrustedUrl(String sourceUrl) {
        var target = parseAndValidateUrl(sourceUrl);
        var certificateChain = fetchServerCertificateChain(target.host(), target.port());
        var rootCertificate = selectRootCertificate(certificateChain);
        var descriptor = toDescriptor(target, sourceUrl, rootCertificate);
        FcliDataHelper.saveFile(getDescriptorPath(target.key()), descriptor, true);
        return descriptor;
    }

    public static TrustedUrlCertificateDescriptor removeTrustedUrl(String sourceUrl) {
        var target = parseAndValidateUrl(sourceUrl);
        var descriptorPath = getDescriptorPath(target.key());
        var existing = FcliDataHelper.readFile(descriptorPath, TrustedUrlCertificateDescriptor.class, false);
        if ( existing==null ) {
            throw new FcliSimpleException("No trusted URL found for "+target.url());
        }
        FcliDataHelper.deleteFile(descriptorPath, true);
        return existing;
    }

    public static Stream<TrustedUrlCertificateDescriptor> listTrustedUrls() {
        var files = FcliDataHelper.listFilesInDir(TRUSTED_URLS_PATH, false);
        if ( files==null ) {
            return Stream.empty();
        }
        return files
            .map(path->FcliDataHelper.readFile(path, TrustedUrlCertificateDescriptor.class, false))
            .filter(Objects::nonNull);
    }

    public static Stream<TrustedUrlCertificateDescriptor> clearTrustedUrls() {
        return listTrustedUrls()
            .peek(d->FcliDataHelper.deleteFile(getDescriptorPath(d.getKey()), true));
    }

    public static KeyStore getTrustedUrlsKeyStore() {
        var descriptors = listTrustedUrls().toList();
        if ( descriptors.isEmpty() ) {
            return null;
        }
        try {
            var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            for ( var descriptor : descriptors ) {
                keyStore.setCertificateEntry(descriptor.getKey(), fromPem(descriptor.getCertificatePem()));
            }
            return keyStore;
        } catch ( GeneralSecurityException | IOException e ) {
            throw new FcliTechnicalException("Error loading trusted URLs trust store", e);
        }
    }

    private static TrustedUrlCertificateDescriptor toDescriptor(UrlTarget target, String sourceUrl, X509Certificate cert) {
        return TrustedUrlCertificateDescriptor.builder()
            .key(target.key())
            .url(target.url())
            .host(target.host())
            .port(target.port())
            .sourceUrl(sourceUrl)
            .subject(cert.getSubjectX500Principal().getName())
            .issuer(cert.getIssuerX500Principal().getName())
            .serialNumber(cert.getSerialNumber().toString(16))
            .sha256(certificateSha256(cert))
            .notBefore(DATE_TIME_FORMATTER.format(cert.getNotBefore().toInstant()))
            .notAfter(DATE_TIME_FORMATTER.format(cert.getNotAfter().toInstant()))
            .certificatePem(toPem(cert))
            .createdAt(DATE_TIME_FORMATTER.format(Instant.now()))
            .build();
    }

    private static UrlTarget parseAndValidateUrl(String sourceUrl) {
        URI uri;
        try {
            uri = URI.create(sourceUrl.trim());
        } catch ( RuntimeException e ) {
            throw new FcliSimpleException("Invalid URL: "+sourceUrl);
        }
        var scheme = uri.getScheme();
        if ( scheme==null || !"https".equalsIgnoreCase(scheme) ) {
            throw new FcliSimpleException("URL must use https:// scheme");
        }
        var host = uri.getHost();
        if ( host==null || host.isBlank() ) {
            throw new FcliSimpleException("URL must include a host name");
        }
        var normalizedHost = host.toLowerCase(Locale.ROOT);
        var port = uri.getPort()>=0 ? uri.getPort() : DEFAULT_HTTPS_PORT;
        var key = normalizedHost+":"+port;
        var normalizedUrl = "https://"+normalizedHost+(port==DEFAULT_HTTPS_PORT ? "" : ":"+port);
        return new UrlTarget(key, normalizedUrl, normalizedHost, port);
    }

    private static List<X509Certificate> fetchServerCertificateChain(String host, int port) {
        try {
            var context = SSLContext.getInstance(TLS_CONTEXT_PROTOCOL);
            context.init(null, new TrustManager[]{TRUST_ALL_MANAGER}, null);
            try ( var socket = (SSLSocket)context.getSocketFactory().createSocket() ) {
                configureEnabledProtocols(socket);
                socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS);
                socket.setSoTimeout(CONNECT_TIMEOUT_MILLIS);
                socket.startHandshake();
                var session = socket.getSession();
                return extractCertificateChain(session);
            }
        } catch ( GeneralSecurityException | IOException e ) {
            throw new FcliSimpleException("Unable to retrieve certificates from https://"+host+":"+port, e);
        }
    }

    private static void configureEnabledProtocols(SSLSocket socket) {
        var supportedProtocols = List.of(socket.getSupportedProtocols());
        var enabledProtocols = new ArrayList<String>(2);
        if ( supportedProtocols.contains(TLS_V1_3) ) {
            enabledProtocols.add(TLS_V1_3);
        }
        if ( supportedProtocols.contains(TLS_V1_2) ) {
            enabledProtocols.add(TLS_V1_2);
        }
        if ( enabledProtocols.isEmpty() ) {
            throw new FcliSimpleException("No supported TLSv1.2 or TLSv1.3 protocol available");
        }
        socket.setEnabledProtocols(enabledProtocols.toArray(String[]::new));
    }

    private static List<X509Certificate> extractCertificateChain(SSLSession session) throws CertificateException {
        Certificate[] peerCertificates;
        try {
            peerCertificates = session.getPeerCertificates();
        } catch (SSLPeerUnverifiedException e) {
            throw new CertificateException("No peer certificates received", e);
        }
        if ( peerCertificates==null || peerCertificates.length==0 ) {
            throw new CertificateException("No peer certificates received");
        }
        List<X509Certificate> certificates = new ArrayList<>(peerCertificates.length);
        for ( var cert : peerCertificates ) {
            if ( cert instanceof X509Certificate x509Certificate ) {
                certificates.add(x509Certificate);
            }
        }
        if ( certificates.isEmpty() ) {
            throw new CertificateException("No X509 peer certificates received");
        }
        return certificates;
    }

    private static X509Certificate selectRootCertificate(List<X509Certificate> chain) {
        for ( int i = chain.size()-1; i>=0; i-- ) {
            var cert = chain.get(i);
            if ( isSelfSigned(cert) ) {
                return cert;
            }
        }
        return chain.get(chain.size()-1);
    }

    private static boolean isSelfSigned(X509Certificate cert) {
        try {
            cert.verify(cert.getPublicKey());
            return cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
        } catch ( Exception e ) {
            return false;
        }
    }

    private static Path getDescriptorPath(String key) {
        return TRUSTED_URLS_PATH.resolve(encodeKey(key)+".json");
    }

    private static String encodeKey(String key) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(StandardCharsets.UTF_8));
    }

    private static String certificateSha256(X509Certificate cert) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(cert.getEncoded());
            var result = new StringBuilder(hash.length*2);
            for ( var b : hash ) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch ( GeneralSecurityException e ) {
            throw new FcliTechnicalException("Unable to calculate certificate fingerprint", e);
        }
    }

    private static String toPem(X509Certificate cert) {
        try {
            var encoded = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(cert.getEncoded());
            return "-----BEGIN CERTIFICATE-----\n"+encoded+"\n-----END CERTIFICATE-----\n";
        } catch ( CertificateEncodingException e ) {
            throw new FcliTechnicalException("Unable to encode certificate as PEM", e);
        }
    }

    private static X509Certificate fromPem(String certificatePem) {
        try {
            var certificateFactory = CertificateFactory.getInstance("X.509");
            var inputStream = new ByteArrayInputStream(certificatePem.getBytes(StandardCharsets.US_ASCII));
            return (X509Certificate)certificateFactory.generateCertificate(inputStream);
        } catch ( CertificateException e ) {
            throw new FcliTechnicalException("Unable to parse stored certificate", e);
        }
    }

    private static final X509TrustManager TRUST_ALL_MANAGER = new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }
    };

    private record UrlTarget(String key, String url, String host, int port) {}
}
