package com.fortify.cli.sc_dast.session.helper;

import com.fortify.cli.common.session.helper.AbstractSessionHelper;

public class SCDastSessionHelper extends AbstractSessionHelper<SCDastSessionDescriptor> {
    private static final SCDastSessionHelper INSTANCE = new SCDastSessionHelper();
    
    private SCDastSessionHelper() {}
    
    @Override
    public String getType() {
        return "SC-DAST";
    }

    @Override
    protected Class<SCDastSessionDescriptor> getSessionDescriptorType() {
        return SCDastSessionDescriptor.class;
    }
    
    public static final SCDastSessionHelper instance() {
        return INSTANCE;
    }
}
