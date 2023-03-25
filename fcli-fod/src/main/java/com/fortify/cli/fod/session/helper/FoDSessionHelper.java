package com.fortify.cli.fod.session.helper;

import com.fortify.cli.common.session.helper.AbstractSessionHelper;

public class FoDSessionHelper extends AbstractSessionHelper<FoDSessionDescriptor> {
    private static final FoDSessionHelper INSTANCE = new FoDSessionHelper();
    
    private FoDSessionHelper() {}
    
    @Override
    public String getType() {
        return "FoD";
    }

    @Override
    protected Class<FoDSessionDescriptor> getSessionDescriptorType() {
        return FoDSessionDescriptor.class;
    }
    
    public static final FoDSessionHelper instance() {
        return INSTANCE;
    }
}
