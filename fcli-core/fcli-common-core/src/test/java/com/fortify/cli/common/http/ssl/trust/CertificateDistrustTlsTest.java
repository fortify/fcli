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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

class CertificateDistrustTlsTest {
    private static final char[] PASSWORD = "test-password".toCharArray();
    @TempDir static Path directory;

    @BeforeAll
    static void generateCertificates() throws Exception {
        generateKey("root", "CN=Test Root", true);
        generateKey("namesake", "CN=Test Root", true);
        exportCertificate("root");
        generateKey("blocked", "CN=Test Intermediate", true);
        sign("blocked", "root", true, false);
        generateKey("allowed", "CN=localhost", false);
        sign("allowed", "root", false, false);
        generateKey("denied", "CN=localhost", false);
        sign("denied", "blocked", false, false);
        generateKey("expired", "CN=localhost", false);
        sign("expired", "root", false, true);
    }

    @Test
    void acceptsTrustedTlsButRejectsDistrustedTlsAndInvalidHostnameAndDates() throws Exception {
        var policy = policyFor(certificate("blocked"));
        var manager = manager(policy, trustStore(certificate("root")));
        assertEquals("ok", request(manager, "allowed", "localhost"));
        var failure = assertThrows(Exception.class, () -> request(manager, "denied", "localhost"));
        assertTrue(causeMessages(failure).contains(CertificateDistrustPolicy.REJECTION_MESSAGE));
        var wrongHost = assertThrows(Exception.class, () -> request(manager, "allowed", "127.0.0.1"));
        assertTrue(causeMessages(wrongHost).contains("subject alternative"));
        var expired = assertThrows(Exception.class, () -> request(manager, "expired", "localhost"));
        assertTrue(causeMessages(expired).contains("NotAfter"));
    }

    @Test
    void excludesRootEvenWhenPeerOmitsItAndCannotBeOverriddenByAnotherStore() throws Exception {
        var policy = policyFor(certificate("root"));
        var factory = new FcliTrustManager(policy, List.of());
        var mixedStore = trustStore(certificate("root"), certificate("blocked"));
        assertEquals(0, factory.createTrustManagers(mixedStore).size());

        var calls = new AtomicInteger();
        var manager = new FcliTrustManager(policy, List.of(acceptingDelegate(calls), acceptingDelegate(calls)));
        assertThrows(CertificateException.class, () -> manager.checkServerTrusted(chain("allowed"), "RSA"));
        assertEquals(0, calls.get());
    }

    @Test
    void directlyTrustedLeafSignedByDistrustedIntermediateIsAlsoExcluded() throws Exception {
        var policy = policyFor(certificate("blocked"));
        assertTrue(policy.rejectionReason(certificate("denied")).contains("test distrust"));
        assertEquals(0, new FcliTrustManager(policy, List.of())
                .createTrustManagers(trustStore(certificate("denied"))).size());
        assertThrows(CertificateException.class, () -> policy.check(certificate("blocked")));
        assertDoesNotThrow(() -> policy.check(certificate("allowed")));
    }

    @Test
    void issuerNameAloneDoesNotDistrustAnUnrelatedKey() throws Exception {
        var root = certificate("root");
        var namesake = certificate("namesake");
        assertEquals(root.getSubjectX500Principal(), namesake.getSubjectX500Principal());
        assertDoesNotThrow(() -> policyFor(root).check(namesake));
    }

    @Test
    void pathCheckerRejectsIntermediateEvenWithoutOuterChainCheck() throws Exception {
        var store = trustStore(certificate("root"));
        var baseline = TrustManagerFactory.getInstance("PKIX");
        baseline.init(store);
        var original = (X509TrustManager) baseline.getTrustManagers()[0];
        assertDoesNotThrow(() -> original.checkServerTrusted(chain("denied"), "RSA"));

        var filtered = new FcliTrustManager(policyFor(certificate("blocked")), List.of()).createTrustManagers(store);
        assertThrows(CertificateException.class, () -> filtered.get(0).checkServerTrusted(chain("denied"), "RSA"));
        assertDoesNotThrow(() -> filtered.get(0).checkServerTrusted(chain("allowed"), "RSA"));
    }

    @Test
    void allTrustCheckOverloadsApplyMandatoryVeto() throws Exception {
        var calls = new AtomicInteger();
        var manager = new FcliTrustManager(policyFor(certificate("blocked")), List.of(acceptingDelegate(calls)));
        var chain = new X509Certificate[] {certificate("blocked")};
        var engine = SSLContext.getDefault().createSSLEngine();
        try (var socket = new Socket()) {
            assertThrows(CertificateException.class, () -> manager.checkServerTrusted(chain, "RSA"));
            assertThrows(CertificateException.class, () -> manager.checkClientTrusted(chain, "RSA"));
            assertThrows(CertificateException.class, () -> manager.checkServerTrusted(chain, "RSA", socket));
            assertThrows(CertificateException.class, () -> manager.checkClientTrusted(chain, "RSA", socket));
            assertThrows(CertificateException.class, () -> manager.checkServerTrusted(chain, "RSA", engine));
            assertThrows(CertificateException.class, () -> manager.checkClientTrusted(chain, "RSA", engine));
        }
        assertEquals(0, calls.get());
        assertThrows(CertificateException.class, () -> manager.checkServerTrusted(new X509Certificate[0], "RSA"));
        assertThrows(CertificateException.class, () -> manager.checkServerTrusted(null, "RSA"));
    }

    @Test
    void pairedValidationPreservesContextOrderAndRejectionsForEveryOverload() throws Exception {
        var chain = chain("allowed");
        var engine = SSLContext.getDefault().createSSLEngine();
        try (var socket = new Socket()) {
            var checks = List.<TrustInvocation>of(
                    tm -> tm.checkClientTrusted(chain, "RSA"),
                    tm -> tm.checkServerTrusted(chain, "RSA"),
                    tm -> tm.checkClientTrusted(chain, "RSA", socket),
                    tm -> tm.checkServerTrusted(chain, "RSA", socket),
                    tm -> tm.checkClientTrusted(chain, "RSA", engine),
                    tm -> tm.checkServerTrusted(chain, "RSA", engine));
            for (int i = 0; i < checks.size(); i++) {
                var events = new ArrayList<String>();
                var original = new RecordingTrustManager("original", events, chain);
                var constrained = new RecordingTrustManager("constrained", events, chain);
                var paired = new FcliTrustManager.PolicyConstrainedTrustManager(original, constrained);
                checks.get(i).check(paired);
                assertEquals(List.of("original:" + i, "constrained:" + i), events);
                Object context = i < 2 ? null : i < 4 ? socket : engine;
                assertSame(context, original.context);
                assertSame(context, constrained.context);
                assertSame(chain, paired.getAcceptedIssuers());

                events.clear();
                original.failure = new CertificateException("Original provider restriction");
                int index = i;
                assertSame(original.failure, assertThrows(CertificateException.class, () -> checks.get(index).check(paired)));
                assertEquals(List.of("original:" + i), events);

                events.clear();
                original.failure = null;
                constrained.failure = new CertificateException("Policy validation restriction");
                assertSame(constrained.failure, assertThrows(CertificateException.class, () -> checks.get(index).check(paired)));
                assertEquals(List.of("original:" + i, "constrained:" + i), events);
            }
        }
    }

    @Test
    void cannotCombineOriginalAcceptanceAndPolicyAcceptanceFromDifferentSources() throws Exception {
        var calls = new AtomicInteger();
        var accepting = acceptingDelegate(calls);
        var chain = chain("allowed");
        var rejecting = new RecordingTrustManager("rejecting", new ArrayList<>(), chain);
        rejecting.failure = new CertificateException("Rejected by this source");
        var manager = new FcliTrustManager(policyFor(certificate("blocked")), List.of(
                new FcliTrustManager.PolicyConstrainedTrustManager(accepting, rejecting),
                new FcliTrustManager.PolicyConstrainedTrustManager(rejecting, accepting)));
        assertThrows(CertificateException.class, () -> manager.checkServerTrusted(chain, "RSA"));
        assertEquals(1, calls.get());

        var fallback = new FcliTrustManager(policyFor(certificate("blocked")), List.of(
                new FcliTrustManager.PolicyConstrainedTrustManager(rejecting, accepting),
                new FcliTrustManager.PolicyConstrainedTrustManager(accepting, accepting)));
        fallback.checkServerTrusted(chain, "RSA");
        assertEquals(3, calls.get());
    }

    @Test
    void pairedValidationSupportsNonExtendedManagersForSocketAndEngineChecks() throws Exception {
        var calls = new AtomicInteger();
        var paired = new FcliTrustManager.PolicyConstrainedTrustManager(acceptingDelegate(calls), acceptingDelegate(calls));
        try (var socket = new Socket()) {
            var chain = chain("allowed");
            var engine = SSLContext.getDefault().createSSLEngine();
            paired.checkClientTrusted(chain, "RSA", socket);
            paired.checkServerTrusted(chain, "RSA", socket);
            paired.checkClientTrusted(chain, "RSA", engine);
            paired.checkServerTrusted(chain, "RSA", engine);
        }
        assertEquals(8, calls.get());
    }

    @Test
    void blocksReissuedKeyAndRejectsDuplicatePolicyEntries() throws Exception {
        var blocked = certificate("blocked");
        var entry = policyEntry(blocked);
        var policy = CertificateDistrustPolicyTest.load(entry);
        assertThrows(CertificateException.class, () -> CertificateDistrustPolicyTest.load(entry + "\n" + entry));
        keytool("-gencert", "-alias", "root", "-keystore", storePath("root").toString(),
                "-infile", directory.resolve("blocked.csr").toString(), "-outfile", directory.resolve("reissued.crt").toString(),
                "-dname", "CN=Different name", "-validity", "365", "-ext", "BC=ca:true");
        try (var input = Files.newInputStream(directory.resolve("reissued.crt"))) {
            var reissued = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(input);
            assertFalse(reissued.getSubjectX500Principal().equals(blocked.getSubjectX500Principal()));
            assertThrows(CertificateException.class, () -> policy.check(reissued));
        }
    }

    @Test
    void builderSanitizerRemovesKeysRegardlessOfAliasAndIsIdempotent() throws Exception {
        var policyFile = directory.resolve("test-policy.txt");
        Files.writeString(policyFile, policyEntry(certificate("blocked")));
        var storeFile = directory.resolve("builder-store");
        var store = trustStore(certificate("root"), certificate("blocked"), certificate("denied"));
        try (var output = Files.newOutputStream(storeFile)) {
            store.store(output, "changeit".toCharArray());
        }
        for (int i = 0; i < 2; i++) {
            CertificateDistrustPolicy.main(new String[] {policyFile.toString(), storeFile.toString()});
            assertEquals(1, KeyStore.getInstance(storeFile.toFile(), "changeit".toCharArray()).size());
        }
    }

    private static FcliTrustManager manager(CertificateDistrustPolicy policy, KeyStore store) throws Exception {
        var factory = new FcliTrustManager(policy, List.of());
        return new FcliTrustManager(policy, factory.createTrustManagers(store));
    }

    private static String policyEntry(X509Certificate certificate) {
        var key = certificate.getPublicKey();
        return key.getAlgorithm() + "|" + Base64.getEncoder().encodeToString(key.getEncoded()) + "|"
                + certificate.getSubjectX500Principal().getName() + "|test distrust";
    }

    private static CertificateDistrustPolicy policyFor(X509Certificate certificate) throws Exception {
        return CertificateDistrustPolicyTest.load(policyEntry(certificate));
    }

    private static KeyStore trustStore(X509Certificate... certificates) throws Exception {
        var store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        for (int i = 0; i < certificates.length; i++) {
            store.setCertificateEntry("arbitrary-alias-" + i, certificates[i]);
        }
        return store;
    }

    private static X509Certificate certificate(String alias) throws Exception {
        return (X509Certificate) loadStore(alias).getCertificate(alias);
    }

    private static X509Certificate[] chain(String alias) throws Exception {
        return Arrays.stream(loadStore(alias).getCertificateChain(alias))
                .map(X509Certificate.class::cast).toArray(X509Certificate[]::new);
    }

    private static KeyStore loadStore(String alias) throws Exception {
        return KeyStore.getInstance(storePath(alias).toFile(), PASSWORD);
    }

    private static Path storePath(String alias) {
        return directory.resolve(alias + ".p12");
    }

    private static void generateKey(String alias, String subject, boolean ca) throws Exception {
        keytool("-genkeypair", "-alias", alias, "-dname", subject, "-keyalg", "RSA", "-keysize", "2048",
                "-keystore", storePath(alias).toString(), "-storetype", "PKCS12", "-validity", "3650",
                "-ext", ca ? "BC=ca:true" : "SAN=dns:localhost");
    }

    private static void exportCertificate(String alias) throws Exception {
        keytool("-exportcert", "-alias", alias, "-keystore", storePath(alias).toString(),
                "-file", directory.resolve(alias + ".crt").toString());
    }

    private static void sign(String alias, String issuer, boolean ca, boolean expired) throws Exception {
        var csr = directory.resolve(alias + ".csr").toString();
        var signed = directory.resolve(alias + ".crt").toString();
        keytool("-certreq", "-alias", alias, "-keystore", storePath(alias).toString(), "-file", csr);
        keytool("-gencert", "-alias", issuer, "-keystore", storePath(issuer).toString(), "-infile", csr, "-outfile", signed,
                "-validity", expired ? "1" : "365", "-startdate", expired ? "-10d" : "-1d",
                "-ext", ca ? "BC=ca:true" : "SAN=dns:localhost");
        keytool("-importcert", "-noprompt", "-alias", "root", "-keystore", storePath(alias).toString(),
                "-file", directory.resolve("root.crt").toString());
        if (!issuer.equals("root")) {
            keytool("-importcert", "-noprompt", "-alias", issuer, "-keystore", storePath(alias).toString(),
                    "-file", directory.resolve(issuer + ".crt").toString());
        }
        keytool("-importcert", "-noprompt", "-alias", alias, "-keystore", storePath(alias).toString(), "-file", signed);
    }

    private static void keytool(String... args) throws Exception {
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "keytool").toString());
        command.addAll(List.of(args));
        command.addAll(List.of("-storepass", new String(PASSWORD)));
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "keytool timed out");
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
    }

    private static String request(FcliTrustManager manager, String serverAlias, String host) throws Exception {
        var keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(loadStore(serverAlias), PASSWORD);
        var serverContext = SSLContext.getInstance("TLS");
        serverContext.init(keys.getKeyManagers(), null, null);
        var server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(serverContext));
        server.createContext("/", exchange -> {
            var body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        var previous = SSLContext.getDefault();
        try {
            server.start();
            var clientContext = SSLContext.getInstance("TLS");
            clientContext.init(null, new TrustManager[] {manager}, null);
            SSLContext.setDefault(clientContext);
            try (var unirest = UnirestHelper.createUnirestInstance()) {
                unirest.config().connectTimeout(3000).socketTimeout(3000);
                var response = unirest.get("https://" + host + ":" + server.getAddress().getPort() + "/").asString();
                assertEquals(200, response.getStatus());
                return response.getBody();
            }
        } finally {
            SSLContext.setDefault(previous);
            server.stop(0);
        }
    }

    private static String causeMessages(Throwable exception) {
        var text = new StringBuilder();
        for (var cause = exception; cause != null; cause = cause.getCause()) {
            text.append(cause.getMessage()).append('\n');
        }
        return text.toString();
    }

    @FunctionalInterface
    private interface TrustInvocation {
        void check(X509ExtendedTrustManager manager) throws CertificateException;
    }

    private static final class RecordingTrustManager extends X509ExtendedTrustManager {
        private final String name;
        private final List<String> events;
        private final X509Certificate[] expectedChain;
        private Object context;
        private CertificateException failure;

        private RecordingTrustManager(String name, List<String> events, X509Certificate[] expectedChain) {
            this.name = name;
            this.events = events;
            this.expectedChain = expectedChain;
        }

        private void record(int overload, X509Certificate[] chain, String authType, Object context) throws CertificateException {
            events.add(name + ":" + overload);
            assertSame(expectedChain, chain);
            assertEquals("RSA", authType);
            this.context = context;
            if (failure != null) {
                throw failure;
            }
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            record(0, chain, authType, null);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            record(1, chain, authType, null);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            record(2, chain, authType, socket);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            record(3, chain, authType, socket);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            record(4, chain, authType, engine);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            record(5, chain, authType, engine);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return expectedChain;
        }
    }

    private static X509TrustManager acceptingDelegate(AtomicInteger calls) {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                calls.incrementAndGet();
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                calls.incrementAndGet();
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }
}
