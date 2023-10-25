/**
 * Copyright 2023 Open Text.
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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fortify.cli.common.util.StringUtils;

import lombok.SneakyThrows;

/**
 *
 * @author Ruud Senden
 */
public class URIHelper {
    @SneakyThrows
    public static final String addOrReplaceParam(String uriString, String param, Object newValue) {
        return addOrReplaceParam(new URI(uriString), param, newValue).toString();
    }

    @SneakyThrows
    public static final URI addOrReplaceParam(URI uri, String param, Object newValue) {
        // Match and remove param, either:
        // - At the start of the query string, also removing the trailing & if present
        // - After an &, also removing the leading &
        // Note that this only works for simple parameter names, not parameter names that
        // contain characters that have a special meaning in regex. For example, if param
        // contains a dot like in a.c=value, we'd also match and remove abc=value.
        var pattern = String.format("^%s=[^&]+&?|&%s=[^&]", param, param);
        var query = uri.getQuery();
        if (StringUtils.isNotBlank(query)) { query = query.replaceAll(pattern, ""); }
        var newParamAndValue = String.format("%s=%s", param, URLEncoder.encode(newValue.toString(), StandardCharsets.UTF_8));
        query = (StringUtils.isBlank(query) ? "" : query+"&") + newParamAndValue;
        return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                uri.getPath(), query, uri.getFragment());
    }

}
