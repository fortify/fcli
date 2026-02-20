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
package com.fortify.cli.ssc._common.session.helper;

import com.fortify.cli.common.rest.unirest.HttpMcpAuthContext;
import com.fortify.cli.common.rest.unirest.config.UrlConfig;
import com.fortify.cli.common.session.helper.AbstractSessionHelper;
import com.fortify.cli.ssc.access_control.helper.SSCTokenGetOrCreateResponse.SSCTokenData;

public class SSCAndScanCentralSessionHelper extends AbstractSessionHelper<SSCAndScanCentralSessionDescriptor> {
    private static final SSCAndScanCentralSessionHelper INSTANCE = new SSCAndScanCentralSessionHelper();

    private SSCAndScanCentralSessionHelper() {}

    @Override
    public String getType() {
        return "SSC";
    }

    @Override
    protected String getLoginCmd() {
        return "fcli ssc session login";
    }

    @Override
    protected Class<SSCAndScanCentralSessionDescriptor> getSessionDescriptorType() {
        return SSCAndScanCentralSessionDescriptor.class;
    }

    public static final SSCAndScanCentralSessionHelper instance() {
        return INSTANCE;
    }

    /**
     * If running inside an HTTP MCP server (HttpMcpAuthContext is set), return
     * a synthetic in-memory session descriptor built from the request's bearer
     * token and the server-configured SSC URL.  Otherwise fall back to the
     * normal persisted session lookup.
     */
    public static SSCAndScanCentralSessionDescriptor getOrSynthetic(String sessionName, boolean failIfUnavailable) {
        var authInfo = HttpMcpAuthContext.get();
        if (authInfo != null) {
            var tokenData = new SSCTokenData();
            tokenData.setToken(authInfo.token());
            // terminalDate left null → treated as "active" by hasActiveCachedTokenResponse()
            return SSCAndScanCentralSessionDescriptor.builder()
                    .sscUrlConfig(UrlConfig.builder().url(authInfo.sscUrl()).build())
                    .sscTokenData(tokenData)
                    .scSastDisabledReason("Not available in HTTP MCP mode")
                    .scDastDisabledReason("Not available in HTTP MCP mode")
                    .build();
        }
        return instance().get(sessionName, failIfUnavailable);
    }
}
