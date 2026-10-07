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
package com.fortify.cli.aviator._common.session.user.helper;

import com.fortify.cli.aviator._common.util.AviatorJwtUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.session.helper.AbstractSessionHelper;
import com.fortify.grpc.token.TokenValidationResponse;

public class AviatorUserSessionHelper extends AbstractSessionHelper<AviatorUserSessionDescriptor> {
    private static final AviatorUserSessionHelper INSTANCE = new AviatorUserSessionHelper();
    
    private AviatorUserSessionHelper() {}
    
    @Override
    public String getType() {
        return "aviator-user";
    }
    
    @Override
    protected String getLoginCmd() {
        return "fcli aviator session login";
    }

    @Override
    protected Class<AviatorUserSessionDescriptor> getSessionDescriptorType() {
        return AviatorUserSessionDescriptor.class;
    }

    public AviatorUserTokenValidationResult validateToken(AviatorUserSessionDescriptor sessionDescriptor) {
        return validateToken(sessionDescriptor.getAviatorUrl(), sessionDescriptor.getAviatorToken());
    }

    public AviatorUserTokenValidationResult validateToken(String aviatorUrl, String token) {
        var tenantName = AviatorJwtUtils.extractTenantNameFromToken(token);
        try (var client = AviatorGrpcClientHelper.createClient(aviatorUrl)) {
            var response = client.validateUserToken(token, tenantName);
            return new AviatorUserTokenValidationResult(tenantName, response);
        }
    }
    
    public static final AviatorUserSessionHelper instance() {
        return INSTANCE;
    }

    public record AviatorUserTokenValidationResult(String tenantName, TokenValidationResponse response) {}
}
