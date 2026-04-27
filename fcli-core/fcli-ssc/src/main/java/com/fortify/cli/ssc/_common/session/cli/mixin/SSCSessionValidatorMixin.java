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
package com.fortify.cli.ssc._common.session.cli.mixin;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionValidatorMixin;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionDescriptor;
import com.fortify.cli.ssc._common.session.helper.SSCSessionValidationHelper;
import com.fortify.cli.ssc._common.session.helper.SSCSessionValidationHelper.SessionStatus;

public class SSCSessionValidatorMixin extends AbstractSessionValidatorMixin<SSCAndScanCentralSessionDescriptor> {
    private static final DateTimeFormatter EXPIRY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    @Override
    public boolean updateSessionNode(String sessionName, SSCAndScanCentralSessionDescriptor descriptor, ObjectNode sessionNode) {
        var tokenData = descriptor == null ? null : descriptor.getSscTokenData();
        var token = tokenData == null ? null : tokenData.getToken();
        if ( descriptor == null || token == null ) {
            applySessionStatus(sessionNode, new SessionStatus(false, null));
            return false;
        }
        try {
            var status = SSCSessionValidationHelper.checkTokenStatus(descriptor.getSscUrlConfig(), token);
            applySessionStatus(sessionNode, status);
            if ( status.valid() && status.tokenData() != null && status.tokenData().getTerminalDate() != null ) {
                descriptor.setSscTokenData(status.tokenData());
                return true; // descriptor mutated; signal save
            }
        } catch ( FcliSimpleException e ) {
            sessionNode.put("expired", "Unknown");
            sessionNode.put("expires", "Unknown");
        }
        return false;
    }

    private void applySessionStatus(ObjectNode sessionNode, SessionStatus status) {
        sessionNode.put("expired", status.valid() ? "No" : "Yes");
        if ( !status.valid() ) {
            sessionNode.put("expires", "N/A");
        } else {
            var terminalDate = status.tokenData() == null ? null : status.tokenData().getTerminalDate();
            sessionNode.put("expires", terminalDate == null ? "Unknown" : formatExpiryDate(terminalDate));
        }
    }

    private static String formatExpiryDate(Date date) {
        return EXPIRY_DATE_FORMATTER.format(date.toInstant().atZone(ZoneId.systemDefault()));
    }
}
