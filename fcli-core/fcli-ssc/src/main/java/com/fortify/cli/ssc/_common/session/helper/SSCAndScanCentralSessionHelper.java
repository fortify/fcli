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
package com.fortify.cli.ssc._common.session.helper;

import com.fortify.cli.common.session.helper.AbstractSessionHelper;

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
}
