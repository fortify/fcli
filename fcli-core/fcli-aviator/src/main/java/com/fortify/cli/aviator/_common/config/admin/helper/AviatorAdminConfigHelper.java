/*
 * Copyright 2021-2025 Open Text.
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

    public static final AviatorAdminConfigHelper instance() {
        return INSTANCE;
    }
}