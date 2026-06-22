package com.fortify.cli.ftest.config

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix

import spock.lang.Stepwise

@Prefix("config.truststore.trusted-url") @Stepwise
class ConfigTrustStoreTrustedUrlSpec extends FcliBaseSpec {
    private static final String SELF_SIGNED_URL = "https://self-signed.badssl.com/"
    private static final String UNTRUSTED_ROOT_URL = "https://untrusted-root.badssl.com/"
    private static final String SELF_SIGNED_HOST = "self-signed.badssl.com"
    private static final String UNTRUSTED_ROOT_HOST = "untrusted-root.badssl.com"

    def setupSpec() {
        Fcli.run("config truststore clear")
        Fcli.run("config truststore clear-trusted-urls")
    }

    def cleanupSpec() {
        Fcli.run("config truststore clear-trusted-urls")
        Fcli.run("config truststore clear")
    }

    def "trust functionality blocks untrusted TLS by default"() {
        expectPkixFailure(sscSessionLoginCommand())
        expectPkixFailure(toolDefinitionsUpdateCommand())
        expectPkixFailure(actionRunCommand())
    }

    def "trust functionality allows adding and listing trusted urls"() {
        when:
            addTrustedUrl(SELF_SIGNED_URL)
            addTrustedUrl(UNTRUSTED_ROOT_URL)
            def listResult = Fcli.run("config truststore list-trusted-urls")
        then:
            verifyAll(listResult.stdout) {
                it.any { line -> line.contains(SELF_SIGNED_HOST) }
                it.any { line -> line.contains(UNTRUSTED_ROOT_HOST) }
            }
    }

    def "trust functionality allows TLS handshake but still rejects invalid content"() {
        expectNonPkixFailure(
            sscSessionLoginCommand(),
            ["Error connecting to SSC", "Unable to connect to", "Unexpected issue while executing REST request"]
        )
        expectNonPkixFailure(
            toolDefinitionsUpdateCommand(),
            ["Invalid or corrupted ZIP file", "Invalid tool definitions file"]
        )
        expectNonPkixFailure(
            actionRunCommand(),
            ["Error loading action from", "Error loading action"]
        )
    }

    def "trust functionality keeps trusted urls when clearing configured trust store"() {
        when:
            Fcli.run("config truststore clear")
            def listResult = Fcli.run("config truststore list-trusted-urls")
        then:
            verifyAll(listResult.stdout) {
                it.any { line -> line.contains(SELF_SIGNED_HOST) }
                it.any { line -> line.contains(UNTRUSTED_ROOT_HOST) }
            }
    }

    def "trust functionality supports clearing trusted urls"() {
        when:
            def clearResult = Fcli.run("config truststore clear-trusted-urls")
            def listResult = Fcli.run("config truststore list-trusted-urls")
        then:
            verifyAll(clearResult.stdout) {
                it.any { line -> line.contains(SELF_SIGNED_HOST) }
                it.any { line -> line.contains(UNTRUSTED_ROOT_HOST) }
            }
            verifyAll(listResult.stdout) {
                !it.any { line -> line.contains(SELF_SIGNED_HOST) || line.contains(UNTRUSTED_ROOT_HOST) }
            }
    }

    private static String sscSessionLoginCommand() {
        return "ssc session login --url ${SELF_SIGNED_URL} --user dummy --password dummy --disable sc-sast,sc-dast"
    }

    private static String toolDefinitionsUpdateCommand() {
        return "tool definitions update --source ${UNTRUSTED_ROOT_URL}"
    }

    private static String actionRunCommand() {
        return "action run ${SELF_SIGNED_URL} --progress=none --on-unsigned=ignore --on-invalid-version=ignore"
    }

    private void addTrustedUrl(String url) {
        Fcli.run("config truststore add-trusted-url ${url}")
    }

    private void expectPkixFailure(String command) {
        def result = Fcli.run(command, { it.expectSuccess(false) })
        verifyAll(result.stderr) {
            it.any { line -> isPkixFailureLine(line) }
        }
    }

    private void expectNonPkixFailure(String command, List<String> nonPkixHints) {
        def result = Fcli.run(command, { it.expectSuccess(false) })
        verifyAll(result.stderr) {
            size() > 0
            !it.any { line -> isPkixFailureLine(line) }
            it.any { line -> nonPkixHints.any { hint -> line.contains(hint) } }
        }
    }

    private static boolean isPkixFailureLine(String line) {
        if ( line == null ) {
            return false
        }
        def normalized = line.toLowerCase()
        return normalized.contains("pkix")
            || normalized.contains("certificate validation failed")
            || normalized.contains("suncertpathbuilderexception")
    }
}