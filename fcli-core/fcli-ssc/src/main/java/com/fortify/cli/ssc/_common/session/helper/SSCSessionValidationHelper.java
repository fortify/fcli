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

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.access_control.helper.SSCTokenGetOrCreateResponse;
import com.fortify.cli.ssc.access_control.helper.SSCTokenGetOrCreateResponse.SSCTokenData;
import com.fortify.cli.ssc.access_control.helper.SSCTokenHelper;

/**
 * Provides SSC session-level status checking, separate from general token CRUD operations
 * in {@link SSCTokenHelper}.
 */
public class SSCSessionValidationHelper {
    public static record SessionStatus(boolean valid, SSCTokenData tokenData) {}

    /**
     * Checks whether the given token can authenticate against SSC.
     * <ul>
     *   <li>Returns {@code valid=true} with token data on success.</li>
     *   <li>Returns {@code valid=false} on explicit authentication failure (HTTP 401/403).</li>
     *   <li>Throws {@link FcliSimpleException} when SSC is unreachable or returns an unexpected error,
     *       so the caller can distinguish "unknown/unreachable" from "definitely invalid".</li>
     * </ul>
     */
    public static SessionStatus checkTokenStatus(IUrlConfig urlConfig, char[] token) {
        try ( var unirest = UnirestHelper.createUnirestInstance() ) {
            SSCTokenHelper.configureUnirest(unirest, urlConfig, token);
            try {
                var tokenData = unirest.post(SSCUrls.USER_SESSION_TOKEN_DATA)
                        .body(JsonHelper.getObjectMapper().createObjectNode())
                        .asObject(SSCTokenGetOrCreateResponse.class)
                        .getBody().getData();
                tokenData.setToken(token);
                return new SessionStatus(true, tokenData);
            } catch ( UnexpectedHttpResponseException e ) {
                if ( e.getStatus()==401 || e.getStatus()==403 ) {
                    return new SessionStatus(false, null);
                } else {
                    throw new FcliSimpleException("Unable to verify session status: SSC returned HTTP " + e.getStatus(), e);
                }
            }
        } catch ( FcliSimpleException e ) {
            throw e;
        } catch ( Exception e ) {
            throw new FcliSimpleException("Unable to connect to SSC to verify session status", e);
        }
    }
}
