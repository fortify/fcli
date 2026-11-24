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

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;

/**
 * Obtains the SAML assertion for the FoD SSO flow, either by driving a Chrome/Edge browser through
 * the DevTools protocol (the user authenticates in the browser, after which FoD displays the
 * assertion on its assertion consumer service page), or by reading it from a file.
 */
public final class FoDSsoLoginHelper {
    private FoDSsoLoginHelper() {}

    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    private static final String ACS_PATH_PREFIX = "/SAML/AssertionConsumerService";

    /**
     * @param browserType "chrome" or "edge", or blank to auto-detect
     * @param browserPath Browser executable, or blank to locate the browser based on browserType
     * @param browserProfile Persistent browser profile directory, or blank to use a temporary profile
     */
    public static String acquireSamlAssertion(URI samlIdpUri, String browserType, String browserPath, String browserProfile, Duration timeout) {
        var ssoUri = prepareSsoUri(samlIdpUri);
        System.err.println("SSO URL: " + ssoUri);
        var browser = FoDSsoBrowserLocator.locate(browserType, browserPath);
        var profileDir = StringUtils.isBlank(browserProfile) ? null : Path.of(browserProfile.trim());
        System.err.println("Opening " + browser.getFileName() + " to complete SSO login (timeout: "
                + timeout.toSeconds() + "s). Close the browser window to cancel.");
        try ( var session = FoDSsoBrowser.launch(browser, profileDir, ssoUri) ) {
            var text = session.waitForAssertion(samlIdpUri, timeout);
            return normalizeAssertion(text, "the browser");
        }
    }

    /** FoD only displays the assertion (rather than posting it to the browser) if the saml=1 parameter is present. */
    static URI prepareSsoUri(URI samlIdpUri) {
        var query = samlIdpUri.getRawQuery();
        if ( query != null ) {
            var parameters = query.split("&", -1);
            var samlParameterCount = 0;
            var hasSingleValidSamlParameter = false;
            for ( var parameter : parameters ) {
                if ( isSamlParameter(parameter) ) {
                    samlParameterCount++;
                    hasSingleValidSamlParameter = parameter.equals("saml=1");
                }
            }
            if ( samlParameterCount == 1 && hasSingleValidSamlParameter ) {
                return samlIdpUri;
            }
            if ( samlParameterCount > 0 ) {
                query = java.util.Arrays.stream(parameters).filter(p -> !isSamlParameter(p)).collect(java.util.stream.Collectors.joining("&"));
            }
        }
        var uriWithoutFragment = samlIdpUri.toString();
        var fragmentIndex = uriWithoutFragment.indexOf('#');
        var fragment = fragmentIndex < 0 ? "" : uriWithoutFragment.substring(fragmentIndex);
        if ( fragmentIndex >= 0 ) {
            uriWithoutFragment = uriWithoutFragment.substring(0, fragmentIndex);
        }
        var queryIndex = uriWithoutFragment.indexOf('?');
        var baseUri = queryIndex < 0 ? uriWithoutFragment : uriWithoutFragment.substring(0, queryIndex);
        var updatedQuery = query == null || query.isEmpty() ? "saml=1" : query + "&saml=1";
        return URI.create(baseUri + "?" + updatedQuery + fragment);
    }

    private static boolean isSamlParameter(String parameter) {
        return parameter.equals("saml") || parameter.startsWith("saml=");
    }

    static boolean isAssertionPage(URI samlIdpUri, String pageUrl) {
        try {
            var uri = URI.create(pageUrl);
            return uri.getHost() != null && uri.getHost().equalsIgnoreCase(samlIdpUri.getHost())
                    && StringUtils.startsWithIgnoreCase(uri.getPath(), ACS_PATH_PREFIX);
        } catch ( IllegalArgumentException e ) {
            return false;
        }
    }

    /**
     * Read a SAML assertion previously copied from the FoD assertion consumer service page.
     * @param fileOrStdin Path of the file containing the assertion, or '-' to read from stdin
     */
    public static String readSamlAssertion(String fileOrStdin) {
        return normalizeAssertion(readContent(fileOrStdin), "-".equals(fileOrStdin) ? "stdin" : fileOrStdin);
    }

    static String normalizeAssertion(String content, String source) {
        var result = StringUtils.deleteWhitespace(content);
        if ( result.startsWith("<") ) {
            result = Base64.getEncoder().encodeToString(content.trim().getBytes(StandardCharsets.UTF_8));
        }
        FcliSimpleException.throwIf(result.isEmpty(), "No SAML assertion found in %s", source);
        return result;
    }

    private static String readContent(String fileOrStdin) {
        try {
            var bytes = "-".equals(fileOrStdin)
                    ? System.in.readNBytes(MAX_RESPONSE_BYTES + 1)
                    : readFileBytes(Path.of(fileOrStdin));
            FcliSimpleException.throwIf(bytes.length > MAX_RESPONSE_BYTES, "SAML assertion is too large");
            return new String(bytes, StandardCharsets.UTF_8);
        } catch ( IOException | InvalidPathException e ) {
            throw new FcliSimpleException("Error reading SAML assertion from " + fileOrStdin, e);
        }
    }

    private static byte[] readFileBytes(Path path) throws IOException {
        try ( var input = Files.newInputStream(path) ) {
            return input.readNBytes(MAX_RESPONSE_BYTES + 1);
        }
    }
}
