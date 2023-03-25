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
