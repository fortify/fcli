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
package com.fortify.cli.fod._common.session.cli.mixin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionValidatorMixin;
import com.fortify.cli.fod._common.session.helper.FoDSessionDescriptor;
import com.fortify.cli.fod._common.session.helper.FoDSessionValidationHelper;
import com.fortify.cli.fod._common.session.helper.FoDSessionValidationHelper.SessionStatus;

public class FoDSessionValidatorMixin extends AbstractSessionValidatorMixin<FoDSessionDescriptor> {
    @Override
    public boolean shouldValidateExpiredSessions() {
        return false;
    }

    @Override
    public boolean updateSessionNode(String sessionName, FoDSessionDescriptor descriptor, ObjectNode sessionNode) {
        var tokenResponse = descriptor == null ? null : descriptor.getCachedTokenResponse();
        var accessToken = tokenResponse == null ? null : tokenResponse.getAccessToken();
        if ( descriptor == null || accessToken == null ) {
            applySessionStatus(sessionNode, new SessionStatus(false, null));
            return false;
        }
        try {
            var status = FoDSessionValidationHelper.checkTokenStatus(descriptor.getUrlConfig(), accessToken);
            applySessionStatus(sessionNode, status);
        } catch ( FcliSimpleException e ) {
            sessionNode.put("expired", "Unknown");
            sessionNode.put("expires", "Unknown");
        }
        return false; // FoD descriptor is never mutated
    }

    private void applySessionStatus(ObjectNode sessionNode, SessionStatus status) {
        sessionNode.put("expired", status.valid() ? "No" : "Yes");
        if ( !status.valid() ) {
            sessionNode.put("expires", "N/A");
        } else {
            sessionNode.put("expires", sessionNode.has("expires") ? sessionNode.get("expires").asText() : "Unknown");
        }
    }
}
