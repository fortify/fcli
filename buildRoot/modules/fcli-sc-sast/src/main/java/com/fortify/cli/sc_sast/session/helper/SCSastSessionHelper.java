/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.sc_sast.session.helper;

import com.fortify.cli.common.session.helper.AbstractSessionHelper;

public class SCSastSessionHelper extends AbstractSessionHelper<SCSastSessionDescriptor> {
    private static final SCSastSessionHelper INSTANCE = new SCSastSessionHelper();
    
    private SCSastSessionHelper() {}
    
    @Override
    public String getType() {
        return "SC-SAST";
    }

    @Override
    protected Class<SCSastSessionDescriptor> getSessionDescriptorType() {
        return SCSastSessionDescriptor.class;
    }
    
    public static final SCSastSessionHelper instance() {
        return INSTANCE;
    }
}
