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
package com.fortify.cli.ai_assist.mcp.helper.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;

import io.modelcontextprotocol.common.McpTransportContext;
import lombok.RequiredArgsConstructor;

/**
 * Parses the X-AUTH-SSC or X-AUTH-FOD HTTP header from an incoming MCP request into a
 * {@link ParsedAuthorization} record.
 *
 * <p>The header value is a semicolon-separated list of {@code key=value} pairs.
 * Backslash-escaping is supported for {@code \}, {@code ;} and {@code =}.</p>
 */
@RequiredArgsConstructor
public final class MCPServerHttpAuthHeaderParser {
    private final MCPServerHttpConfig config;

    public ParsedAuthorization parse(McpTransportContext transportContext) {
        var product = config.getProduct();
        var headerName = getAuthHeaderName(product);
        var headerValue = getRequiredHeader(transportContext, headerName);
        var keyValues = parseAuthHeaderKeyValues(headerValue, headerName);
        return switch ( product ) {
        case ssc -> parseSscAuthorization(keyValues);
        case fod -> parseFoDAuthorization(keyValues);
        };
    }

    /**
     * Parses the auth header and immediately registers all credential values for log masking.
     * Must be called after a {@link com.fortify.cli.common.cli.util.FcliExecutionContext} has
     * been pushed so that the credentials are registered into the per-request
     * {@link com.fortify.cli.common.log.LogMaskContext}.
     */
    public ParsedAuthorization parseAndRegister(McpTransportContext transportContext) {
        var auth = parse(transportContext);
        auth.registerCredentials();
        return auth;
    }

    private String getAuthHeaderName(MCPServerHttpConfig.Product product) {
        return switch ( product ) {
        case ssc -> MCPServerHttpSessionDescriptorResolver.HEADER_AUTH_SSC;
        case fod -> MCPServerHttpSessionDescriptorResolver.HEADER_AUTH_FOD;
        };
    }

    private ParsedAuthorization parseSscAuthorization(Map<String, String> keyValues) {
        var token = keyValues.get(MCPServerHttpSessionDescriptorResolver.SSC_TOKEN_KEY);
        if ( StringUtils.isBlank(token) ) {
            throw new FcliSimpleException("%s header requires key '%s'",
                    MCPServerHttpSessionDescriptorResolver.HEADER_AUTH_SSC,
                    MCPServerHttpSessionDescriptorResolver.SSC_TOKEN_KEY);
        }
        return new ParsedAuthorization(
                MCPServerHttpConfig.Product.ssc,
                token,
                keyValues.get(MCPServerHttpSessionDescriptorResolver.SSC_SC_SAST_CLIENT_AUTH_TOKEN_KEY),
                null, null, null, null, null);
    }

    private ParsedAuthorization parseFoDAuthorization(Map<String, String> keyValues) {
        return new ParsedAuthorization(
                MCPServerHttpConfig.Product.fod,
                null,
                null,
                keyValues.get(MCPServerHttpSessionDescriptorResolver.FOD_CLIENT_ID_KEY),
                keyValues.get(MCPServerHttpSessionDescriptorResolver.FOD_CLIENT_SECRET_KEY),
                keyValues.get(MCPServerHttpSessionDescriptorResolver.FOD_TENANT_KEY),
                keyValues.get(MCPServerHttpSessionDescriptorResolver.FOD_USER_KEY),
                keyValues.get(MCPServerHttpSessionDescriptorResolver.FOD_PAT_KEY));
    }

    private Map<String, String> parseAuthHeaderKeyValues(String valuePart, String headerName) {
        var result = new LinkedHashMap<String, String>();
        for ( var segment : splitEscapedSegments(valuePart, headerName) ) {
            var trimmedSegment = StringUtils.trimToNull(segment);
            if ( trimmedSegment == null ) {
                continue;
            }
            var separatorIndex = findUnescapedSeparator(trimmedSegment, '=');
            if ( separatorIndex <= 0 || separatorIndex == trimmedSegment.length() - 1 ) {
                throw new FcliSimpleException("Invalid %s header segment '%s'; expected key=value", headerName, trimmedSegment);
            }
            var key = StringUtils.trimToNull(unescapeHeaderValue(trimmedSegment.substring(0, separatorIndex), headerName));
            var value = StringUtils.trimToNull(unescapeHeaderValue(trimmedSegment.substring(separatorIndex + 1), headerName));
            if ( key == null || value == null ) {
                throw new FcliSimpleException("Invalid %s header segment '%s'; expected key=value", headerName, trimmedSegment);
            }
            var normalizedKey = key.toLowerCase(Locale.ROOT);
            if ( result.containsKey(normalizedKey) ) {
                throw new FcliSimpleException("Duplicate %s header key: %s", headerName, key);
            }
            result.put(normalizedKey, value);
        }
        if ( result.isEmpty() ) {
            throw new FcliSimpleException("%s header doesn't contain any key/value entries", headerName);
        }
        return result;
    }

    private List<String> splitEscapedSegments(String valuePart, String headerName) {
        var result = new ArrayList<String>();
        var current = new StringBuilder();
        var escaping = false;
        for ( var i = 0; i < valuePart.length(); i++ ) {
            var c = valuePart.charAt(i);
            if ( escaping ) {
                validateEscapeCharacter(c, headerName);
                current.append('\\').append(c);
                escaping = false;
            } else if ( c == '\\' ) {
                escaping = true;
            } else if ( c == ';' ) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if ( escaping ) {
            throw new FcliSimpleException("Invalid %s header value; trailing escape character", headerName);
        }
        result.add(current.toString());
        return result;
    }

    private int findUnescapedSeparator(String value, char separator) {
        var escaping = false;
        for ( var i = 0; i < value.length(); i++ ) {
            var c = value.charAt(i);
            if ( escaping ) {
                escaping = false;
            } else if ( c == '\\' ) {
                escaping = true;
            } else if ( c == separator ) {
                return i;
            }
        }
        return -1;
    }

    private String unescapeHeaderValue(String value, String headerName) {
        var result = new StringBuilder();
        var escaping = false;
        for ( var i = 0; i < value.length(); i++ ) {
            var c = value.charAt(i);
            if ( escaping ) {
                validateEscapeCharacter(c, headerName);
                result.append(c);
                escaping = false;
            } else if ( c == '\\' ) {
                escaping = true;
            } else {
                result.append(c);
            }
        }
        if ( escaping ) {
            throw new FcliSimpleException("Invalid %s header value; trailing escape character", headerName);
        }
        return result.toString();
    }

    private void validateEscapeCharacter(char c, String headerName) {
        if ( c != '\\' && c != ';' && c != '=' ) {
            throw new FcliSimpleException("Invalid %s header escape sequence '\\%s'; supported escapes are \\\\, \\; and \\=", headerName, c);
        }
    }

    @SuppressWarnings("unchecked")
    private String getOptionalHeader(McpTransportContext transportContext, String headerName) {
        var headers = (Map<String, List<String>>) transportContext.get("headers");
        if ( headers == null || headers.isEmpty() ) { return null; }
        return headers.entrySet().stream()
                .filter(entry -> headerName.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(values -> values != null && !values.isEmpty())
                .map(values -> values.get(0))
                .map(StringUtils::trimToNull)
                .findFirst().orElse(null);
    }

    private String getRequiredHeader(McpTransportContext transportContext, String headerName) {
        var value = getOptionalHeader(transportContext, headerName);
        if ( StringUtils.isBlank(value) ) {
            throw new FcliSimpleException("Missing required HTTP header: %s", headerName);
        }
        return value;
    }
}
