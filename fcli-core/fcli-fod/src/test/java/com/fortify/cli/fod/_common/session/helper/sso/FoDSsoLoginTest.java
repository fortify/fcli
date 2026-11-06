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
package com.fortify.cli.fod._common.session.helper.sso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.fod._common.session.cli.mixin.FoDSessionLoginSsoOptions;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

class FoDSsoLoginTest {
    private static final URI IDP = URI.create("https://fodtenant.qa7/SSO/Login/abc?saml=1");

    @Test
    void shouldAddSamlParameterIfMissing() {
        assertEquals(URI.create("https://fod.example.com/SSO/Login/abc?saml=1"),
                FoDSsoLoginHelper.prepareSsoUri(URI.create("https://fod.example.com/SSO/Login/abc")));
        assertEquals(URI.create("https://fod.example.com/IdPRedirect?token=x&saml=1"),
                FoDSsoLoginHelper.prepareSsoUri(URI.create("https://fod.example.com/IdPRedirect?token=x")));
    }

    @Test
    void shouldKeepExistingSamlParameter() {
        assertEquals(IDP, FoDSsoLoginHelper.prepareSsoUri(IDP));
    }

    @Test
    void shouldReplaceInvalidOrDuplicateSamlParametersAndPreserveQueryAndFragment() {
        assertEquals(URI.create("https://fod.example.com/SSO/Login/abc?token=a%2Fb&saml=1#return"),
                FoDSsoLoginHelper.prepareSsoUri(URI.create("https://fod.example.com/SSO/Login/abc?token=a%2Fb&saml=0#return")));
        assertEquals(URI.create("https://fod.example.com/SSO/Login/abc?token=x&saml=1#return"),
                FoDSsoLoginHelper.prepareSsoUri(URI.create("https://fod.example.com/SSO/Login/abc?token=x&saml=1&saml#return")));
    }
    @Test
    void shouldPrintPreparedSsoUriBeforeBrowserLookup() {
        var originalErr = System.err;
        var errorOutput = new ByteArrayOutputStream();
        try ( var capturedErr = new PrintStream(errorOutput, true, StandardCharsets.UTF_8) ) {
            System.setErr(capturedErr);
            assertThrows(FcliSimpleException.class, () -> FoDSsoLoginHelper.acquireSamlAssertion(
                    URI.create("https://fod.example.com/SSO/Login/abc?token=x"), null, "does-not-exist-browser", null,
                    Duration.ofSeconds(1)));
        } finally {
            System.setErr(originalErr);
        }
        assertTrue(errorOutput.toString(StandardCharsets.UTF_8)
                .contains("SSO URL: https://fod.example.com/SSO/Login/abc?token=x&saml=1"));
    }


    @Test
    void shouldDetectAssertionPage() {
        assertTrue(FoDSsoLoginHelper.isAssertionPage(IDP, "https://fodtenant.qa7/SAML/AssertionConsumerService"));
        assertTrue(FoDSsoLoginHelper.isAssertionPage(IDP, "https://FODTENANT.qa7/saml/assertionconsumerservice?x=1"));
    }

    @Test
    void shouldNotDetectOtherPagesAsAssertionPage() {
        assertFalse(FoDSsoLoginHelper.isAssertionPage(IDP, "https://other.example.com/SAML/AssertionConsumerService"));
        assertFalse(FoDSsoLoginHelper.isAssertionPage(IDP, "https://fodtenant.qa7/SSO/Login/abc"));
        assertFalse(FoDSsoLoginHelper.isAssertionPage(IDP, "https://integrator.okta.com/app/x/sso/saml"));
        assertFalse(FoDSsoLoginHelper.isAssertionPage(IDP, "not a url"));
        assertFalse(FoDSsoLoginHelper.isAssertionPage(IDP, "about:blank"));
    }

    @Test
    void shouldReadSamlAssertionFromFileAndStripWhitespace() throws Exception {
        var file = Files.createTempFile("saml", ".txt");
        try {
            Files.writeString(file, "PD94\nbWwg\r\n  abc= \n");
            assertEquals("PD94bWwgabc=", FoDSsoLoginHelper.readSamlAssertion(file.toString()));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void shouldRejectOversizedSamlAssertionFile() throws Exception {
        var file = Files.createTempFile("saml-oversized", ".txt");
        try {
            Files.write(file, new byte[2 * 1024 * 1024 + 1]);
            var exception = assertThrows(FcliSimpleException.class, () -> FoDSsoLoginHelper.readSamlAssertion(file.toString()));
            assertEquals("SAML assertion is too large", exception.getMessage());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void shouldBase64EncodeRawXmlSamlAssertion() {
        assertEquals(Base64.getEncoder().encodeToString("<samlp:Response/>".getBytes(StandardCharsets.UTF_8)),
                FoDSsoLoginHelper.normalizeAssertion("<samlp:Response/>\n", "test"));
    }

    @Test
    void shouldFailOnEmptyAssertion() {
        assertThrows(FcliSimpleException.class, () -> FoDSsoLoginHelper.normalizeAssertion(" \n ", "test"));
    }

    @Test
    void shouldFailOnMissingSamlAssertionFile() {
        assertThrows(FcliSimpleException.class, () -> FoDSsoLoginHelper.readSamlAssertion("does-not-exist.txt"));
    }

    @Test
    void shouldListWindowsBrowserCandidates() {
        var env = Map.of("ProgramFiles", "C:\\PF", "ProgramFiles(x86)", "C:\\PF86");
        var candidates = FoDSsoBrowserLocator.candidates(true, false, env::get);
        assertTrue(candidates.contains(Path.of("C:\\PF", "Google", "Chrome", "Application", "chrome.exe")));
        assertTrue(candidates.contains(Path.of("C:\\PF86", "Microsoft", "Edge", "Application", "msedge.exe")));
        assertEquals(4, candidates.size());
    }

    @Test
    void shouldListLinuxBrowserCandidatesFromPath() {
        var env = Map.of("PATH", "/usr/bin" + File.pathSeparator + "/opt/bin");
        var candidates = FoDSsoBrowserLocator.candidates(false, false, env::get);
        assertTrue(candidates.contains(Path.of("/usr/bin", "google-chrome")));
        assertTrue(candidates.contains(Path.of("/opt/bin", "microsoft-edge")));
    }

    @Test
    void shouldListMacBrowserCandidatesInPriorityOrder() {
        var candidates = FoDSsoBrowserLocator.candidates(false, true, ignored -> null);
        assertEquals(List.of(
                Path.of("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"),
                Path.of("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"),
                Path.of("/Applications/Chromium.app/Contents/MacOS/Chromium")), candidates);
        assertEquals(List.of(Path.of("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge")),
                FoDSsoBrowserLocator.candidates(false, true, ignored -> null, false, true));
    }

    @Test
    void shouldLimitCandidatesToSelectedBrowser() {
        var env = Map.of("ProgramFiles", "C:\\PF", "ProgramFiles(x86)", "C:\\PF86");
        var edgeOnly = FoDSsoBrowserLocator.candidates(true, false, env::get, false, true);
        assertEquals(2, edgeOnly.size());
        assertTrue(edgeOnly.stream().allMatch(p -> p.endsWith("msedge.exe")));
        var chromeOnly = FoDSsoBrowserLocator.candidates(true, false, env::get, true, false);
        assertTrue(chromeOnly.stream().allMatch(p -> p.endsWith("chrome.exe")));
    }

    @Test
    void shouldFailOnInvalidExplicitBrowserPath() {
        assertThrows(FcliSimpleException.class, () -> FoDSsoBrowserLocator.locate(null, "does-not-exist-browser"));
    }

    @Test
    void shouldFailOnInvalidBrowserType() {
        assertThrows(FcliSimpleException.class, () -> FoDSsoBrowserLocator.locate("firefox", null));
    }

    @Test
    void shouldDeriveApiUrlFromSsoUrl() {
        var options = parse("--sso-url", "https://ams.fortify.com:8443/SSO/Login/abc?saml=1");
        assertEquals("https://api.ams.fortify.com:8443", options.getUrlConfig().getUrl());
    }

    @Test
    void shouldUseExplicitUrlAsIs() {
        var options = parse("--sso-url", "https://fodtenant.qa7/SSO/Login/abc?saml=1", "--url", "https://10.0.0.1:4443/");
        assertEquals("https://10.0.0.1:4443", options.getUrlConfig().getUrl());
    }

    @Test
    void shouldParseBrowserOptions() {
        var options = parse("--sso-url", "https://fodtenant.qa7/SSO/Login/abc",
                "--browser-path", "C:\\chrome.exe", "--browser-profile", "C:\\profile", "--saml-assertion-file", "x.txt");
        assertEquals("C:\\chrome.exe", options.getBrowserPath());
        assertEquals("edge", parse("--sso-url", "https://fodtenant.qa7/SSO/Login/abc", "--browser", "edge").getBrowser());
        assertEquals("C:\\profile", options.getBrowserProfile());
        assertTrue(options.hasSamlAssertionFile());
        assertNull(parse("--sso-url", "https://fodtenant.qa7/SSO/Login/abc").getBrowserProfile());
    }

    @Test
    void shouldRejectInvalidSsoUrl() {
        var options = parse("--sso-url", "not a url");
        assertThrows(FcliSimpleException.class, options::getSsoUri);
    }

    private static FoDSessionLoginSsoOptions parse(String... args) {
        var cmd = new TestCommand();
        new CommandLine(cmd).parseArgs(args);
        return cmd.options;
    }

    @Command(name = "test")
    static final class TestCommand {
        @Mixin private FoDSessionLoginSsoOptions options;
    }
}
