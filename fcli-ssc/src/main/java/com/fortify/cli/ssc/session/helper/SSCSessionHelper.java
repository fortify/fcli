package com.fortify.cli.ssc.session.helper;

import com.fortify.cli.common.session.helper.AbstractSessionHelper;

public class SSCSessionHelper extends AbstractSessionHelper<SSCSessionDescriptor> {
    private static final SSCSessionHelper INSTANCE = new SSCSessionHelper();
    
    private SSCSessionHelper() {}
    
    @Override
    public String getType() {
        return "SSC";
    }

    @Override
    protected Class<SSCSessionDescriptor> getSessionDescriptorType() {
        return SSCSessionDescriptor.class;
    }
    
    public static final SSCSessionHelper instance() {
        return INSTANCE;
    }
}
