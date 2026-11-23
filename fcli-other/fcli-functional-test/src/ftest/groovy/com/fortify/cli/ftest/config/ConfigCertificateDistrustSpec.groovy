package com.fortify.cli.ftest.config

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer

import spock.lang.Shared

@Prefix("config.certificate-distrust")
class ConfigCertificateDistrustSpec extends FcliBaseSpec {
    // Public SPKI of EM DV TLS CA - G3B, independently identified by CVE-2026-91403.
    private static final String DISTRUSTED_KEY =
        "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE557ZwEqXc8RzXlERHahawr1eZKs3S4DhLLwgOOweTw47gS/JhZ4eZ6+wTi+LyX90" +
        "hbplqpEe6KTkW2LVzAPVa+Z4s8OMtNyTfKJGDiOEU8iSk10p1E+R4VYGtjzO2rRo"
    private static final String PASSWORD = "test-password"
    @Shared Path directory
    @Shared KeyStore keys
    @Shared X509Certificate original
    @Shared X509Certificate prohibited
    @Shared AtomicInteger requests = new AtomicInteger()

    def setupSpec() {
        directory = Files.createTempDirectory("fcli-distrust-ftest-")
        def keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString()
        def process = new ProcessBuilder(keytool, "-genkeypair", "-alias", "server", "-dname", "CN=localhost",
            "-keyalg", "EC", "-groupname", "secp384r1", "-ext", "SAN=dns:localhost", "-validity", "3650",
            "-keystore", directory.resolve("server.p12").toString(), "-storetype", "PKCS12",
            "-storepass", PASSWORD).redirectErrorStream(true).start()
        assert process.waitFor(30, TimeUnit.SECONDS)
        assert process.exitValue() == 0: new String(process.inputStream.readAllBytes(), StandardCharsets.UTF_8)
        keys = KeyStore.getInstance(directory.resolve("server.p12").toFile(), PASSWORD.toCharArray())
        original = (X509Certificate)keys.getCertificate("server")
        prohibited = certificateWithDistrustedKey(original)
        Fcli.run("config truststore clear-trusted-urls")
        Fcli.run(["config", "truststore", "set", "-f", directory.resolve("server.p12").toString(),
            "--type", "PKCS12", "--password", PASSWORD, "--no-os-truststore"])
    }

    def cleanupSpec() {
        Fcli.run("config truststore clear-trusted-urls")
        Fcli.run("config truststore clear")
        if (directory != null) {
            Files.deleteIfExists(directory.resolve("server.p12"))
            Files.deleteIfExists(directory)
        }
    }

    def "allows local HTTPS using a configured trusted certificate"() {
        when:
            def result = withServer(false) { url ->
                Fcli.run(actionCommand(url))
            }
        then:
            result.success
            requests.get() == 1
    }

    def "rejects a distrusted key even when the leaf is explicitly trusted"() {
        when:
            def result = withServer(true) { url ->
                Fcli.run(actionCommand(url), { it.expectSuccess(false) })
            }
        then:
            result.stderr.any { it.contains("Certificate rejected by fcli distrust policy") }
            result.stderr.any { it.contains("CVE-2026-91403") }
            requests.get() == 0
    }

    def "cannot add a distrusted certificate through trusted URLs"() {
        when:
            def result = withServer(true) { url ->
                Fcli.run(["config", "truststore", "add-trusted-url", url], { it.expectSuccess(false) })
            }
        then:
            result.stderr.any { it.contains("Certificate rejected by fcli distrust policy") }
            !Fcli.run("config truststore list-trusted-urls").stdout.any { it.contains("localhost") }
    }

    private static List<String> actionCommand(String url) {
        ["action", "run", url, "--progress=none", "--on-unsigned=ignore", "--on-invalid-version=ignore"]
    }

    private def withServer(boolean includeDistrusted, Closure operation) {
        requests.set(0)
        def chain = includeDistrusted ? [original, prohibited] : [original]
        def key = (PrivateKey)keys.getKey("server", PASSWORD.toCharArray())
        def keyManager = new X509ExtendedKeyManager() {
            String[] getClientAliases(String type, Principal[] issuers) { null }
            String chooseClientAlias(String[] types, Principal[] issuers, Socket socket) { null }
            String[] getServerAliases(String type, Principal[] issuers) { type.startsWith("EC") ? ["server"] : null }
            String chooseServerAlias(String type, Principal[] issuers, Socket socket) { type.startsWith("EC") ? "server" : null }
            String chooseEngineServerAlias(String type, Principal[] issuers, SSLEngine engine) {
                type.startsWith("EC") ? "server" : null
            }
            X509Certificate[] getCertificateChain(String alias) { chain as X509Certificate[] }
            PrivateKey getPrivateKey(String alias) { key }
        }
        def context = SSLContext.getInstance("TLS")
        context.init([keyManager] as KeyManager[], null, null)
        def server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0)
        server.httpsConfigurator = new HttpsConfigurator(context)
        server.createContext("/action.yaml", { exchange ->
            requests.incrementAndGet()
            def body = getClass().getResourceAsStream("/runtime/actions/exit-code.yaml").withCloseable { it.readAllBytes() }
            exchange.sendResponseHeaders(200, body.length)
            exchange.responseBody.withCloseable { it.write(body) }
        })
        try {
            server.start()
            return operation("https://localhost:${server.address.port}/action.yaml".toString())
        } finally {
            server.stop(0)
        }
    }

    private static X509Certificate certificateWithDistrustedKey(X509Certificate certificate) {
        def encoded = certificate.encoded.clone()
        def oldKey = certificate.publicKey.encoded
        def newKey = Base64.decoder.decode(DISTRUSTED_KEY)
        assert oldKey.length == newKey.length
        int position = -1
        for (int i = 0; i <= encoded.length - oldKey.length; i++) {
            if (Arrays.equals(Arrays.copyOfRange(encoded, i, i + oldKey.length), oldKey)) {
                assert position == -1
                position = i
            }
        }
        assert position >= 0
        System.arraycopy(newKey, 0, encoded, position, newKey.length)
        // Deliberately invalid extra certificate: it must be vetoed before path validation,
        // even though the independently trusted original leaf already establishes trust.
        (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(encoded))
    }
}
