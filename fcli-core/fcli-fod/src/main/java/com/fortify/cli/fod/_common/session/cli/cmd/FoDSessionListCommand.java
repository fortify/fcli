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
package com.fortify.cli.fod._common.session.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionListCommand;
import com.fortify.cli.common.session.cli.mixin.ValidateSessionOptionMixin;
import com.fortify.cli.fod._common.session.helper.FoDSessionDescriptor;
import com.fortify.cli.fod._common.session.helper.FoDSessionHelper;
import com.fortify.cli.fod._common.session.helper.oauth.FoDOAuthHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME, sortOptions = false)
public class FoDSessionListCommand extends AbstractSessionListCommand<FoDSessionDescriptor> {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Mixin private ValidateSessionOptionMixin validateSessionOption;
    @Getter private FoDSessionHelper sessionHelper = FoDSessionHelper.instance();

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
            var tokenResponse = descriptor==null ? null : descriptor.getCachedTokenResponse();
            var accessToken = tokenResponse==null ? null : tokenResponse.getAccessToken();
            var isValid = accessToken!=null && FoDOAuthHelper.validateToken(descriptor.getUrlConfig(), accessToken);
            session.put("expired", isValid ? "No" : "Yes");
            if ( !isValid ) {
                session.put("expires", "N/A");
            }
        }
    }
}
