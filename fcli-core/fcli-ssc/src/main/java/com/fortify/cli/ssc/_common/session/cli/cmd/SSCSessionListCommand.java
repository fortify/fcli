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
package com.fortify.cli.ssc._common.session.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionListCommand;
import com.fortify.cli.common.session.cli.mixin.ValidateSessionOptionMixin;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionDescriptor;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionHelper;
import com.fortify.cli.ssc.access_control.helper.SSCTokenHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME, sortOptions = false)
public class SSCSessionListCommand extends AbstractSessionListCommand<SSCAndScanCentralSessionDescriptor> {
    @Mixin @Getter private OutputHelperMixins.List outputHelper;
    @Mixin private ValidateSessionOptionMixin validateSessionOption;
    @Getter private SSCAndScanCentralSessionHelper sessionHelper = SSCAndScanCentralSessionHelper.instance();

    @Override
    public JsonNode getJsonNode() {
        var result = (ArrayNode)super.getJsonNode();
        validateSessionOption.validateIfNeeded(result, this::validateSession);
        return result;
    }

    private void validateSession(JsonNode sessionNode) {
        if ( sessionNode instanceof ObjectNode session ) {
            var sessionName = session.path("name").asText(null);
            var descriptor = sessionName==null ? null : getSessionHelper().get(sessionName, false);
            var tokenData = descriptor==null ? null : descriptor.getSscTokenData();
            var token = tokenData==null ? null : tokenData.getToken();
            var validationResult = descriptor==null || token==null
                    ? new SSCTokenHelper.SessionValidationResult(false, null)
                    : SSCTokenHelper.validateSession(descriptor.getSscUrlConfig(), token);
            updateSessionNode(session, validationResult);
            refreshSessionExpiry(sessionName, descriptor, validationResult);
        }
    }

    private void updateSessionNode(ObjectNode session, SSCTokenHelper.SessionValidationResult validationResult) {
        session.put("expired", validationResult.valid() ? "No" : "Yes");
        if ( !validationResult.valid() ) {
            session.put("expires", "N/A");
        } else {
            var terminalDate = validationResult.tokenData()==null ? null : validationResult.tokenData().getTerminalDate();
            if ( terminalDate!=null ) {
                session.put("expires", SSCTokenHelper.formatExpiryDate(terminalDate));
            }
        }
    }

    private void refreshSessionExpiry(String sessionName, SSCAndScanCentralSessionDescriptor descriptor, SSCTokenHelper.SessionValidationResult validationResult) {
        if ( sessionName!=null && descriptor!=null && validationResult.valid()
                && validationResult.tokenData()!=null && validationResult.tokenData().getTerminalDate()!=null ) {
            descriptor.setSscTokenData(validationResult.tokenData());
            getSessionHelper().save(sessionName, descriptor);
        }
    }
}
