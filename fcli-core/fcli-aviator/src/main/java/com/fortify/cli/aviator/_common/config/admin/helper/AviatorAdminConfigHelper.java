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
package com.fortify.cli.aviator._common.config.admin.helper;

import com.fortify.cli.aviator._common.util.AviatorSignatureUtils;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.common.session.helper.AbstractSessionHelper;

public class AviatorAdminConfigHelper extends AbstractSessionHelper<AviatorAdminConfigDescriptor> {
    private static final AviatorAdminConfigHelper INSTANCE = new AviatorAdminConfigHelper();

    private AviatorAdminConfigHelper() {}

    @Override
    public String getType() {
        return "aviator-admin-config";
    }
    
    @Override
    protected String getLoginCmd() {
        return "fcli aviator admin-config create";
    }

    @Override
    protected Class<AviatorAdminConfigDescriptor> getSessionDescriptorType() {
        return AviatorAdminConfigDescriptor.class;
    }

    public void validateConfig(AviatorAdminConfigDescriptor configDescriptor) {
        try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(configDescriptor.getAviatorUrl())) {
            String[] messageAndSignature = AviatorSignatureUtils.createMessageAndSignature(configDescriptor, configDescriptor.getTenant());
            String message = messageAndSignature[0];
            String signature = messageAndSignature[1];
            client.validateAdminSession(configDescriptor.getTenant(), signature, message);
        }
    }

    public static final AviatorAdminConfigHelper instance() {
        return INSTANCE;
    }
}