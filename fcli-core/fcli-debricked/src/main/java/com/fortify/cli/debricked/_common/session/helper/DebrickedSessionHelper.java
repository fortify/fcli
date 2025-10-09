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
package com.fortify.cli.debricked._common.session.helper;

import com.fortify.cli.common.session.helper.AbstractSessionHelper;

public final class DebrickedSessionHelper extends AbstractSessionHelper<DebrickedSessionDescriptor> {
    private static final DebrickedSessionHelper INSTANCE = new DebrickedSessionHelper();
    
    private DebrickedSessionHelper() {}
    
    public static final DebrickedSessionHelper instance() {
        return INSTANCE;
    }

    @Override
    public final String getType() {
        return "Debricked";
    }

    @Override
    protected final Class<DebrickedSessionDescriptor> getSessionDescriptorType() {
        return DebrickedSessionDescriptor.class;
    }
    
    @Override
    public String getLoginCmd() {
        return "fcli debricked session login";
    }
}