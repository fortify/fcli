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
package com.fortify.cli.common.rest.unirest;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.log.LogMaskHelper;
import com.fortify.cli.common.log.LogMaskSource;
import com.fortify.cli.common.log.LogSensitivityLevel;

public final class RemoteUrlAuthHelper {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteUrlAuthHelper.class);
    /**
     * Pattern for masking URL userinfo auth values through {@link com.fortify.cli.common.log.MaskValue}.
     * Captures password for basic auth URLs and token/header value payload for bearer/header(s) formats.
     */
    public static final String URL_USERINFO_AUTH_VALUE_MASK_PATTERN = "https?://(?:(?:[^:@/]+:|bearer:|headers?:)([^@]*)@)?.*";
    /**
     * To be used as {@link com.fortify.cli.common.log.MaskValue#maskFullValueOnNoMatch()} together with
     * {@link #URL_USERINFO_AUTH_VALUE_MASK_PATTERN}: when the value does not match the URL pattern (e.g.,
     * a plain action name), no masking should occur because the value carries no embedded auth credentials.
     */
    public static final boolean URL_USERINFO_AUTH_VALUE_MASK_FULL_ON_NO_MATCH = false;

    private static final String PREFIX_BEARER = "bearer:";
    private static final String PREFIX_HEADER = "header:";
    private static final String PREFIX_HEADERS = "headers:";

    private RemoteUrlAuthHelper() {}

    public static ParsedRemoteUrl parse(String source) throws MalformedURLException {
        var url = new URL(source);
        var protocol = url.getProtocol();
        if ( !"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol) ) {
            return new ParsedRemoteUrl(source, Collections.emptyMap());
        }

        try {
            var uri = url.toURI();
            var headers = parseHeaders(uri.getRawUserInfo());
            return new ParsedRemoteUrl(removeUserInfo(uri), headers);
        } catch ( URISyntaxException e ) {
            throw new FcliSimpleException("Invalid URL: "+source, e);
        }
    }

    public static InputStream openStream(String source) throws IOException {
        var parsed = parse(source);
        var url = new URL(parsed.getRequestUrl());
        if ( parsed.getHeaders().isEmpty() || !isHttpProtocol(url.getProtocol()) ) {
            return url.openStream();
        }

        LOG.debug("Opening URL: {}", parsed.getRequestUrl());
        LogMaskHelper.INSTANCE.registerValue(LogSensitivityLevel.high, LogMaskSource.HTTP_AUTH_HEADER, "REQUEST HEADER", parsed.getHeaders().values(), "");
        LOG.debug("Request headers: {}", parsed.getHeaders());

        var connection = (HttpURLConnection)url.openConnection();
        parsed.getHeaders().forEach(connection::setRequestProperty);
        LOG.debug("Response status: {} {}", connection.getResponseCode(), connection.getResponseMessage());
        return connection.getInputStream();
    }

    private static boolean isHttpProtocol(String protocol) {
        return "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol);
    }

    private static String removeUserInfo(URI uri) throws URISyntaxException {
        var rawAuthority = uri.getRawAuthority();
        if ( rawAuthority!=null ) {
            var atSignIndex = rawAuthority.lastIndexOf('@');
            if ( atSignIndex >= 0 ) {
                rawAuthority = rawAuthority.substring(atSignIndex + 1);
            }
        }
        // Build the URI string from raw (already-encoded) components to avoid double-encoding.
        // The multi-arg URI constructors treat their arguments as decoded and would re-encode
        // any percent-encoded sequences (e.g. %2B → %252B), breaking URLs that already contain
        // percent-encoded characters such as Adoptium JRE download URLs (jdk-17.0.9%2B9/...).
        var sb = new StringBuilder();
        if ( uri.getScheme() != null ) { sb.append(uri.getScheme()).append(':'); }
        if ( rawAuthority != null ) { sb.append("//").append(rawAuthority); }
        if ( uri.getRawPath() != null ) { sb.append(uri.getRawPath()); }
        if ( uri.getRawQuery() != null ) { sb.append('?').append(uri.getRawQuery()); }
        if ( uri.getRawFragment() != null ) { sb.append('#').append(uri.getRawFragment()); }
        return sb.toString();
    }

    private static Map<String, String> parseHeaders(String rawUserInfo) {
        if ( StringUtils.isBlank(rawUserInfo) ) { return Collections.emptyMap(); }
        if ( rawUserInfo.startsWith(PREFIX_BEARER) ) {
            return Map.of("Authorization", "Bearer "+decode(rawUserInfo.substring(PREFIX_BEARER.length())));
        }
        if ( rawUserInfo.startsWith(PREFIX_HEADER) ) {
            return parseHeaderAssignments(rawUserInfo.substring(PREFIX_HEADER.length()));
        }
        if ( rawUserInfo.startsWith(PREFIX_HEADERS) ) {
            return parseHeaderAssignments(rawUserInfo.substring(PREFIX_HEADERS.length()));
        }
        return parseBasicAuth(rawUserInfo);
    }

    private static Map<String, String> parseBasicAuth(String rawUserInfo) {
        var separatorIndex = rawUserInfo.indexOf(':');
        String username;
        String password;
        if ( separatorIndex < 0 ) {
            username = decode(rawUserInfo);
            password = "";
        } else {
            username = decode(rawUserInfo.substring(0, separatorIndex));
            password = decode(rawUserInfo.substring(separatorIndex + 1));
        }
        var value = Base64.getEncoder().encodeToString((username+":"+password).getBytes(StandardCharsets.UTF_8));
        return Map.of("Authorization", "Basic "+value);
    }

    private static Map<String, String> parseHeaderAssignments(String assignments) {
        if ( StringUtils.isBlank(assignments) ) {
            throw new FcliSimpleException("No headers specified in URL userinfo");
        }
        var result = new LinkedHashMap<String, String>();
        for ( var assignment : assignments.split("&") ) {
            if ( StringUtils.isBlank(assignment) ) { continue; }
            var separatorIndex = assignment.indexOf('=');
            if ( separatorIndex <= 0 ) {
                throw new FcliSimpleException("Invalid header assignment in URL userinfo: "+assignment);
            }
            var name = decode(assignment.substring(0, separatorIndex));
            var value = decode(assignment.substring(separatorIndex + 1));
            if ( StringUtils.isBlank(name) ) {
                throw new FcliSimpleException("Header name must not be blank in URL userinfo");
            }
            result.put(name, value);
        }
        if ( result.isEmpty() ) {
            throw new FcliSimpleException("No valid headers specified in URL userinfo");
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * URLDecoder treats '+' as a space; preserve literal '+' characters in userinfo.
     */
    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    public static final class ParsedRemoteUrl {
        private final String requestUrl;
        private final Map<String, String> headers;

        private ParsedRemoteUrl(String requestUrl, Map<String, String> headers) {
            this.requestUrl = requestUrl;
            this.headers = headers;
        }

        public String getRequestUrl() {
            return requestUrl;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }
}
