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
package com.fortify.cli.fod._common.scan.helper.compare.attribution;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 * URL normalization for matching DAST issue locations against site tree URLs. Issue locations
 * may be absolute or relative, may be percent-encoded or not, may include default ports, and may
 * have trailing request data separated by whitespace.
 */
public final class FoDAttributionUrls {
    private FoDAttributionUrls() {}

    /**
     * @param origin Lower-case 'scheme://host[:port]', without default port; null for relative URLs
     * @param path Decoded path including query string, without trailing slash (except for the root path)
     */
    public record NormalizedUrl(String origin, String path) {
        public String key() {
            return origin == null ? path : origin + path;
        }
    }

    /** @return Normalized URL, or null if the given URL is blank */
    public static NormalizedUrl normalize(String url) {
        var value = StringUtils.trimToEmpty(url);
        var whitespaceIdx = StringUtils.indexOfAny(value, ' ', '\t', '\r', '\n');
        if ( whitespaceIdx >= 0 ) { value = value.substring(0, whitespaceIdx); }
        if ( value.isEmpty() ) { return null; }
        value = percentDecode(value);
        String origin = null;
        var schemeIdx = value.indexOf("://");
        if ( schemeIdx > 0 ) {
            var scheme = value.substring(0, schemeIdx).toLowerCase(Locale.ROOT);
            var remainder = value.substring(schemeIdx + 3);
            var pathIdx = StringUtils.indexOfAny(remainder, '/', '?');
            var authority = (pathIdx < 0 ? remainder : remainder.substring(0, pathIdx)).toLowerCase(Locale.ROOT);
            origin = scheme + "://" + stripDefaultPort(scheme, authority);
            value = pathIdx < 0 ? "/" : remainder.substring(pathIdx);
        }
        return new NormalizedUrl(origin, normalizePath(value));
    }

    /** @return Normalized URL key, or the original value if it can't be normalized */
    public static String normalizeKey(String url) {
        var normalized = normalize(url);
        return normalized == null ? url : normalized.key();
    }

    private static String stripDefaultPort(String scheme, String authority) {
        if ( ("http".equals(scheme) && authority.endsWith(":80")) || ("https".equals(scheme) && authority.endsWith(":443")) ) {
            return StringUtils.substringBeforeLast(authority, ":");
        }
        return authority;
    }

    private static String normalizePath(String pathAndQuery) {
        var value = pathAndQuery.startsWith("/") ? pathAndQuery : "/" + pathAndQuery;
        var queryIdx = value.indexOf('?');
        var path = queryIdx < 0 ? value : value.substring(0, queryIdx);
        var query = queryIdx < 0 ? "" : value.substring(queryIdx);
        if ( path.length() > 1 && path.endsWith("/") ) { path = StringUtils.stripEnd(path, "/"); }
        return (path.isEmpty() ? "/" : path) + query;
    }

    // Decodes %XX sequences only; unlike URLDecoder, '+' is retained and malformed sequences are left as-is
    static String percentDecode(String value) {
        if ( value.indexOf('%') < 0 ) { return value; }
        var out = new ByteArrayOutputStream(value.length());
        for ( int i = 0; i < value.length(); i++ ) {
            var c = value.charAt(i);
            if ( c == '%' && i + 2 < value.length() && isHex(value.charAt(i + 1)) && isHex(value.charAt(i + 2)) ) {
                out.write(Integer.parseInt(value.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                var codePoint = value.codePointAt(i);
                var bytes = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8);
                out.write(bytes, 0, bytes.length);
                i += Character.charCount(codePoint) - 1;
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static boolean isHex(char c) {
        return Character.digit(c, 16) >= 0;
    }
}
