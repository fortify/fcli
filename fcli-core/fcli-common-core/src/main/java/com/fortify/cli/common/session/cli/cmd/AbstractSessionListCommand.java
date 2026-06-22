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
package com.fortify.cli.common.session.cli.cmd;

import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionValidatorMixin;
import com.fortify.cli.common.session.helper.ISessionDescriptor;

public abstract class AbstractSessionListCommand<D extends ISessionDescriptor> extends AbstractSessionCommand<D> {
    @Override
    public JsonNode getJsonNode() {
        var result = getSessionHelper().sessionSummariesAsArrayNode();
        var validator = getSessionValidatorMixin();
        if ( validator != null && validator.isValidationEnabled() ) {
            result.forEach(node -> {
                if ( node instanceof ObjectNode sessionNode ) {
                    var sessionName = sessionNode.path("name").asText(null);
                    if ( sessionName != null ) {
                        var descriptor = getSessionHelper().get(sessionName, false);
                        var locallyExpired = descriptor != null
                                && descriptor.getExpiryDate() != null
                                && descriptor.getExpiryDate().before(new Date());
                        if ( !locallyExpired || validator.shouldValidateExpiredSessions() ) {
                            if ( validator.updateSessionNode(sessionName, descriptor, sessionNode) && descriptor != null ) {
                                getSessionHelper().save(sessionName, descriptor);
                            }
                        }
                    }
                }
            });
        }
        return result;
    }

    /**
     * Override to return the product-specific validator mixin when {@code --validate}
     * should be supported. The default returns {@code null} (no validation).
     */
    protected AbstractSessionValidatorMixin<D> getSessionValidatorMixin() {
        return null;
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
